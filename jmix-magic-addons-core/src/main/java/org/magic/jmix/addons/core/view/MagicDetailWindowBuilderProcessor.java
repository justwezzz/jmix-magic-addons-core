package org.magic.jmix.addons.core.view;

import com.vaadin.flow.component.Focusable;
import io.jmix.core.ExtendedEntities;
import io.jmix.core.Metadata;
import io.jmix.flowui.UiViewProperties;
import io.jmix.flowui.Views;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.Nested;
import io.jmix.flowui.sys.UiAccessChecker;
import io.jmix.flowui.view.*;
import io.jmix.flowui.view.builder.DetailWindowBuilder;
import io.jmix.flowui.view.builder.DetailWindowBuilderProcessor;
import io.jmix.flowui.view.builder.EditedEntityTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * 自定义 DetailWindowBuilderProcessor，修复 Dialog 模式下未实际持久化时仍 replaceItem 的问题。
 * <p>
 * 通过 {@code @Primary} Bean 替换框架默认实现。
 */
public class MagicDetailWindowBuilderProcessor extends DetailWindowBuilderProcessor {

    private static final Logger log = LoggerFactory.getLogger(MagicDetailWindowBuilderProcessor.class);

    public MagicDetailWindowBuilderProcessor(ApplicationContext applicationContext,
                                              Views views,
                                              ViewRegistry viewRegistry,
                                              Metadata metadata,
                                              ExtendedEntities extendedEntities,
                                              UiViewProperties viewProperties,
                                              UiAccessChecker uiAccessChecker,
                                              @Nullable List<EditedEntityTransformer> editedEntityTransformers) {
        super(applicationContext, views, viewRegistry, metadata, extendedEntities,
                viewProperties, uiAccessChecker, editedEntityTransformers);
    }

    @Override
    protected <E, V extends View<?>> void setupListDataComponent(
            DetailWindowBuilder<E, V> builder,
            DetailView<E> detailView, DialogWindow<V> dialog,
            @Nullable CollectionContainer<E> container,
            @Nullable DataContext parentDataContext) {

        if (container == null) {
            return;
        }

        // Track whether DataContext actually persisted entities
        boolean[] persisted = {true}; // conservative default
        if (detailView instanceof View<?> view) {
            DataContext detailDC = ViewControllerUtils.getViewData(view).getDataContextOrNull();
            if (detailDC != null) {
                detailDC.addPostSaveListener(postSaveEvent -> {
                    boolean hasSaved = !postSaveEvent.getSavedInstances().isEmpty();
                    log.debug("[Dialog-PostSave] persisted={} | savedCount={}", hasSaved, postSaveEvent.getSavedInstances().size());
                    persisted[0] = hasSaved;
                });
            } else {
                log.debug("[Dialog-Setup] detailDC is null, using conservative default persisted=true");
            }
        }

        detailView.setReloadSaved(true);

        dialog.addAfterCloseListener(closeEvent -> {
            if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                E entityFromDetail = getSavedEntity(detailView, parentDataContext);
                E reloadedEntity = transformForCollectionContainer(entityFromDetail, container);
                E savedEntity = transform(reloadedEntity, builder);
                E mergedEntity = merge(savedEntity, builder.getOrigin(), parentDataContext, container);

                if (builder.getMode() == DetailViewMode.CREATE) {
                    boolean addsFirst = false;

                    if (!(container instanceof Nested)) {
                        addsFirst = viewProperties.isCreateActionAddsFirst();
                        if (builder.getAddFirst() != null) {
                            addsFirst = builder.getAddFirst();
                        }
                    }

                    if (container instanceof Nested || !addsFirst) {
                        container.getMutableItems().add(mergedEntity);
                    } else {
                        container.getMutableItems().add(0, mergedEntity);
                    }
                    log.debug("[Dialog-AfterClose] ADD (CREATE) | containerSize={}", container.getItems().size());
                } else if (persisted[0]) {
                    container.replaceItem(mergedEntity);
                    log.debug("[Dialog-AfterClose] REPLACE | persisted=true | containerSize={}", container.getItems().size());
                } else {
                    log.debug("[Dialog-AfterClose] SKIP REPLACE: not actually persisted");
                }
            }
        });
    }
}
