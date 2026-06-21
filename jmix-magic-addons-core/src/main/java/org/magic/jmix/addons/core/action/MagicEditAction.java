package org.magic.jmix.addons.core.action;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.router.QueryParameters;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.list.EditAction;
import io.jmix.flowui.component.ListDataComponent;
import io.jmix.flowui.data.ContainerDataUnit;
import io.jmix.flowui.data.DataUnit;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.CloseAction;
import io.jmix.flowui.view.DetailView;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.navigation.DetailViewNavigator;
import org.magic.jmix.addons.core.view.base.DetailViewCloseCallback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 增强版编辑 Action，统一 TAB 和 DIALOG 模式下的关闭事件流程。
 * <p>
 * 关闭事件时序与 {@link MagicCreateAction} 一致（isNew=false）。
 * <p>
 * 使用 {@code type="magic_list_edit"} 替代标准 {@code list_edit}。
 */
@ActionType(MagicEditAction.ID)
public class MagicEditAction<E> extends EditAction<E> {

    public static final String ID = "magic_list_edit";
    private static final String AFTER_SAVE_HANDLER_KEY = "magic_afterSaveHandler";

    protected DialogWindows dialogWindows;

    public MagicEditAction() {
        super(ID);
    }

    public MagicEditAction(String id) {
        super(id);
    }

    @Autowired
    @Override
    public void setDialogWindowBuilders(DialogWindows dialogWindows) {
        this.dialogWindows = dialogWindows;
        super.setDialogWindowBuilders(dialogWindows);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void navigate(E editedEntity) {
        DetailViewNavigator<E> navigator = viewNavigators.detailView(target)
                .editEntity(editedEntity)
                .withBackwardNavigation(true);

        if (target instanceof Component component) {
            String gridId = component.getId().orElse(null);
            if (gridId != null) {
                QueryParameters queryParams = QueryParameters.simple(
                        Map.of("_pgrid", gridId, "_mode", "edit"));
                navigator = (DetailViewNavigator<E>) navigator
                        .withQueryParameters(queryParams);
            }

            // TAB 模式：将 afterSaveHandler 暂存到 Grid 组件上，供 TabDetailProcessor 读取
            Consumer<E> handler = getAfterSaveHandler();
            if (handler != null) {
                ComponentUtil.setData(component, AFTER_SAVE_HANDLER_KEY, handler);
            }
        }

        navigator.navigate();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void openDialog(E editedEntity) {
        var builder = dialogWindows.detail(target);

        String viewId = getViewId();
        if (viewId != null) {
            builder = builder.withViewId(viewId);
        }

        Class<? extends View> viewClass = getViewClass();
        if (viewClass != null) {
            builder = builder.withViewClass((Class) viewClass);
        }

        Consumer afterCloseHandler = getAfterCloseHandler();
        if (afterCloseHandler != null) {
            builder = builder.withAfterCloseListener(afterCloseHandler);
        }

        Function<E, E> transformation = getTransformation();
        if (transformation != null) {
            builder = builder.withTransformation(transformation);
        }

        DialogWindow<?> dialogWindow = builder.build();

        dialogWindow.addAfterCloseListener(event -> {
            if (!event.closedWith(StandardOutcome.SAVE)) return;
            if (!(event.getView() instanceof DetailView)) return;

            E savedEntity = (E) ((DetailView) event.getView()).getEditedEntity();
            if (savedEntity == null) return;

            if (target instanceof Component component) {
                View<?> parentView = UiComponentUtils.findView(component);
                if (parentView instanceof DetailViewCloseCallback) {
                    DetailViewCloseCallback callback = (DetailViewCloseCallback) parentView;
                    fireCallbackChain(callback, event.getCloseAction(), savedEntity,
                            component, false);
                }
            }

            Consumer<E> handler = getAfterSaveHandler();
            if (handler != null) {
                handler.accept(savedEntity);
            }
        });

        dialogWindow.open();
    }

    protected void fireCallbackChain(DetailViewCloseCallback callback,
                                      CloseAction closeAction,
                                      Object entity, Component grid, boolean isNew) {
        CollectionContainer<?> container = extractContainer(grid);
        ListDataComponent<?> ldc = (grid instanceof ListDataComponent<?>) ? (ListDataComponent<?>) grid : null;

        // Phase 1: onClose
        boolean useDefault = callback.onClose(closeAction, entity, container, ldc, isNew);
        if (!useDefault) return;

        // Phase 2: onSaved（DIALOG 模式下 Jmix 已处理容器操作）
        callback.onSaved(entity, container, ldc, isNew);

        // Phase 3: afterSaved
        callback.afterSaved(entity, ldc, isNew);
    }

    protected CollectionContainer<?> extractContainer(Component grid) {
        if (grid instanceof ListDataComponent<?> ldc) {
            DataUnit items = ldc.getItems();
            if (items instanceof ContainerDataUnit<?>) {
                return ((ContainerDataUnit<?>) items).getContainer();
            }
        }
        return null;
    }
}
