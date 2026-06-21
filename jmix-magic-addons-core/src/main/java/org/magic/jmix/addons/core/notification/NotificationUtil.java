package org.magic.jmix.addons.core.notification;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.Messages;
import org.magic.jmix.addons.core.MagicCoreProperties;
import org.springframework.context.ApplicationContext;

/**
 * 通知工具类
 * <p>
 * 统一管理通知的样式、位置与时长。位置支持两级覆盖：全局 {@code notification-default-position}
 * 与可选的按类型 {@code notification-{type}-position}（未配置时回落全局默认）。
 * 时长与位置通过 {@link MagicCoreProperties}（{@code magic.core.*}）配置，支持热修改。
 * 标题经 Jmix {@link Messages} 国际化（addon 根 messages）。
 * <p>
 * 调用方无需注入任何 bean：
 * <pre>
 * NotificationUtil.success("保存成功");
 * NotificationUtil.error("自定义标题", "失败原因");
 * </pre>
 */
public final class NotificationUtil {

    /** Spring 上下文，用于获取配置与消息 Bean */
    private static ApplicationContext applicationContext;

    private NotificationUtil() {
    }

    /** 供 {@link ContextLoader} 注入上下文（包级私有，仅同包可访问） */
    static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    private static MagicCoreProperties getConfig() {
        return applicationContext != null ? applicationContext.getBean(MagicCoreProperties.class) : null;
    }

    private static Messages getMessages() {
        return applicationContext != null ? applicationContext.getBean(Messages.class) : null;
    }

    private static String getTitle(String type) {
        Messages messages = getMessages();
        return messages != null ? messages.getMessage(NotificationUtil.class, "title." + type) : "title." + type;
    }

    /** 默认标题：仅在 {@code show-default-title=true} 时返回 i18n 标题，否则返回 null（不显示标题） */
    private static String defaultTitle(String type) {
        return shouldShowDefaultTitle() ? getTitle(type) : null;
    }

    private static boolean shouldShowDefaultTitle() {
        MagicCoreProperties config = getConfig();
        return config != null && config.isShowDefaultTitle();
    }

    public static void success(String text) {
        show(NotificationVariant.LUMO_SUCCESS, defaultTitle("success"), text,
                getSuccessDuration(), getSuccessPosition());
    }

    public static void success(String title, String text) {
        show(NotificationVariant.LUMO_SUCCESS, title, text,
                getSuccessDuration(), getSuccessPosition());
    }

    public static void error(String text) {
        show(NotificationVariant.LUMO_ERROR, defaultTitle("error"), text,
                getErrorDuration(), getErrorPosition());
    }

    public static void error(String title, String text) {
        show(NotificationVariant.LUMO_ERROR, title, text,
                getErrorDuration(), getErrorPosition());
    }

    public static void warning(String text) {
        show(NotificationVariant.LUMO_WARNING, defaultTitle("warning"), text,
                getWarningDuration(), getWarningPosition());
    }

    public static void warning(String title, String text) {
        show(NotificationVariant.LUMO_WARNING, title, text,
                getWarningDuration(), getWarningPosition());
    }

    public static void info(String text) {
        show(NotificationVariant.LUMO_PRIMARY, defaultTitle("info"), text,
                getInfoDuration(), getInfoPosition());
    }

    public static void info(String title, String text) {
        show(NotificationVariant.LUMO_PRIMARY, title, text,
                getInfoDuration(), getInfoPosition());
    }

    private static int getSuccessDuration() {
        MagicCoreProperties config = getConfig();
        return config != null ? config.getNotificationSuccessDuration() : 5000;
    }

    private static int getErrorDuration() {
        MagicCoreProperties config = getConfig();
        return config != null ? config.getNotificationErrorDuration() : 8000;
    }

    private static int getWarningDuration() {
        MagicCoreProperties config = getConfig();
        return config != null ? config.getNotificationWarningDuration() : 8000;
    }

    private static int getInfoDuration() {
        MagicCoreProperties config = getConfig();
        return config != null ? config.getNotificationInfoDuration() : 5000;
    }

    private static Notification.Position getSuccessPosition() {
        MagicCoreProperties config = getConfig();
        if (config == null) {
            return Notification.Position.BOTTOM_END;
        }
        Notification.Position p = config.getNotificationSuccessPosition();
        return p != null ? p : config.getNotificationDefaultPosition();
    }

    private static Notification.Position getErrorPosition() {
        MagicCoreProperties config = getConfig();
        if (config == null) {
            return Notification.Position.BOTTOM_END;
        }
        Notification.Position p = config.getNotificationErrorPosition();
        return p != null ? p : config.getNotificationDefaultPosition();
    }

    private static Notification.Position getWarningPosition() {
        MagicCoreProperties config = getConfig();
        if (config == null) {
            return Notification.Position.BOTTOM_END;
        }
        Notification.Position p = config.getNotificationWarningPosition();
        return p != null ? p : config.getNotificationDefaultPosition();
    }

    private static Notification.Position getInfoPosition() {
        MagicCoreProperties config = getConfig();
        if (config == null) {
            return Notification.Position.BOTTOM_END;
        }
        Notification.Position p = config.getNotificationInfoPosition();
        return p != null ? p : config.getNotificationDefaultPosition();
    }

    private static void show(NotificationVariant variant, String title, String text,
                             int durationMs, Notification.Position position) {
        Notification notification = new Notification();
        notification.addThemeVariants(variant);
        notification.setPosition(position);
        notification.setDuration(durationMs);

        Div content = new Div(new Text(text));

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        // title 为 null 时不渲染标题（单参数 API 默认不显示标题）
        if (title != null) {
            Span titleSpan = new Span(title);
            titleSpan.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("margin", "-6px 0 0 -16px")
                    .set("font-weight", "600");
            layout.add(titleSpan);
        }
        layout.add(content);

        notification.add(layout);
        notification.open();
    }
}
