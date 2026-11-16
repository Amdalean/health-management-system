package com.hms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取 OpenCode Go 代理相关配置
 *
 * add by CYQ 2026-09-21 AI 模块重构：OpenCode Go 独立配置
 */
@Component
@ConfigurationProperties(prefix = "ai.opencode-go")
public class OpenCodeGoConfig {
    private static String apiUrl;
    private static String model;
    private static String apiKey;

    public static String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        OpenCodeGoConfig.apiUrl = apiUrl;
    }

    public static String getModel() {
        return model;
    }

    public void setModel(String model) {
        OpenCodeGoConfig.model = model;
    }

    public static String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        OpenCodeGoConfig.apiKey = apiKey;
    }
}
