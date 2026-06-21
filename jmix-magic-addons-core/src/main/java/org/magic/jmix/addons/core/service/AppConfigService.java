package org.magic.jmix.addons.core.service;

import io.jmix.core.DataManager;
import io.jmix.core.querycondition.PropertyCondition;
import org.magic.jmix.addons.core.entity.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * 应用配置服务类，提供配置的读写操作
 */
@Service
public class AppConfigService {

    protected final DataManager dataManager;

    @Autowired
    public AppConfigService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值，不存在则返回 null
     */
    @Nullable
    public String getValue(String key) {
        return getValue(key, null);
    }

    /**
     * 获取配置值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值，不存在则返回默认值
     */
    public String getValue(String key, String defaultValue) {
        Optional<AppConfig> config = dataManager.load(AppConfig.class)
                .condition(PropertyCondition.equal("configKey", key))
                .optional();

        return config.map(AppConfig::getConfigValue).orElse(defaultValue);
    }

    /**
     * 设置配置值，不存在则创建
     *
     * @param key   配置键
     * @param value 配置值
     */
    public void setValue(String key, String value) {
        setValue(key, value, null);
    }

    /**
     * 设置配置值，不存在则创建
     *
     * @param key         配置键
     * @param value       配置值
     * @param description 描述
     */
    public void setValue(String key, String value, @Nullable String description) {
        Optional<AppConfig> existing = dataManager.load(AppConfig.class)
                .condition(PropertyCondition.equal("configKey", key))
                .optional();

        AppConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setConfigValue(value);
            if (description != null) {
                config.setDescription(description);
            }
        } else {
            config = dataManager.create(AppConfig.class);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
        }

        dataManager.save(config);
    }

    /**
     * 删除配置
     *
     * @param key 配置键
     */
    public void delete(String key) {
        dataManager.load(AppConfig.class)
                .condition(PropertyCondition.equal("configKey", key))
                .optional()
                .ifPresent(dataManager::remove);
    }

    /**
     * 检查配置是否存在
     *
     * @param key 配置键
     * @return 是否存在
     */
    public boolean exists(String key) {
        return dataManager.load(AppConfig.class)
                .condition(PropertyCondition.equal("configKey", key))
                .optional()
                .isPresent();
    }
}
