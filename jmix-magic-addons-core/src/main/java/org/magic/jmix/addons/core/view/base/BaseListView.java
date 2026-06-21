package org.magic.jmix.addons.core.view.base;

import io.jmix.flowui.component.ListDataComponent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataLoader;
import io.jmix.flowui.model.ViewData;
import io.jmix.flowui.view.CloseAction;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.ViewControllerUtils;
import org.magic.jmix.addons.core.tab.TabActivationAware;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础列表视图。
 * <p>
 * 实现 {@link DetailViewCloseCallback} 接口，提供默认的关闭回调行为。
 * 开发者可以重写 {@link #onClose}、{@link #onSaved}、{@link #afterSaved} 方法自定义行为，
 * 并通过 {@code super.xxx()} 调用默认实现。
 * <p>
 * 数据加载由 XML 中的 {@code <dataLoadCoordinator>} 在 {@code BeforeShowEvent} 时自动触发。
 * 如需手动加载所有 loader，可调用 {@link #loadAllCollectionLoaders()}。
 *
 * @param <T> 实体类型
 */
public abstract class BaseListView<T> extends StandardListView<T> implements DetailViewCloseCallback, TabActivationAware {

    private static final Logger log = LoggerFactory.getLogger(BaseListView.class);

    /**
     * 手动触发视图中所有 CollectionLoader 加载数据。
     * 正常情况下由 dataLoadCoordinator 自动处理，仅在特殊场景需要手动调用。
     */
    protected void loadAllCollectionLoaders() {
        ViewData viewData = ViewControllerUtils.getViewData(this);
        for (String loaderId : viewData.getLoaderIds()) {
            DataLoader loader = viewData.getLoader(loaderId);
            if (loader instanceof CollectionLoader<?>) {
                loader.load();
            }
        }
    }

    // ==================== DetailViewCloseCallback 默认实现 ====================

    @Override
    public boolean onClose(CloseAction closeAction,
                           @Nullable Object entity,
                           @Nullable CollectionContainer<?> container,
                           @Nullable ListDataComponent<?> sourceGrid,
                           boolean isNew) {
        return DetailViewCloseCallback.super.onClose(closeAction, entity, container, sourceGrid, isNew);
    }

    @Override
    public boolean onSaved(@Nullable Object entity,
                           @Nullable CollectionContainer<?> container,
                           @Nullable ListDataComponent<?> sourceGrid,
                           boolean isNew) {
        return DetailViewCloseCallback.super.onSaved(entity, container, sourceGrid, isNew);
    }

    @Override
    public void afterSaved(@Nullable Object entity,
                           @Nullable ListDataComponent<?> sourceGrid,
                           boolean isNew) {
        DetailViewCloseCallback.super.afterSaved(entity, sourceGrid, isNew);
    }
}
