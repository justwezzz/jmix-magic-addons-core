package org.magic.jmix.addons.core.view.navigation;

import io.jmix.flowui.view.View;

/**
 * 视图导航支持接口，提供跨视图传数据和回调的能力。
 * <p>
 * Core addon 提供默认实现（基于 Jmix 原生 viewNavigators），
 * Tab Layout addon 提供覆盖实现（基于 TabRouterService）。
 * 宿主项目引入 Tab Layout 后自动使用覆盖实现，调用方无需感知底层差异。
 * <p>
 * 使用方式：
 * <pre>
 * &#64;Autowired
 * private ViewNavigationSupport viewNavigationSupport;
 *
 * viewNavigationSupport.open(MyView.class)
 *     .withAfterViewCreated(view -> view.setProfile(profile))
 *     .withAfterViewClosed(event -> { ... })
 *     .navigate();
 * </pre>
 */
public interface ViewNavigationSupport {

    /**
     * 打开指定视图，返回构建器用于配置回调后导航。
     *
     * @param viewClass 目标视图类
     * @param <V>       视图类型
     * @return ViewNavigationBuilder 实例，支持链式配置
     */
    <V extends View<?>> ViewNavigationBuilder<V> open(Class<V> viewClass);
}
