package org.magic.jmix.addons.core.exception;

import io.jmix.core.JmixOrder;
import io.jmix.core.Messages;
import io.jmix.flowui.exception.UiExceptionHandler;
import jakarta.persistence.PersistenceException;
import org.apache.commons.lang3.StringUtils;
import org.magic.jmix.addons.core.notification.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("magic_GenericForeignKeyViolationHandler")
@Order(JmixOrder.HIGHEST_PRECEDENCE + 60)
public class GenericForeignKeyViolationHandler implements UiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GenericForeignKeyViolationHandler.class);

    protected static final String MESSAGE_PREFIX = "databaseForeignKeyViolation.";
    protected static final String DEFAULT_MESSAGE_KEY = MESSAGE_PREFIX + "default";

    protected static final Pattern DEFAULT_FK_PATTERN = Pattern.compile(
            "外键约束\\s+\"([^\"]+)\"|foreign key constraint\\s+\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    protected final Messages messages;
    protected final Pattern fkPattern;

    public GenericForeignKeyViolationHandler(Messages messages,
                                             @Value("${magic.db.foreignKeyViolationPattern:}") String customPattern) {
        this.messages = messages;
        this.fkPattern = StringUtils.isNotBlank(customPattern)
                ? Pattern.compile(customPattern, Pattern.CASE_INSENSITIVE)
                : DEFAULT_FK_PATTERN;
    }

    @Override
    public boolean handle(Throwable exception) {
        Throwable throwable = exception;
        while (throwable != null) {
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
        Matcher matcher = fkPattern.matcher(exceptionString);
        if (matcher.find()) {
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
        // 1. 具体约束的自定义提示
        String messageKey = MESSAGE_PREFIX + constraintName;
        String customMessage = messages.findMessage(messageKey, null);
        if (customMessage != null) {
            NotificationUtil.error(customMessage);
            return true;
        }

        // 2. 兜底通用提示
        String defaultMessage = messages.findMessage(DEFAULT_MESSAGE_KEY, null);
        if (defaultMessage != null) {
            NotificationUtil.error(defaultMessage);
            return true;
        }

        // 3. 都没找到，交给框架默认处理
        return false;
    }
}
