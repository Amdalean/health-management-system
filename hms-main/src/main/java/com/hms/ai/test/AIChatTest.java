package com.hms.ai.test;

import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.template.AbstractAIChatTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AI对话系统测试类
 */
public class AIChatTest {
    public static void main(String[] args) {
        // 创建上下文信息
        Map<String, Object> context = new HashMap<>();
        context.put("userId", "123");
        context.put("sessionId", "abc");
        
        // 测试健康建议策略
        System.out.println("=== 测试健康建议策略 ===");
        AbstractAIChatTemplate healthChat = AIChatFactory.createAIChat(AIChatFactory.StrategyType.HEALTH_ADVICE);
        String healthResponse = healthChat.chat("如何保持健康饮食？", context);
        System.out.println("健康建议回复: " + healthResponse);
        System.out.println();
        
        // 测试报表输出策略
        System.out.println("=== 测试报表输出策略 ===");
        AbstractAIChatTemplate reportChat = AIChatFactory.createAIChat(AIChatFactory.StrategyType.REPORT_OUTPUT);
        String reportResponse = reportChat.chat("生成本月健康数据报告", context);
        System.out.println("报表输出回复: " + reportResponse);
        System.out.println();
        
        // 测试财务分析策略
        System.out.println("=== 测试财务分析策略 ===");
        AbstractAIChatTemplate financeChat = AIChatFactory.createAIChat(AIChatFactory.StrategyType.FINANCIAL_ANALYSIS);
        String financeResponse = financeChat.chat("分析健康管理系统本月收入情况", context);
        System.out.println("财务分析回复: " + financeResponse);
    }
}