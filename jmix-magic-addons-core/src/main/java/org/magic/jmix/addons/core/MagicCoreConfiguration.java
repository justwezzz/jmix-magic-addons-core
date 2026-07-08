package org.magic.jmix.addons.core;

import io.jmix.core.CoreConfiguration;
import io.jmix.core.ExtendedEntities;
import io.jmix.core.Metadata;
import io.jmix.core.annotation.JmixModule;
import io.jmix.core.impl.scanning.AnnotationScanMetadataReaderFactory;
import io.jmix.flowui.FlowuiConfiguration;
import io.jmix.flowui.UiViewProperties;
import io.jmix.flowui.Views;
import io.jmix.flowui.Views;
import io.jmix.flowui.sys.ActionsConfiguration;
import io.jmix.flowui.sys.UiAccessChecker;
import io.jmix.flowui.view.ViewRegistry;
import io.jmix.flowui.view.builder.DetailWindowBuilderProcessor;
import io.jmix.flowui.view.builder.EditedEntityTransformer;
import org.magic.jmix.addons.core.view.MagicDetailWindowBuilderProcessor;
import org.magic.jmix.addons.core.view.navigation.DefaultViewNavigationSupport;
import org.magic.jmix.addons.core.view.navigation.ViewNavigationSupport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Jmix Magic Core Addon 配置类。
 */
@Configuration
@ComponentScan
@JmixModule(dependsOn = {CoreConfiguration.class, FlowuiConfiguration.class})
@EnableConfigurationProperties(MagicCoreProperties.class)
public class MagicCoreConfiguration {

    @Bean("magic_DetailWindowBuilderProcessor")
    @Primary
    public DetailWindowBuilderProcessor detailWindowBuilderProcessor(
            ApplicationContext applicationContext,
            Views views,
            ViewRegistry viewRegistry,
            Metadata metadata,
            ExtendedEntities extendedEntities,
            UiViewProperties viewProperties,
            UiAccessChecker uiAccessChecker,
            @Nullable List<EditedEntityTransformer> editedEntityTransformers) {
        return new MagicDetailWindowBuilderProcessor(
                applicationContext, views, viewRegistry, metadata,
                extendedEntities, viewProperties, uiAccessChecker,
                editedEntityTransformers);
    }

    @Bean("magic_Actions")
    public ActionsConfiguration magicActions(ApplicationContext applicationContext,
                                              AnnotationScanMetadataReaderFactory metadataReaderFactory) {
        ActionsConfiguration actionsConfiguration = new ActionsConfiguration(applicationContext, metadataReaderFactory);
        actionsConfiguration.setBasePackages(Collections.singletonList("org.magic.jmix.addons.core.action"));
        return actionsConfiguration;
    }

    @Bean("magic_ViewNavigationSupport")
    public ViewNavigationSupport viewNavigationSupport(Views views) {
        return new DefaultViewNavigationSupport(views);
    }
}
