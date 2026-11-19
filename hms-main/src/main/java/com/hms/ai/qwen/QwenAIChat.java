package com.hms.ai.qwen;

import com.hms.ai.core.AbstractOpenAiChat;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.common.config.QwenConfig;

/**
 * 通义千问 AI 对话实现（DashScope OpenAI 兼容模式）
 *
 * add by CYQ 2026-09-21 AI 模块重构：归位至 ai.qwen
 * @author 崔永琦
 */
public class QwenAIChat extends AbstractOpenAiChat {

    public QwenAIChat(AIResponseStrategy strategy) {
        super(strategy);
    }

    @Override
    protected String getApiUrl() {
        return QwenConfig.getApiUrl();
    }

    @Override
    protected String getModel() {
        return QwenConfig.getModel();
    }

    @Override
    protected String getApiKey() {
        return QwenConfig.getApiKey();
    }

    @Override
    protected String getProvider() {
        return "qwen";
    }
}
