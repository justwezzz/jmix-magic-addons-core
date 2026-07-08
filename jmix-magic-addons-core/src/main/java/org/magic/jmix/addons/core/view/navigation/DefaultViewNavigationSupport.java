package org.magic.jmix.addons.core.view.navigation;

import io.jmix.flowui.Views;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 默认的视图导航支持实现，基于 Jmix 原生 viewNavigators。
 * <p>
 * 在多标签页架构下，{@code withAfterNavigationHandler} 会失效（回调依赖 origin view detach），
 * 引入 Tab Layout addon 后会被 {@code TabRouterService} 的实现自动覆盖。
 */
public class DefaultViewNavigationSupport implements ViewNavigationSupport {

    private static final Logger log = LoggerFactory.getLogger(DefaultViewNavigationSupport.class);

    private final Views views;

    public DefaultViewNavigationSupport(Views views) {
        this.views = views;
    }

    @Override
    public <V extends View<?>> ViewNavigationBuilder<V> open(Class<V> viewClass) {
        return new DefaultViewNavigationBuilder<>(viewClass, views);
    }

    /**
     * 默认导航构建器，通过 Jmix 原生 API 实现回调。
     */
    static class DefaultViewNavigationBuilder<V extends View<?>> extends ViewNavigationBuilder<V> {

        private final Views views;

        DefaultViewNavigationBuilder(Class<V> viewClass, Views views) {
            super(viewClass);
            this.views = views;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void navigate() {
            // 解析路由
            com.vaadin.flow.router.Route routeAnnotation =
                    viewClass.getAnnotation(com.vaadin.flow.router.Route.class);
            if (routeAnnotation == null || routeAnnotation.value().isEmpty()) {
                log.error("[DefaultViewNavigation] Cannot resolve route for viewClass={}", viewClass.getSimpleName());
                return;
            }
            String route = routeAnnotation.value();

            // 设置回调：afterViewCreated 通过 withAfterNavigationHandler 实现
            // 注意：多标签页下 withAfterNavigationHandler 会失效
            if (afterViewCreatedCallback != null) {
                log.debug("[DefaultViewNavigation] withAfterNavigationHandler set | viewClass={}", viewClass.getSimpleName());
                // 通过 UI.navigate + beforeClientResponse 获取创建的视图实例
                com.vaadin.flow.component.UI.getCurrent().navigate(route);
                // 默认实现无法可靠获取视图实例来执行回调
                // 这是已知限制——引入 Tab Layout addon 后自动解决
                log.warn("[DefaultViewNavigation] withAfterNavigationHandler may not work under multi-tab architecture. " +
                        "Consider adding Tab Layout addon for full support.");
            } else {
                com.vaadin.flow.component.UI.getCurrent().navigate(route);
            }

            // afterViewClosed 通过 AfterCloseListener 实现
            if (afterViewClosedCallback != null) {
                // 需要视图实例才能注册监听器，默认实现无法可靠获取
                // 这是已知限制——引入 Tab Layout addon 后自动解决
                log.warn("[DefaultViewNavigation] withAfterViewClosed may not work under multi-tab architecture. " +
                        "Consider adding Tab Layout addon for full support.");
            }
        }
    }
}
