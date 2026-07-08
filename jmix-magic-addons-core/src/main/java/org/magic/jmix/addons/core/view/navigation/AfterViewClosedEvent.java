package org.magic.jmix.addons.core.view.navigation;

import io.jmix.flowui.view.CloseAction;
import io.jmix.flowui.view.View;

/**
 * 视图关闭后触发的事件，由 {@link ViewNavigationBuilder#withAfterViewClosed} 注册的回调消费。
 * <p>
 * 与 {@link View.AfterCloseEvent} 的区别：
 * <ul>
 *   <li>AfterCloseEvent — 视图内部永久监听，适合视图自身清理逻辑</li>
 *   <li>AfterViewClosedEvent — 调用方一次性回调，适合跨视图协作（如打开方接收关闭结果）</li>
 * </ul>
 *
 * @param <V> 视图类型
 */
public class AfterViewClosedEvent<V extends View<?>> {

    private final V view;
    private final CloseAction closeAction;

    public AfterViewClosedEvent(V view, CloseAction closeAction) {
        this.view = view;
        this.closeAction = closeAction;
    }

    /**
     * @return 已关闭的视图实例
     */
    public V getView() {
        return view;
    }

    /**
     * @return 关闭动作，与 {@link View.AfterCloseEvent#getCloseAction()} 一致
     */
    public CloseAction getCloseAction() {
        return closeAction;
    }

    /**
     * 检查视图是否以指定的 {@link io.jmix.flowui.view.StandardOutcome} 关闭。
     *
     * @see View.AfterCloseEvent#closedWith(io.jmix.flowui.view.StandardOutcome)
     */
    public boolean closedWith(io.jmix.flowui.view.StandardOutcome outcome) {
        return outcome.getCloseAction().equals(closeAction);
    }
}
