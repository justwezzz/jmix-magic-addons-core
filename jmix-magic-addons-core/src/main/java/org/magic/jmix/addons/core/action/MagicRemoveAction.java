package org.magic.jmix.addons.core.action;

import com.vaadin.flow.component.Component;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.action.list.RemoveAction;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.ContainerDataUnit;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.View;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Set;

/**
 * 通用删除 Action，兼容 Container 模式和 MetaClass 模式。
 * <p>
 * Container 模式（含 LazyGrid）委托给标准 {@link RemoveAction}，通过 DataContext 删除。
 * MetaClass 模式使用 {@link DataManager#remove(Id)} 删除并刷新 DataProvider。
 * <p>
 * MetaClass 模式下 Grid 的 SelectionModel 可能持有编辑前的旧实体引用（version 过期），
 * 直接 remove 会因 {@code em.merge()} 乐观锁版本不匹配而抛异常。
 * 因此先通过 {@code dataManager.load(Id.of(entity))} 重新加载最新实体，确保版本一致后再删除。
 * <p>
 * 使用 {@code type="magic_list_remove"} 替代标准 {@code list_remove}。
 */
@ActionType(MagicRemoveAction.ID)
public class MagicRemoveAction<E> extends RemoveAction<E> {

    public static final String ID = "magic_list_remove";

    @Autowired
    private DataManager dataManager;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private Messages messages;

    public MagicRemoveAction() {
        super(ID);
    }

    public MagicRemoveAction(String id) {
        super(id);
    }

    @Override
    protected boolean isPermitted() {
        if (target == null) return false;
        if (target.getItems() instanceof ContainerDataUnit) {
            return super.isPermitted();
        }
        return true;
    }

    @Override
    public void execute() {
        if (target.getItems() instanceof ContainerDataUnit) {
            super.execute();
            return;
        }

        Set<E> selected = target.getSelectedItems();
        if (selected.isEmpty()) return;

        if (!(target instanceof Component component)) return;
        View<?> view = UiComponentUtils.findView(component);
        if (view == null) return;

        String header = getConfirmationHeader() != null
                ? getConfirmationHeader()
                : messages.getMessage("dialogs.Confirmation");
        String text = getConfirmationText() != null
                ? getConfirmationText()
                : messages.getMessage("dialogs.Confirmation.Remove");

        dialogs.createOptionDialog()
                .withHeader(header)
                .withText(text)
                .withActions(
                        new DialogAction(DialogAction.Type.YES).withHandler(e -> {
                            if (beforeActionPerformedHandler != null) {
                                RemoveOperation.BeforeActionPerformedEvent<E> beforeEvent =
                                        new RemoveOperation.BeforeActionPerformedEvent<>(
                                                view, new ArrayList<>(selected));
                                beforeActionPerformedHandler.accept(beforeEvent);
                                if (beforeEvent.isActionPrevented()) return;
                            }
                            // MetaClass Grid 的 SelectionModel 可能持有编辑前的旧实体（version 过期），
                            // JpaDataStore.deleteAll() 内部 em.merge() 会对比版本导致 OptimisticLockException。
                            // 先 load 拿到最新版本，再 remove，与 Container 模式的行为等价。
                            selected.forEach(entity ->
                                    dataManager.load(Id.of(entity)).optional().ifPresent(dataManager::remove)
                            );
                            if (target instanceof DataGrid<?> grid) {
                                grid.deselectAll();
                                grid.getDataProvider().refreshAll();
                            }
                            if (afterActionPerformedHandler != null) {
                                afterActionPerformedHandler.accept(
                                        new RemoveOperation.AfterActionPerformedEvent<>(
                                                view, new ArrayList<>(selected)));
                            }
                        }),
                        new DialogAction(DialogAction.Type.NO)
                )
                .open();
    }
}
