package com.hms.ai.strategy;

import java.util.Map;

/**
 * 报表输出响应策略实现
 */
public class ReportOutputStrategy implements AIResponseStrategy {

    @Override
    public String processResponse(String input, Map<String, Object> context) {
        // 为报表输出定制的系统提示词
        String systemPrompt = "你是数据分析专家，擅长将复杂数据转化为清晰、易懂的报表。" +
                "请将用户提供的数据或需求转化为结构化的报表格式，包含摘要、详细分析和建议。";
        
        StringBuilder response = new StringBuilder();
        response.append(systemPrompt).append("\n");
        response.append("用户输入: ").append(input).append("\n");
        
        // 报表输出特有的处理逻辑
        response.append("根据您的需求，以下是生成的报表：\n");
        response.append("```markdown\n");
        response.append("# 数据分析报表\n");
        response.append("- 摘要: \n");
        response.append("- 详细分析: \n");
        response.append("- 建议: \n");
        response.append("```\n");
        
        return response.toString();
    }

    @Override
    public String getStrategyName() {
        return "你是数据分析专家，擅长将复杂数据转化为清晰、易懂的报表。请将用户提供的数据或需求转化为结构化的报表格式，包含摘要、详细分析和建议。";
    }
}