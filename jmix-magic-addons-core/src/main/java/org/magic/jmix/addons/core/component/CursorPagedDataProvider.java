package org.magic.jmix.addons.core.component;

import com.vaadin.flow.data.provider.AbstractDataProvider;
import com.vaadin.flow.data.provider.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 游标分页 Grid 数据提供器。
 * <p>
 * 将「只能顺序前进」的游标分页接口适配为 Grid 所需的 offset/limit 懒加载接口。
 * <p>
 * <b>核心行为：</b>
 * <ul>
 *   <li>维护已加载缓存，向下滚动时按需推进游标补齐请求范围</li>
 *   <li>向上滚动时直接命中已加载缓存，不重复请求后端</li>
 *   <li>fetch 返回缓存切片快照，避免缓存结构变化导致并发修改异常</li>
 *   <li>size 始终返回当前已加载数量，不返回估计值</li>
 * </ul>
 *
 * @param <T> 数据项类型
 */
public class CursorPagedDataProvider<T> extends AbstractDataProvider<T, Void> {

    private static final Logger log = LoggerFactory.getLogger(CursorPagedDataProvider.class);

    private final CursorPageFetcher<T> fetcher;
    private final List<T> loaded = new ArrayList<>();

    private String cursor;
    private boolean exhausted;

    /**
     * 创建游标分页数据提供器。
     *
     * @param fetcher 游标分页加载函数
     */
    public CursorPagedDataProvider(CursorPageFetcher<T> fetcher) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher must not be null");
    }

    @Override
    public synchronized Stream<T> fetch(Query<T, Void> query) {
        int offset = query.getOffset();
        int limit = query.getLimit();
        int requiredSize = offset + limit;

        while (!exhausted && loaded.size() < requiredSize) {
            CursorPage<T> page = fetcher.fetch(cursor);
            if (page == null) {
                log.debug("cursor fetch returned null page, mark exhausted");
                exhausted = true;
                break;
            }

            List<T> items = page.getItems();
            if (!items.isEmpty()) {
                loaded.addAll(items);
            }

            String previousCursor = cursor;
            cursor = page.getNextCursor();
            exhausted = !page.isHasMore();

            if (items.isEmpty() && page.isHasMore() && Objects.equals(previousCursor, cursor)) {
                log.debug("cursor fetch did not advance cursor and returned empty page, mark exhausted");
                exhausted = true;
            }
        }

        int end = Math.min(requiredSize, loaded.size());
        int from = Math.min(offset, end);
        return new ArrayList<>(loaded.subList(from, end)).stream();
    }

    @Override
    public synchronized int size(Query<T, Void> query) {
        return loaded.size();
    }

    /**
     * 返回当前已加载到缓存的数据条数。
     *
     * @return 已加载条数
     */
    public synchronized int getLoadedCount() {
        return loaded.size();
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    /**
     * 重置游标分页状态，并通知 Grid 重新加载。
     */
    public synchronized void reset() {
        loaded.clear();
        cursor = null;
        exhausted = false;
        refreshAll();
    }
}
