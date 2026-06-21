package org.magic.jmix.addons.core.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.AbstractDataProvider;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.*;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaPropertyPath;
import io.jmix.core.querycondition.Condition;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.BindingState;
import io.jmix.flowui.data.ContainerDataUnit;
import io.jmix.flowui.data.EntityDataUnit;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.data.grid.EntityDataGridItems;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.flowui.model.ViewData;
import io.jmix.flowui.kit.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.magic.jmix.addons.core.MagicCoreProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.annotation.Nullable;
import jakarta.servlet.ServletContext;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 懒加载 DataGrid 数据提供器，包装标准 CollectionContainer 实现 DB 级分页。
 * <p>
 * 通过 {@link #install(DataGrid, DataManager)} 静态方法激活，一行代码完成配置。
 * <p>
 * <b>核心特性：</b>
 * <ul>
 *   <li>实现 {@link ContainerDataUnit}，完全兼容标准 CC 的事件机制和 DetailView 流程</li>
 *   <li>新建实体自动加入 prependBuffer，显示在列表顶部</li>
 *   <li>支持 genericFilter、排序、baseCondition</li>
 *   <li>滚动时按需从 DB 加载数据，CC 只持有当前视口数据</li>
 * </ul>
 * <p>
 * <b>数据流程：</b>
 * <ol>
 *   <li>fetch() 从 prependBuffer 和 DB 获取数据，DB 数据通过 DataGenerator 回调同步到 CC</li>
 *   <li>新建实体触发 CC 的 ADD_ITEMS 事件，被捕获到 prependBuffer 并从 CC 移除避免重复</li>
 *   <li>编辑触发 CC 的 SET_ITEM 事件，更新 prependBuffer 中对应实体引用</li>
 *   <li>删除触发 CC 的 REMOVE_ITEMS 事件，从 prependBuffer 移除对应实体</li>
 * </ol>
 *
 * @param <T> 实体类型
 * @see #install(DataGrid, DataManager)
 */
@SuppressWarnings("unchecked")
public class LazyContainerDataGridItems<T> extends AbstractDataProvider<T, Void>
        implements ContainerDataUnit<T>, EntityDataGridItems<T>, DataGridItems.Sortable<T> {

    private static final Logger log = LoggerFactory.getLogger(LazyContainerDataGridItems.class);

    private static final String PROPERTY_REPLACE_ITEM = "<replaceItem>";

    private final CollectionContainer<T> container;
    private final DataManager dataManager;
    private final CollectionLoader<T> loader;
    private final MetadataTools metadataTools;
    private final Messages messages;
    private DataGrid<T> grid;

    // Configuration
    private int pageSize = 30;
    private Span totalCountLabel;
    private Runnable afterDataChange;
    private Supplier<Condition> baseConditionSupplier;

    // State
    private Condition filterCondition;
    private int loadedRows = 0;
    private final List<T> prependBuffer = new ArrayList<>();
    private Span rangeSpan;
    private Span totalSpan;

    // Viewport cache (optimizes multiple fetches per fetchFromProvider into 1 DB query)
    private List<T> viewportCache;
    private int cacheBaseOffset = -1;
    private int cacheRangeEnd = -1;

    private void clearCache() {
        viewportCache = null;
        cacheBaseOffset = -1;
        cacheRangeEnd = -1;
    }

    // Event bus (mirrors ContainerDataGridItems pattern)
    protected EventBus eventBus;

    // ==================== Constructor ====================

    LazyContainerDataGridItems(CollectionContainer<T> container,
                                CollectionLoader<T> loader,
                                DataManager dataManager,
                                MetadataTools metadataTools,
                                Messages messages) {
        this.container = container;
        this.loader = loader;
        this.dataManager = dataManager;
        this.metadataTools = metadataTools;
        this.messages = messages;
        initContainerListeners();
    }

    private void initContainerListeners() {
        container.addCollectionChangeListener(event -> {
            switch (event.getChangeType()) {
                case SET_ITEM -> {
                    // 编辑保存后调用 container.replaceItem() 触发
                    // 需要同步更新 prependBuffer 中的实体引用
                    for (T item : event.getChanges()) {
                        Object id = EntityValues.getId(item);
                        int idx = findInPrependBuffer(id);
                        if (idx >= 0) {
                            prependBuffer.set(idx, item);
                        }
                        fireValueChangeEvent(item, PROPERTY_REPLACE_ITEM, null, null);
                    }
                }
                case ADD_ITEMS -> {
                    // 触发场景：
                    // 1. 新建实体 container.add() → 加入 prependBuffer 头部
                    // 2. 编辑保存 container.replaceItem() 但实体不在 CC 中（已离开视口）→ 更新 prependBuffer 引用
                    for (T item : event.getChanges()) {
                        Object id = EntityValues.getId(item);
                        int idx = findInPrependBuffer(id);
                        if (idx >= 0) {
                            // 场景2：实体已在 buffer 中，更新引用
                            prependBuffer.set(idx, item);
                        } else {
                            // 场景1：新实体加入 buffer 头部
                            prependBuffer.add(0, item);
                        }
                    }
                    muteContainerRemoveAll(event.getChanges());
                    fireItemSetChangeEvent();
                }
                case REMOVE_ITEMS -> {
                    // 删除实体时从 prependBuffer 移除
                    for (T item : event.getChanges()) {
                        Object id = EntityValues.getId(item);
                        prependBuffer.removeIf(b -> Objects.equals(EntityValues.getId(b), id));
                    }
                    fireItemSetChangeEvent();
                }
                default -> fireItemSetChangeEvent();
            }
        });

        container.addItemPropertyChangeListener(event -> {
            T item = (T) event.getItem();
            String property = event.getProperty();
            Object prevValue = event.getPrevValue();
            Object value = event.getValue();
            fireValueChangeEvent(item, property, prevValue, value);
        });

        container.addItemChangeListener(event ->
                fireSelectedItemChangeEvent((T) event.getItem()));
    }

    // ==================== PrependBuffer helpers ====================

    /** 在 prependBuffer 中查找指定 ID 的实体索引，返回 -1 表示未找到 */
    private int findInPrependBuffer(Object id) {
        for (int i = 0; i < prependBuffer.size(); i++) {
            if (Objects.equals(EntityValues.getId(prependBuffer.get(i)), id)) {
                return i;
            }
        }
        return -1;
    }

    /** 收集 prependBuffer 中所有实体的 ID */
    private Set<Object> collectBufferIds() {
        return prependBuffer.stream()
                .map(EntityValues::getId)
                .collect(Collectors.toSet());
    }

    /** 静默从 CC 中移除指定集合的实体，避免触发循环事件 */
    @SuppressWarnings("unchecked")
    private void muteContainerRemoveAll(Collection<?> items) {
        container.mute();
        try {
            container.getMutableItems().removeAll((Collection<? extends T>) items);
        } finally {
            container.unmute();
        }
    }

    // ==================== DataProvider: fetch / size ====================

    /**
     * Vaadin DataProvider 核心方法：按需获取数据。
     * <p>
     * 实现视口缓存优化：单次 fetch 请求可能被 DataCommunicator 拆分成多次调用，
     * 使用 viewportCache 将同一范围的所有请求合并为一次 DB 查询。
     */
    @Override
    public Stream<T> fetch(Query<T, Void> query) {
        if (getState() == BindingState.INACTIVE) {
            return Stream.empty();
        }

        int offset = query.getOffset();
        int limit = query.getLimit();
        int[] vr = getViewportRange();

        if (vr == null) {
            clearCache();
            return Stream.empty();
        }

        int vrStart = vr[0];
        int vrEnd = vr[1];

        List<T> result;
        int serveCount;

        if (viewportCache != null) {
            // 后续 fetch：从缓存返回，避免重复 DB 查询
            int fromIndex = offset - cacheBaseOffset;
            if (fromIndex < 0 || fromIndex >= viewportCache.size()) {
                clearCache();
                return Stream.empty();
            }
            serveCount = Math.min(limit, viewportCache.size() - fromIndex);
            result = new ArrayList<>(viewportCache.subList(fromIndex, fromIndex + serveCount));
        } else {
            // 首次 fetch：执行 DB 查询并构建缓存
            int rangeLength = computeRangeLength(offset, vrStart, vrEnd);
            int bufferSize = prependBuffer.size();

            if (rangeLength <= pageSize) {
                List<T> dbResult = fetchWithBuffer(offset, rangeLength, bufferSize);
                loadedRows = Math.max(loadedRows, offset + dbResult.size());
                updateCountDisplay(-1);
                return dbResult.stream();
            }

            List<T> allData = fetchWithBuffer(offset, rangeLength, bufferSize);
            viewportCache = allData;
            cacheBaseOffset = offset;
            cacheRangeEnd = offset + rangeLength;

            serveCount = Math.min(limit, allData.size());
            result = new ArrayList<>(allData.subList(0, serveCount));
        }

        // 缓存清理条件（匹配 DataCommunicator 的分页退出条件）
        boolean dataExhausted = serveCount < limit;
        boolean allPagesServed = offset + serveCount >= cacheRangeEnd;
        if (dataExhausted || allPagesServed) {
            clearCache();
        }

        loadedRows = Math.max(loadedRows, offset + serveCount);
        updateCountDisplay(-1);
        return result.stream();
    }

    /**
     * 结合 prependBuffer 和 DB 数据获取指定范围的数据。
     * <p>
     * prependBuffer 占据虚拟位置 0 ~ bufferSize-1，DB 数据从 bufferSize 开始。
     * 为避免重复，DB 查询会排除 buffer 中的实体 ID，并额外获取补偿数据。
     *
     * @param offset     请求起始位置
     * @param count      请求数量
     * @param bufferSize prependBuffer 大小
     * @return 合并后的数据列表
     */
    private List<T> fetchWithBuffer(int offset, int count, int bufferSize) {
        if (bufferSize == 0) {
            return fetchFromDb(offset, count);
        }

        Set<Object> bufferIds = collectBufferIds();

        // Buffer items for this range (newest first, prependBuffer[0] = newest)
        int bufferCount = (offset < bufferSize) ? Math.min(bufferSize - offset, count) : 0;

        // Buffer covers entire range — skip DB query
        if (bufferCount >= count) {
            return new ArrayList<>(prependBuffer.subList(offset, offset + count));
        }

        // DB params: shift offset by bufferSize, over-fetch to compensate for excluded buffer IDs
        int dbOffset = Math.max(0, offset - bufferSize);
        int dbNeeded = count - bufferCount;
        List<T> dbData = fetchFromDbExcluding(dbOffset, dbNeeded + bufferSize, bufferIds, dbNeeded);

        if (bufferCount == 0) {
            return dbData;
        }

        List<T> result = new ArrayList<>(prependBuffer.subList(offset, offset + bufferCount));
        result.addAll(dbData);
        return result;
    }

    private List<T> fetchFromDbExcluding(int offset, int fetchCount, Set<Object> excludeIds, int neededCount) {
        List<T> raw = fetchFromDb(offset, fetchCount);
        List<T> result = new ArrayList<>();
        for (T item : raw) {
            if (!excludeIds.contains(EntityValues.getId(item))) {
                result.add(item);
                if (result.size() >= neededCount) break;
            }
        }
        return result;
    }

    @Override
    public int size(Query<T, Void> query) {
        if (getState() == BindingState.INACTIVE) {
            return 0;
        }
        int count = countFromDb();
        if (count < loadedRows) {
            loadedRows = count;
        }
        updateCountDisplay(count);
        return count;
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    // ==================== DB access ====================

    private List<T> fetchFromDb(int offset, int limit) {
        Condition condition = buildCondition();
        Sort sort = buildSort();

        Class<T> entityClass = getType();
        return dataManager.load(entityClass)
                .condition(condition)
                .fetchPlan(container.getFetchPlan())
                .sort(sort)
                .firstResult(offset)
                .maxResults(limit)
                .list();
    }

    private int countFromDb() {
        Condition condition = buildCondition();

        LoadContext<?> loadContext = new LoadContext<>(container.getEntityMetaClass())
                .setQuery(new LoadContext.Query(
                        "select e from " + container.getEntityMetaClass().getName() + " e")
                        .setCondition(condition));

        long count = dataManager.getCount(loadContext);
        return (int) count;
    }

    // ==================== Condition / Sort ====================

    private Condition buildCondition() {
        List<Condition> conditions = new ArrayList<>();
        if (baseConditionSupplier != null) {
            Condition base = baseConditionSupplier.get();
            if (base != null) {
                conditions.add(base);
            }
        }
        if (filterCondition != null) {
            conditions.add(filterCondition);
        }
        if (conditions.isEmpty()) {
            return LogicalCondition.and();
        } else if (conditions.size() == 1) {
            return conditions.get(0);
        } else {
            return LogicalCondition.and(conditions.toArray(new Condition[0]));
        }
    }

    private Sort buildSort() {
        Sort loaderSort = loader.getSort();
        if (loaderSort != null && !loaderSort.getOrders().isEmpty()) {
            return loaderSort;
        }

        Sort querySort = extractSortFromQuery();
        if (querySort != null) {
            return querySort;
        }

        return Sort.UNSORTED;
    }

    @Nullable
    private Sort extractSortFromQuery() {
        String query = loader.getQuery();
        if (query == null) return null;

        String lowerQuery = query.toLowerCase();
        int orderByIndex = lowerQuery.lastIndexOf("order by");
        if (orderByIndex < 0) return null;

        String orderByClause = query.substring(orderByIndex + "order by".length()).trim();
        String[] parts = orderByClause.split(",");
        List<Sort.Order> orders = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            // Remove entity alias: "e.createTs" → "createTs"
            if (trimmed.contains(".")) {
                trimmed = trimmed.substring(trimmed.lastIndexOf('.') + 1);
            }
            if (trimmed.toLowerCase().endsWith(" desc")) {
                orders.add(Sort.Order.desc(trimmed.substring(0, trimmed.length() - 5).trim()));
            } else if (trimmed.toLowerCase().endsWith(" asc")) {
                orders.add(Sort.Order.asc(trimmed.substring(0, trimmed.length() - 4).trim()));
            } else {
                orders.add(Sort.Order.asc(trimmed));
            }
        }
        return orders.isEmpty() ? null : Sort.by(orders);
    }

    // ==================== genericFilter bridge ====================

    /**
     * 设置 loader 的 loadDelegate，桥接 genericFilter 的刷新请求。
     * <p>
     * 当用户修改过滤条件时，genericFilter 会调用 loader.load()，
     * 通过此 delegate 捕获条件变化并触发 DataProvider 刷新。
     */
    void setupLoadDelegate() {
        loader.setLoadDelegate(loadContext -> {
            filterCondition = loadContext.getQuery().getCondition();
            loadedRows = 0;
            prependBuffer.clear();
            log.debug("setupLoadDelegate: clearing cache (filter change)");
            clearCache();
            clearContainer();
            refreshAll();
            fireDataChange();
            return Collections.emptyList();
        });
    }

    private void clearContainer() {
        container.mute();
        try {
            container.setItems(Collections.emptyList());
        } finally {
            container.unmute();
        }
    }

    // ==================== Count display ====================

    private void fireDataChange() {
        if (afterDataChange != null) {
            afterDataChange.run();
        }
    }

    private void updateCountDisplay(int totalCount) {
        if (totalCountLabel == null) return;

        if (loadedRows > 0) {
            String range = "1-" + loadedRows;
            rangeSpan.setText(messages.formatMessage("", "pagination.msg1", range));
            rangeSpan.setVisible(true);
        } else {
            rangeSpan.setVisible(false);
        }

        if (totalCount >= 0) {
            totalSpan.setText(String.valueOf(totalCount));
        }
    }

    // ==================== ContainerDataUnit ====================

    @Override
    public CollectionContainer<T> getContainer() {
        return container;
    }

    /**
     * 返回 CC 中的所有项。
     * <p>
     * <b>注意：</b>CC 只持有当前视口的数据，不是全部数据。
     * 类似于分页模式下 {@code ContainerDataGridItems} 只持有当前页数据。
     */
    @Override
    public List<T> getItems() {
        return container.getItems();
    }

    @Override
    public T getSelectedItem() {
        return container.getItemOrNull();
    }

    @Override
    public void setSelectedItem(@Nullable T item) {
        container.setItem(item);
    }

    /**
     * 判断 CC 是否包含指定项。
     * <p>
     * <b>注意：</b>只检查当前视口数据，不在视口中的实体返回 {@code false}，
     * 即使该实体在数据库中存在。
     */
    @Override
    public boolean containsItem(T item) {
        return container.containsItem(item);
    }

    @Override
    public BindingState getState() {
        return BindingState.ACTIVE;
    }

    @Override
    public Registration addStateChangeListener(Consumer<StateChangeEvent> listener) {
        return getEventBus().addListener(StateChangeEvent.class, listener);
    }

    // ==================== EntityDataGridItems / EntityDataUnit ====================

    /**
     * 根据 ID 获取实体。
     * <p>
     * <b>注意：</b>只从 CC 中查找，不在视口中的实体返回 {@code null}，
     * 即使该实体在数据库中存在。
     */
    @Override
    public T getItem(Object entityId) {
        return container.getItem(entityId);
    }

    /**
     * 获取实体属性值。
     * <p>
     * <b>注意：</b>只对当前视口中的实体有效，不在视口中的实体返回 {@code null}。
     */
    @Override
    public Object getItemValue(Object itemId, MetaPropertyPath propertyId) {
        T item = container.getItem(itemId);
        return item != null ? EntityValues.getValueEx(item, propertyId) : null;
    }

    @Override
    public MetaClass getEntityMetaClass() {
        return container.getEntityMetaClass();
    }

    @Override
    public Class<T> getType() {
        return getEntityMetaClass().getJavaClass();
    }

    // ==================== DataGridItems listeners ====================

    @Override
    public Registration addValueChangeListener(Consumer<DataGridItems.ValueChangeEvent<T>> listener) {
        return getEventBus().addListener(DataGridItems.ValueChangeEvent.class, ((Consumer) listener));
    }

    @Override
    public Registration addItemSetChangeListener(Consumer<DataGridItems.ItemSetChangeEvent<T>> listener) {
        return getEventBus().addListener(DataGridItems.ItemSetChangeEvent.class, ((Consumer) listener));
    }

    @Override
    public Registration addSelectedItemChangeListener(Consumer<DataGridItems.SelectedItemChangeEvent<T>> listener) {
        return getEventBus().addListener(DataGridItems.SelectedItemChangeEvent.class, ((Consumer) listener));
    }

    // ==================== DataGridItems.Sortable ====================

    @Override
    public void sort(Object[] propertyId, boolean[] ascending) {
        if (propertyId.length != ascending.length) {
            throw new IllegalArgumentException("Property and ascending arrays must be same length");
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (int i = 0; i < propertyId.length; i++) {
            String propertyName;
            if (propertyId[i] instanceof MetaPropertyPath mpp) {
                propertyName = mpp.toPathString();
            } else {
                propertyName = propertyId[i].toString();
            }
            orders.add(ascending[i] ? Sort.Order.asc(propertyName) : Sort.Order.desc(propertyName));
        }

        Sort sort = Sort.by(orders);
        loader.setSort(sort);

        loadedRows = 0;
        prependBuffer.clear();
        log.debug("sort: clearing cache (sort change)");
        clearCache();
        clearContainer();
        refreshAll();
    }

    @Override
    public void resetSortOrder() {
        loader.setSort(Sort.UNSORTED);
        loadedRows = 0;
        prependBuffer.clear();
        log.debug("resetSortOrder: clearing cache");
        clearCache();
        clearContainer();
        refreshAll();
    }

    // ==================== Event firing ====================

    private void fireItemSetChangeEvent() {
        clearCache();
        getEventBus().fireEvent(new DataGridItems.ItemSetChangeEvent<>(this));
    }

    private void fireValueChangeEvent(T item, String property,
                                       @Nullable Object prevValue, @Nullable Object value) {
        getEventBus().fireEvent(new DataGridItems.ValueChangeEvent<>(
                this, item, property, prevValue, value));
    }

    private void fireSelectedItemChangeEvent(@Nullable T item) {
        getEventBus().fireEvent(new DataGridItems.SelectedItemChangeEvent<>(this, item));
    }

    protected EventBus getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus();
        }
        return eventBus;
    }

    // ==================== Viewport range (reflection) ====================

    @Nullable
    private int[] getViewportRange() {
        if (grid == null) return null;
        try {
            var dc = grid.getDataCommunicator();
            Object viewportRange = getFieldValue(dc, "viewportRange");
            if (viewportRange != null) {
                int vrStart = (int) viewportRange.getClass().getMethod("getStart").invoke(viewportRange);
                int vrEnd = (int) viewportRange.getClass().getMethod("getEnd").invoke(viewportRange);
                return new int[]{vrStart, vrEnd};
            }
        } catch (Exception e) {
            log.debug("getViewportRange: reflection failed", e);
        }
        return null;
    }

    /**
     * 计算当前 fetch 请求需要获取的数据范围长度。
     * <p>
     * DataCommunicator 可能将一个视口范围拆分成多个分区（partition）：
     * <ul>
     *   <li>resendEntireRange=true：全量重发，返回整个有效范围</li>
     *   <li>resendEntireRange=false：增量更新，根据 activeStart/activeKeys 判断分区</li>
     * </ul>
     * 此方法匹配 DataCommunicator 的分区逻辑，确保只获取必要的数据。
     */
    private int computeRangeLength(int offset, int vrStart, int vrEnd) {
        int assumedSize = getAssumedSize();
        int erEnd = Math.min(vrEnd, assumedSize);

        if (getResendEntireRange()) {
            return erEnd - offset;
        }

        // resendEntireRange=false: determine which partition
        int activeStart = getActiveStart();
        int activeKeyCount = getActiveKeys().size();
        int paStart = activeStart;
        int paEnd = activeStart + activeKeyCount;

        if (offset < paStart) {
            // partition[0]: [erStart, min(erEnd, paStart))
            return Math.min(erEnd, paStart) - offset;
        } else if (offset >= paEnd) {
            // partition[2]: [max(erStart, paEnd), erEnd)
            return erEnd - offset;
        } else {
            // Fallback: offset within previousActive, shouldn't happen
            return erEnd - offset;
        }
    }

    private int getAssumedSize() {
        if (grid == null) return Integer.MAX_VALUE;
        try {
            var dc = grid.getDataCommunicator();
            Integer val = getIntFieldValue(dc, "assumedSize");
            return val != null ? val : Integer.MAX_VALUE;
        } catch (Exception ignored) {}
        return Integer.MAX_VALUE;
    }

    private int getActiveStart() {
        if (grid == null) return 0;
        try {
            var dc = grid.getDataCommunicator();
            Integer val = getIntFieldValue(dc, "activeStart");
            return val != null ? val : 0;
        } catch (Exception ignored) {}
        return 0;
    }

    private boolean getResendEntireRange() {
        if (grid == null) return false;
        try {
            var dc = grid.getDataCommunicator();
            java.lang.reflect.Field f = findField(dc.getClass(), "resendEntireRange");
            if (f != null) {
                f.setAccessible(true);
                return f.getBoolean(dc);
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ==================== Reflection helpers ====================

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object getFieldValue(Object obj, String name) throws IllegalAccessException {
        java.lang.reflect.Field f = findField(obj.getClass(), name);
        if (f == null) return null;
        f.setAccessible(true);
        return f.get(obj);
    }

    private static Integer getIntFieldValue(Object obj, String name) throws IllegalAccessException {
        Object val = getFieldValue(obj, name);
        return val != null ? (Integer) val : null;
    }

    private List<String> getActiveKeys() {
        try {
            var dc = grid.getDataCommunicator();
            java.lang.reflect.Field activeKeyOrderField = findField(dc.getClass(), "activeKeyOrder");
            if (activeKeyOrderField != null) {
                activeKeyOrderField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<String> activeKeys = (List<String>) activeKeyOrderField.get(dc);
                return activeKeys;
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    // ==================== Install (static factory) ====================

    /**
     * 激活懒加载模式的入口方法。
     * <p>
     * 使用示例：
     * <pre>{@code
     * LazyContainerDataGridItems.install(dataGrid, dataManager)
     *     .pageSize(50)
     *     .totalCountLabel(totalLabel)
     *     .apply();
     * }</pre>
     *
     * @param grid        DataGrid 组件（XML 必须使用 dataContainer 属性）
     * @param dataManager DataManager bean
     * @return Builder 用于链式配置
     */
    public static <T> Builder<T> install(DataGrid<T> grid, DataManager dataManager) {
        return new Builder<>(grid, dataManager);
    }

    /**
     * 配置构建器。
     */
    public static class Builder<T> {
        private final DataGrid<T> grid;
        private final DataManager dataManager;
        private final ApplicationContext ctx;
        private int pageSize;
        private Span totalCountLabel;
        private Runnable afterDataChange;
        private Supplier<Condition> baseConditionSupplier;

        private Builder(DataGrid<T> grid, DataManager dataManager) {
            this.grid = grid;
            this.dataManager = dataManager;
            this.ctx = getApplicationContext();
            this.pageSize = ctx.getBean(MagicCoreProperties.class).getLazyPageSize();
        }

        /** 每页大小，默认从 MagicCoreProperties 读取 */
        public Builder<T> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /** 总数显示标签，显示 "1-30 / 100" 格式 */
        public Builder<T> totalCountLabel(Span label) {
            this.totalCountLabel = label;
            return this;
        }

        /** 数据变化后的回调（如滚动加载完成） */
        public Builder<T> afterDataChange(Runnable callback) {
            this.afterDataChange = callback;
            return this;
        }

        /** 基础条件提供器，用于主从表等场景过滤从表数据 */
        public Builder<T> baseConditionSupplier(Supplier<Condition> supplier) {
            this.baseConditionSupplier = supplier;
            return this;
        }

        /**
         * 应用配置，完成懒加载激活。
         * <p>
         * 内部会：
         * <ol>
         *   <li>从 Grid 当前的 ContainerDataUnit 提取 CC</li>
         *   <li>创建 LazyContainerDataGridItems 包装 CC</li>
         *   <li>设置 loader.loadDelegate 桥接 genericFilter</li>
         *   <li>注册 DataGenerator 同步 CC 和 keyMapper</li>
         *   <li>调用 grid.setItems() 替换 DataProvider</li>
         * </ol>
         *
         * @return 创建的 LazyContainerDataGridItems 实例
         */
        @SuppressWarnings("unchecked")
        public LazyContainerDataGridItems<T> apply() {
            // Extract CC from current grid DataProvider (must be ContainerDataUnit)
            var currentItems = grid.getDataProvider();
            if (!(currentItems instanceof ContainerDataUnit<?> containerDataUnit)) {
                throw new IllegalStateException(
                        "Grid DataProvider must be ContainerDataUnit. " +
                        "Ensure the XML uses dataContainer attribute, not metaClass.");
            }
            CollectionContainer<T> cc = (CollectionContainer<T>) containerDataUnit.getContainer();

            // Find the loader bound to this container via ViewData
            CollectionLoader<T> loader = findLoader(cc);
            if (loader == null) {
                throw new IllegalStateException("Cannot find CollectionLoader for container: " + cc);
            }

            // Get Spring beans from servlet context
            ApplicationContext ctx = getApplicationContext();
            MetadataTools metadataTools = ctx.getBean(MetadataTools.class);
            Messages messages = ctx.getBean(Messages.class);

            LazyContainerDataGridItems<T> lazyItems = new LazyContainerDataGridItems<>(
                    cc, loader, dataManager, metadataTools, messages);

            lazyItems.pageSize = this.pageSize;
            lazyItems.afterDataChange = this.afterDataChange;
            lazyItems.baseConditionSupplier = this.baseConditionSupplier;

            if (this.totalCountLabel != null) {
                lazyItems.totalCountLabel = this.totalCountLabel;
                lazyItems.totalCountLabel.getStyle().set("display", "flex");
                lazyItems.totalCountLabel.getStyle().set("align-items", "center");
                lazyItems.totalCountLabel.getStyle().set("gap", "0.3em");
                lazyItems.rangeSpan = new Span();
                lazyItems.totalSpan = new Span();
                lazyItems.totalCountLabel.removeAll();
                lazyItems.totalCountLabel.add(lazyItems.rangeSpan, lazyItems.totalSpan);
            }

            lazyItems.setupLoadDelegate();

            lazyItems.grid = grid;
            grid.setPageSize(this.pageSize);
            grid.setItems(lazyItems);

            // 注册 DataGenerator，同步 keyMapper 和 CC
            grid.addDataGenerator(new DataGenerator<T>() {
                @Override
                public void generateData(T item, elemental.json.JsonObject jsonObject) {
                    // generateData 不只是 add，reset() 不清 keyMapper 时
                    // 同一实体的旧映射会被更新为新实例，所以需要同时处理 add 和 replace
                    cc.mute();
                    try {
                        List<T> items = cc.getMutableItems();
                        int idx = items.indexOf(item);
                        if (idx >= 0) {
                            items.set(idx, item);
                        } else {
                            items.add(item);
                        }
                    } finally {
                        cc.unmute();
                    }
                }

                @Override
                public void destroyData(T item) {
                    // 如果实体被选中，先取消选中（视口离开后操作无意义）
                    if (grid.getSelectedItems().contains(item)) {
                        grid.deselect(item);
                    }
                    cc.mute();
                    try {
                        cc.getMutableItems().remove(item);
                    } finally {
                        cc.unmute();
                    }
                }

                @Override
                public void destroyAllData() {
                    // DataCommunicator.reset() 的完整行为：
                    //   1. resendEntireRange = true（标记全量重发）
                    //   2. dataGenerator.destroyAllData()（本回调）
                    //   3. updatedData.clear()（清空 refresh(item) 积累的单行更新队列）
                    //   4. requestFlush()（注册 flush 回调）
                    // 注意：reset() 不清 keyMapper（removeAll 只在 setDataProvider 中调用），
                    //       不清 activeKeyOrder。keyMapper 中仍有旧数据，会在后续 flush 中被覆盖或 passivate。
                    //       这里清空 CC，flush 后 generateData 会从新的 fetch 结果重建 CC。
                    cc.mute();
                    try {
                        cc.setItems(Collections.emptyList());
                    } finally {
                        cc.unmute();
                    }
                }
            });

            return lazyItems;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        private CollectionLoader<T> findLoader(CollectionContainer<T> cc) {
            var view = UiComponentUtils.getView(grid);
            ViewData viewData = ViewControllerUtils.getViewData(view);
            for (String loaderId : viewData.getLoaderIds()) {
                var l = viewData.getLoader(loaderId);
                if (l instanceof CollectionLoader<?> cl && cl.getContainer() == cc) {
                    return (CollectionLoader<T>) cl;
                }
            }
            return null;
        }

        private ApplicationContext getApplicationContext() {
            // Grid is attached to UI during onInit, so UI.getCurrent() works
            ServletContext servletContext = com.vaadin.flow.server.VaadinServlet
                    .getCurrent().getServletContext();
            return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        }
    }
}
