package org.magic.jmix.addons.core.view.navigation;

import io.jmix.flowui.view.View;

import java.util.function.Consumer;

/**
 * 视图导航构建器，提供链式回调配置。
 * <p>
 * 使用方式：
 * <pre>
 * viewNavigationSupport.open(MyView.class)
 *     .withAfterViewCreated(view -> view.setProfile(profile))
 *     .withAfterViewClosed(event -> { ... })
 *     .navigate();
 * </pre>
 *
 * @param <V> 目标视图类型
 */
public class ViewNavigationBuilder<V extends View<?>> {

    protected final Class<V> viewClass;
    protected Consumer<V> afterViewCreatedCallback;
    protected Consumer<AfterViewClosedEvent<V>> afterViewClosedCallback;

    public ViewNavigationBuilder(Class<V> viewClass) {
        this.viewClass = viewClass;
    }

    /**
     * 注册一次性回调，在目标视图创建后、BeforeShowEvent 触发前执行。
     * <p>
     * 覆盖 Jmix 原生 {@code withAfterNavigationHandler} + {@code withViewConfigurer} 的使用场景。
     *
     * @param callback 回调函数，接收已创建的视图实例
     * @return this 实例，支持链式调用
     */
    public ViewNavigationBuilder<V> withAfterViewCreated(Consumer<V> callback) {
        this.afterViewCreatedCallback = callback;
        return this;
    }

    /**
     * 注册一次性关闭回调，在目标视图的 AfterCloseEvent 触发时执行。
     *
     * @param callback 回调函数，接收 AfterViewClosedEvent
     * @return this 实例，支持链式调用
     */
    public ViewNavigationBuilder<V> withAfterViewClosed(Consumer<AfterViewClosedEvent<V>> callback) {
        this.afterViewClosedCallback = callback;
        return this;
    }

    /**
     * 执行导航。子类覆盖此方法提供具体的导航实现。
     */
    public void navigate() {
        throw new UnsupportedOperationException("navigate() must be overridden by subclass");
    }
}
