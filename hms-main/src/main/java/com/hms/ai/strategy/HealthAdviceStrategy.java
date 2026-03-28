package com.hms.ai.strategy;

import java.util.Map;

/**
 * 健康建议响应策略实现
 */
public class HealthAdviceStrategy implements AIResponseStrategy {

    @Override
    public String processResponse(String input, Map<String, Object> context) {
        // 为健康建议定制的系统提示词
        String systemPrompt = "你是健康专家，专注于提供科学、准确的健康建议。" +
                "请基于循证医学原则，为用户提供的健康相关问题提供专业建议。";
        
        StringBuilder response = new StringBuilder();
        response.append(systemPrompt).append("\n");
        response.append("用户输入: ").append(input).append("\n");
        
        // 健康建议特有的处理逻辑
        response.append("基于您的健康咨询，以下是专业建议：\n");
        
        return response.toString();
    }

    @Override
    public String getStrategyName() {
        return "你是健康专家，专注于提供科学、准确的健康建议。请基于循证医学原则，为用户提供的健康相关问题提供专业建议。";
    }
}