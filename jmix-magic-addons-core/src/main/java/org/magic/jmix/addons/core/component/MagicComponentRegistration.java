package org.magic.jmix.addons.core.component;

import io.jmix.flowui.component.genericfilter.GenericFilter;
import io.jmix.flowui.component.pagination.SimplePagination;
import io.jmix.flowui.sys.registration.ComponentRegistration;
import io.jmix.flowui.sys.registration.ComponentRegistrationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 魔法组件注册配置。
 * <p>
 * 将增强组件注册为标准组件的替代实现，使用 XML 时自动使用增强版本。
 */
@Configuration
public class MagicComponentRegistration {

    @Bean
    public ComponentRegistration magicGenericFilter() {
        return ComponentRegistrationBuilder.create(MagicGenericFilter.class)
                .replaceComponent(GenericFilter.class)
                .build();
    }

    @Bean
    public ComponentRegistration magicSimplePagination() {
        return ComponentRegistrationBuilder.create(MagicSimplePagination.class)
                .replaceComponent(SimplePagination.class)
                .build();
    }
}