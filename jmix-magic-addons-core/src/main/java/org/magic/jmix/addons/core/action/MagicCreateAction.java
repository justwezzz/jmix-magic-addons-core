package org.magic.jmix.addons.core.action;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.router.QueryParameters;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.list.CreateAction;
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
import jakarta.annotation.Nullable;
import org.magic.jmix.addons.core.view.base.DetailViewCloseCallback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 增强版新建 Action，统一 TAB 和 DIALOG 模式下的关闭事件流程。
 * <p>
 * TAB 模式：添加 {@code _pgrid} 路由参数，通过 ComponentUtil 桥接 afterSaveHandler。
 * DIALOG 模式：Jmix 处理容器操作后，触发 {@link DetailViewCloseCallback} 三阶段回调。
 * <p>
 * 关闭事件时序（两种模式一致）：
 * <ol>
 *   <li>Phase 1: {@code onClose()} — 任何关闭都触发，return false 中断流程</li>
 *   <li>Phase 2: {@code onSaved()} — 仅 SAVE 触发，处理数据逻辑</li>
 *   <li>Phase 3: {@code afterSaved()} — 仅 SAVE 触发，处理 UI 逻辑</li>
 *   <li>Phase 4: {@code afterSaveHandler} — Action 级别的 listview 后处理</li>
 * </ol>
 * <p>
 * 使用 {@code type="magic_list_create"} 替代标准 {@code list_create}。
 */
@ActionType(MagicCreateAction.ID)
public class MagicCreateAction<E> extends CreateAction<E> {

    public static final String ID = "magic_list_create";

    private static final String AFTER_SAVE_HANDLER_KEY = "magic_afterSaveHandler";

    protected DialogWindows dialogWindows;

    public MagicCreateAction() {
        super(ID);
    }

    public MagicCreateAction(String id) {
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
    protected void navigate() {
        DetailViewNavigator<E> navigator = viewNavigators.detailView(target)
                .newEntity()
                .withBackwardNavigation(true);

        if (target instanceof Component component) {
            String gridId = component.getId().orElse(null);
            if (gridId != null) {
                QueryParameters queryParams = QueryParameters.simple(
                        Map.of("_pgrid", gridId));
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
    protected void openDialog() {
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

        Consumer viewConfigurer = getViewConfigurer();
        if (viewConfigurer != null) {
            builder = builder.withViewConfigurer(viewConfigurer);
        }

        Supplier<E> supplier = getNewEntitySupplier();
        if (supplier != null) {
            builder = builder.newEntity(supplier.get());
        } else {
            builder = builder.newEntity();
        }

        Consumer<E> initializer = getInitializer();
        if (initializer != null) {
            builder = builder.withInitializer(initializer);
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
                            component, true);
                }
            }

            Consumer<E> handler = getAfterSaveHandler();
            if (handler != null) {
                handler.accept(savedEntity);
            }
        });

        dialogWindow.open();
    }

    /**
     * 按统一时序触发 DetailViewCloseCallback 三阶段回调。
     * <p>
     * Phase 1: onClose — return false 中断
     * Phase 2: onSaved — 数据逻辑（DIALOG 模式下 Jmix 已处理容器操作）
     * Phase 3: afterSaved — UI 逻辑
     */
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

    @Nullable
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
