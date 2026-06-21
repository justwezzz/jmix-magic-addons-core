package org.magic.jmix.addons.core.exception;

import io.jmix.core.JmixOrder;
import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.data.exception.UniqueConstraintViolationException;
import io.jmix.flowui.exception.UiExceptionHandler;
import jakarta.persistence.PersistenceException;
import org.apache.commons.lang3.StringUtils;
import org.magic.jmix.addons.core.notification.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用唯一约束异常处理器。
 * <p>
 * 支持自动拦截的约束定义方式：
 * <ul>
 *   <li>{@code @Index(name = "IDX_XXX", columnList = "COL", unique = true)}</li>
 *   <li>{@code @UniqueConstraint(name = "UK_XXX", columnNames = "COL")}</li>
 * </ul>
 * <b>不要使用 {@code @Column(unique = true)}</b>，该方式由数据库自动生成约束名，扫描器无法预测。
 * 如果需要唯一约束被自动拦截，请改用 {@code @UniqueConstraint} 或 {@code @Index(unique = true)}。
 */
@Component("magic_GenericUniqueConstraintViolationHandler")
@Order(JmixOrder.HIGHEST_PRECEDENCE + 50)
public class GenericUniqueConstraintViolationHandler implements UiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GenericUniqueConstraintViolationHandler.class);

    protected static final String MESSAGE_PREFIX = "databaseUniqueConstraintViolation.";
    protected static final String DEFAULT_TEMPLATE_KEY = "org.magic.jmix.addons.core/uniqueConstraintViolation.default";

    /**
     * 默认多数据库唯一约束异常正则，捕获组 1 为约束名。
     * 同时匹配中英文错误信息。
     */
    protected static final Pattern DEFAULT_CONSTRAINT_PATTERN = Pattern.compile(
            "(?:duplicate key value violates unique constraint|重复键违反唯一约束)" +
                    "\\s*\"([^\"]+)\"" +
                    "|Unique index or primary key violation:\\s*\"[^\"]+\\.([^\"]+)\"" +
                    "|Duplicate entry '[^']*' for key '([^']+)'" +
                    "|ORA-00001:.*\\(([^)]+)\\)" +
                    "|(?:Violation of UNIQUE KEY constraint|unique index) '([^']+)'",
            Pattern.CASE_INSENSITIVE
    );

    protected final Messages messages;
    protected final MessageTools messageTools;
    protected final ConstraintMetadataScanner scanner;
    protected final Pattern constraintPattern;

    public GenericUniqueConstraintViolationHandler(Messages messages,
                                                   MessageTools messageTools,
                                                   ConstraintMetadataScanner scanner,
                                                   @Value("${magic.db.uniqueConstraintViolationPattern:}") String customPattern) {
        this.messages = messages;
        this.messageTools = messageTools;
        this.scanner = scanner;
        this.constraintPattern = StringUtils.isNotBlank(customPattern)
                ? Pattern.compile(customPattern, Pattern.CASE_INSENSITIVE)
                : DEFAULT_CONSTRAINT_PATTERN;
    }

    @Override
    public boolean handle(Throwable exception) {
        Throwable throwable = exception;
        while (throwable != null) {
            if (throwable instanceof UniqueConstraintViolationException uve) {
                return doHandle(uve.getConstraintName());
            }
            if (throwable instanceof PersistenceException pe) {
                String constraintName = extractConstraintName(pe.toString());
                if (constraintName != null) {
                    return doHandle(constraintName);
                }
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    protected String extractConstraintName(String exceptionString) {
        Matcher matcher = constraintPattern.matcher(exceptionString);
        if (matcher.find()) {
            // 取第一个非空的捕获组
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (StringUtils.isNotBlank(group)) {
                    return group.toUpperCase();
                }
            }
        }
        return null;
    }

    protected boolean doHandle(String constraintName) {
        // 1. 优先使用用户自定义消息
        String messageKey = MESSAGE_PREFIX + constraintName;
        String customMessage = messages.findMessage(messageKey, null);
        if (customMessage != null) {
            NotificationUtil.error(customMessage);
            return true;
        }

        // 2. 从缓存映射解析
        String resolvedMessage = resolveMessageFromMetadata(constraintName);
        if (resolvedMessage != null) {
            NotificationUtil.error(resolvedMessage);
            return true;
        }

        // 3. 无法解析，交给框架默认 handler
        return false;
    }

    protected String resolveMessageFromMetadata(String constraintName) {
        return scanner.findConstraint(constraintName)
                .map(info -> {
                    String entityCaption = messageTools.getEntityCaption(info.metaClass());
                    List<String> propertyCaptions = new ArrayList<>();
                    for (String propName : info.propertyNames()) {
                        MetaProperty property = info.metaClass().findProperty(propName);
                        if (property != null) {
                            propertyCaptions.add(messageTools.getPropertyCaption(property));
                        } else {
                            propertyCaptions.add(propName);
                        }
                    }
                    String fieldsText = String.join(messages.getMessage("org.magic.jmix.addons.core/fieldConnector"), propertyCaptions);
                    String template = messages.findMessage(DEFAULT_TEMPLATE_KEY, null);
                    if (StringUtils.isNotBlank(template)) {
                        return MessageFormat.format(template, fieldsText, entityCaption);
                    }
                    return null;
                })
                .orElse(null);
    }
}
