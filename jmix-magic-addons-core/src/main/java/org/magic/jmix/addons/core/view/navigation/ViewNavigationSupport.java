package org.magic.jmix.addons.core.view.navigation;

import io.jmix.flowui.view.View;

/**
 * 视图导航支持接口，提供跨视图传数据和回调的能力。
 * <p>
 * Core addon 提供默认实现（基于 Jmix 原生 viewNavigators），
 * 其他插件可通过实现该接口提供覆盖实现（使用 {@code @ConditionalOnMissingBean} 机制自动覆盖）。
 * 调用方注入 {@code ViewNavigationSupport} 即可，无需感知底层实现差异。
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
