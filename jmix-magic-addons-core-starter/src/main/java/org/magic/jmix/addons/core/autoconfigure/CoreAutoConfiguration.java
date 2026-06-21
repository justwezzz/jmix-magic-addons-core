package org.magic.jmix.addons.core.autoconfigure;

import org.magic.jmix.addons.core.MagicCoreConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Jmix Magic Core Addon 自动配置类。
 */
@AutoConfiguration
@Import({MagicCoreConfiguration.class})
public class CoreAutoConfiguration {
}
