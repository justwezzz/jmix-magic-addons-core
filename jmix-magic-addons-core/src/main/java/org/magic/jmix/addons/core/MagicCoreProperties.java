package org.magic.jmix.addons.core;

import com.vaadin.flow.component.notification.Notification;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Core addon 通用配置属性。
 * <p>
 * 绑定 {@code magic.core.*} 前缀，作为整个 core addon 的统一配置入口，
 * 兼容宿主配置管理的热修改机制。采用 setter 绑定（字段初始值即默认值，未配置项保持初始值）。
 * <p>
 * 配置按功能域分组：
 * <ul>
 *   <li><b>通知域</b>：各类型自动关闭时长、显示位置（全局默认 + 按类型覆盖）、
 *       是否显示默认标题，供 {@code org.magic.jmix.addons.core.notification.NotificationUtil} 使用</li>
 *   <li><b>懒加载域</b>：{@code lazy-page-size}，供 datagrid 懒加载组件使用</li>
 * </ul>
 * <p>
 * 配置项（{@code application.properties}）：
 * <pre>
 * # 通知域
 * magic.core.notification-success-duration=5000
 * magic.core.notification-error-duration=8000
 * magic.core.notification-warning-duration=8000
 * magic.core.notification-info-duration=5000
 * magic.core.notification-default-position=BOTTOM_END
 * # 以下按类型位置为可选，未配置时回落 default-position
 * magic.core.notification-error-position=TOP_CENTER
 * # 是否显示默认标题（i18n 标题），默认 false
 * magic.core.notification-show-default-title=false
 * # 懒加载域
 * magic.core.lazy-page-size=50
 * </pre>
 */
@ConfigurationProperties(prefix = "magic.core")
public class MagicCoreProperties {

    /** 成功通知自动关闭时间（毫秒），默认 5000 */
    private int notificationSuccessDuration = 5000;

    /** 错误通知自动关闭时间（毫秒），默认 8000 */
    private int notificationErrorDuration = 8000;

    /** 警告通知自动关闭时间（毫秒），默认 8000 */
    private int notificationWarningDuration = 8000;

    /** 信息通知自动关闭时间（毫秒），默认 5000 */
    private int notificationInfoDuration = 5000;

    /** 通知全局默认位置，默认 BOTTOM_END */
    private Notification.Position notificationDefaultPosition = Notification.Position.BOTTOM_END;

    /** 成功通知位置（可选，未配置回落 default-position） */
    private Notification.Position notificationSuccessPosition;

    /** 错误通知位置（可选，未配置回落 default-position） */
    private Notification.Position notificationErrorPosition;

    /** 警告通知位置（可选，未配置回落 default-position） */
    private Notification.Position notificationWarningPosition;

    /** 信息通知位置（可选，未配置回落 default-position） */
    private Notification.Position notificationInfoPosition;

    /** 是否显示默认标题（i18n 标题），默认 false（不显示） */
    private boolean showDefaultTitle = false;

    /** 懒加载页大小，默认 50 */
    private int lazyPageSize = 50;

    public int getNotificationSuccessDuration() {
        return notificationSuccessDuration;
    }

    public void setNotificationSuccessDuration(int notificationSuccessDuration) {
        this.notificationSuccessDuration = notificationSuccessDuration;
    }

    public int getNotificationErrorDuration() {
        return notificationErrorDuration;
    }

    public void setNotificationErrorDuration(int notificationErrorDuration) {
        this.notificationErrorDuration = notificationErrorDuration;
    }

    public int getNotificationWarningDuration() {
        return notificationWarningDuration;
    }

    public void setNotificationWarningDuration(int notificationWarningDuration) {
        this.notificationWarningDuration = notificationWarningDuration;
    }

    public int getNotificationInfoDuration() {
        return notificationInfoDuration;
    }

    public void setNotificationInfoDuration(int notificationInfoDuration) {
        this.notificationInfoDuration = notificationInfoDuration;
    }

    public Notification.Position getNotificationDefaultPosition() {
        return notificationDefaultPosition;
    }

    public void setNotificationDefaultPosition(Notification.Position notificationDefaultPosition) {
        this.notificationDefaultPosition = notificationDefaultPosition;
    }

    public Notification.Position getNotificationSuccessPosition() {
        return notificationSuccessPosition;
    }

    public void setNotificationSuccessPosition(Notification.Position notificationSuccessPosition) {
        this.notificationSuccessPosition = notificationSuccessPosition;
    }

    public Notification.Position getNotificationErrorPosition() {
        return notificationErrorPosition;
    }

    public void setNotificationErrorPosition(Notification.Position notificationErrorPosition) {
        this.notificationErrorPosition = notificationErrorPosition;
    }

    public Notification.Position getNotificationWarningPosition() {
        return notificationWarningPosition;
    }

    public void setNotificationWarningPosition(Notification.Position notificationWarningPosition) {
        this.notificationWarningPosition = notificationWarningPosition;
    }

    public Notification.Position getNotificationInfoPosition() {
        return notificationInfoPosition;
    }

    public void setNotificationInfoPosition(Notification.Position notificationInfoPosition) {
        this.notificationInfoPosition = notificationInfoPosition;
    }

    public boolean isShowDefaultTitle() {
        return showDefaultTitle;
    }

    public void setShowDefaultTitle(boolean showDefaultTitle) {
        this.showDefaultTitle = showDefaultTitle;
    }

    public int getLazyPageSize() {
        return lazyPageSize;
    }

    public void setLazyPageSize(int lazyPageSize) {
        this.lazyPageSize = lazyPageSize;
    }
}
