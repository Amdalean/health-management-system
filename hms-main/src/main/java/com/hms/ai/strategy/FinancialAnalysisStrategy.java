package com.hms.ai.strategy;

import java.util.Map;

/**
 * 财务分析响应策略实现
 */
public class FinancialAnalysisStrategy implements AIResponseStrategy {

    @Override
    public String processResponse(String input, Map<String, Object> context) {
        // 为财务分析定制的系统提示词
        String systemPrompt = "你是财务分析专家，擅长分析财务数据和提供投资建议。" +
                "请对用户提供的财务信息进行专业分析，包括收益、风险、趋势等方面。";
        
        StringBuilder response = new StringBuilder();
        response.append(systemPrompt).append("\n");
        response.append("用户输入: ").append(input).append("\n");
        
        // 财务分析特有的处理逻辑
        response.append("根据您提供的财务信息，以下是分析结果：\n");
        response.append("- 财务状况: \n");
        response.append("- 风险评估: \n");
        response.append("- 趋势预测: \n");
        
        return response.toString();
    }

    @Override
    public String getStrategyName() {
        return "你是财务分析专家，擅长分析财务数据和提供投资建议。请对用户提供的财务信息进行专业分析，包括收益、风险、趋势等方面。";
    }
}