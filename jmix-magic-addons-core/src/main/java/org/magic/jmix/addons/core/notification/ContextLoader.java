package org.magic.jmix.addons.core.notification;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文加载器。
 * <p>
 * 容器启动时注入 {@link ApplicationContext}，使 {@link NotificationUtil}
 * 的静态方法能访问 Spring Bean。靠 {@code MagicCoreConfiguration} 的 {@code @ComponentScan} 自动注册
 * （本类位于 {@code org.magic.jmix.addons.core} 的子包，在扫描范围内）。
 * <p>
 * 与 {@link NotificationUtil} 同包，故可访问其包级私有的
 * {@code setApplicationContext}，无需将内部注入方法暴露为 public。
 */
@Component
public class ContextLoader implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        NotificationUtil.setApplicationContext(applicationContext);
    }
}
