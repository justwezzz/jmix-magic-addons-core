package org.magic.jmix.addons.core.view.base;

import io.jmix.flowui.component.ListDataComponent;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.CloseAction;
import jakarta.annotation.Nullable;

/**
 * DetailView 关闭回调接口，统一 TAB 和 DIALOG 两种模式的事件处理流程。
 * <p>
 * ListView 实现此接口后，DetailView 关闭时自动触发四阶段回调：
 * <ol>
 *   <li>Phase 1: {@code onClose()} — 任何关闭都触发</li>
 *   <li>Phase 2: {@code onSaved()} — 仅 SAVE 触发，处理数据逻辑</li>
 *   <li>Phase 3: {@code afterSaved()} — 仅 SAVE 触发，处理 UI 逻辑</li>
 *   <li>Phase 4: {@code afterSaveHandler} — Action 级别的 ListView 后处理</li>
 * </ol>
 * Phase 1-3 在 TabDetailProcessor / MagicAction 中触发，Phase 4 由 Action 或 ComponentUtil 桥接触发。
 * <p>
 * 推荐模式：onSaved 处理数据逻辑，afterSaved 处理 UI 逻辑，afterSaveHandler 针对整个 ListView。
 */
public interface DetailViewCloseCallback {

    /**
     * Phase 1: 关闭回调（任何关闭都触发）。
     * <p>
     * 前置：无（这是第一个回调）。后置：{@link #onSaved}（仅 SAVE）。
     * <p>
     * 返回 true 继续后续阶段；返回 false 完全接管关闭逻辑，跳过所有后续阶段。
     *
     * @param closeAction 关闭动作（StandardOutcome.SAVE、CLOSE、DISCARD 等）
     * @param entity      保存的实体（未保存时为 null）
     * @param container   目标容器（MetaClass Grid 时为 null）
     * @param sourceGrid  触发导航的 Grid
     * @param isNew       是否是新建
     * @return true 继续后续阶段，false 中断流程
     */
    default boolean onClose(CloseAction closeAction,
                             @Nullable Object entity,
                             @Nullable CollectionContainer<?> container,
                             @Nullable ListDataComponent<?> sourceGrid,
                             boolean isNew) {
        return true;
    }

    /**
     * Phase 2: 数据逻辑回调（仅 SAVE 触发）。
     * <p>
     * 前置：{@link #onClose}。后置：{@link #afterSaved}。
     * <p>
     * 推荐在此方法中处理数据逻辑（如刷新数据源）。
     * <p>
     * 返回 true 使用默认容器操作（add/replaceItem）；返回 false 跳过默认容器操作（如 MetaClass Grid 需自行 refreshAll）。
     *
     * @param entity      保存的实体
     * @param container   目标容器（MetaClass Grid 时为 null）
     * @param sourceGrid  触发导航的 Grid
     * @param isNew       是否是新建
     * @return true 使用默认容器操作，false 跳过
     */
    default boolean onSaved(@Nullable Object entity,
                             @Nullable CollectionContainer<?> container,
                             @Nullable ListDataComponent<?> sourceGrid,
                             boolean isNew) {
        return true;
    }

    /**
     * Phase 3: UI 逻辑回调（仅 SAVE 触发）。
     * <p>
     * 前置：{@link #onSaved}。后置：afterSaveHandler（Action 级别）。
     * <p>
     * 推荐在此方法中处理 UI 逻辑（如滚动、选中、焦点）。
     * <p>
     * 默认行为：新建时滚动 Grid 到顶部。
     *
     * @param entity      保存的实体
     * @param sourceGrid  触发导航的 Grid
     * @param isNew       是否是新建
     */
    default void afterSaved(@Nullable Object entity,
                             @Nullable ListDataComponent<?> sourceGrid,
                             boolean isNew) {
        if (isNew && sourceGrid instanceof DataGrid<?> grid) {
            grid.scrollToIndex(0);
        }
    }
}
