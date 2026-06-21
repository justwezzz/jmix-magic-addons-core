package org.magic.jmix.addons.core.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

/**
 * 应用配置实体，简单的键值对存储
 */
@JmixEntity
@Entity(name = "magic_AppConfig")
@Table(name = "MAGIC_APP_CONFIG", indexes = {
        @Index(name = "IDX_APP_CONFIG_ON_KEY", columnList = "CONFIG_KEY", unique = true)
})
public class AppConfig extends BaseEntity {

    @InstanceName
    @Column(name = "CONFIG_KEY", nullable = false, length = 32)
    private String configKey;

    @Column(name = "CONFIG_VALUE", length = 256)
    private String configValue;

    @Column(name = "DESCRIPTION", length = 256)
    private String description;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
