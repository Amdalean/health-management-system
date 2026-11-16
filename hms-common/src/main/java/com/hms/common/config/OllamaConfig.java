package com.hms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取本地 Ollama 服务相关配置
 *
 * add by CYQ 2026-09-21 AI 模块重构：Ollama 独立配置
 */
@Component
@ConfigurationProperties(prefix = "ai.ollama")
public class OllamaConfig {
    private static String host;
    private static String model;

    public static String getHost() {
        return host;
    }

    public void setHost(String host) {
        OllamaConfig.host = host;
    }

    public static String getModel() {
        return model;
    }

    public void setModel(String model) {
        OllamaConfig.model = model;
    }
}
