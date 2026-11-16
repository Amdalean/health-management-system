package com.hms.ai.opencodego;

import com.hms.ai.core.AbstractOpenAiChat;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.common.config.OpenCodeGoConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OpenCode Go 代理 AI 对话实现
 * <p>
 * 基于 OpenAI 兼容协议，额外携带 x-opencode-session 会话标识与可识别的 User-Agent。
 *
 * add by CYQ 2026-09-21 AI 模块重构：从 Qwen 中拆出 OpenCode Go
 */
public class OpenCodeGoAIChat extends AbstractOpenAiChat {

    /** 客户端标识 */
    private static final String USER_AGENT = "hms-health-agent/1.0";

    public OpenCodeGoAIChat(AIResponseStrategy strategy) {
        super(strategy);
    }

    @Override
    protected String getApiUrl() {
        return OpenCodeGoConfig.getApiUrl();
    }

    @Override
    protected String getModel() {
        return OpenCodeGoConfig.getModel();
    }

    @Override
    protected String getApiKey() {
        return OpenCodeGoConfig.getApiKey();
    }

    @Override
    protected String getProvider() {
        return "opencode-go";
    }

    @Override
    protected Map<String, String> extraHeaders() {
        Map<String, String> headers = new HashMap<>();
        // OpenCode Go 要求携带会话ID，否则返回 MissingSessionID
        headers.put("x-opencode-session", UUID.randomUUID().toString());
        headers.put("User-Agent", USER_AGENT);
        return headers;
    }
}
