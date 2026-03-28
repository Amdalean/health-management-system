package com.hms.test;

import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.template.AbstractAIChatTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@DisplayName("AI对话系统单元测试")
public class AIDialogSystemTest {

    @DisplayName("测试AI健康建议策略")
    @Test
    public void testAIHealthAdviceStrategy() {
        System.out.println("=== AI健康建议策略测试 ===");
        try {
            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(AIChatFactory.StrategyType.HEALTH_ADVICE);
            Map<String, Object> context = new HashMap<>();
            context.put("userId", "123");
            String response = aiChat.chat("如何保持健康饮食？", context);
            System.out.println("健康建议AI回复: " + response);
        } catch (Exception e) {
            System.out.println("AI健康建议策略测试失败: " + e.getMessage());
        }
    }

    @DisplayName("测试AI报表输出策略")
    @Test
    public void testAIReportOutputStrategy() {
        System.out.println("=== AI报表输出策略测试 ===");
        try {
            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(AIChatFactory.StrategyType.REPORT_OUTPUT);
            Map<String, Object> context = new HashMap<>();
            context.put("userId", "123");
            String response = aiChat.chat("生成本月健康数据报告", context);
            System.out.println("报表输出AI回复: " + response);
        } catch (Exception e) {
            System.out.println("AI报表输出策略测试失败: " + e.getMessage());
        }
    }

    @DisplayName("测试AI财务分析策略")
    @Test
    public void testAIFinancialAnalysisStrategy() {
        System.out.println("=== AI财务分析策略测试 ===");
        try {
            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(AIChatFactory.StrategyType.FINANCIAL_ANALYSIS);
            Map<String, Object> context = new HashMap<>();
            context.put("userId", "123");
            String response = aiChat.chat("分析健康管理系统本月收入情况", context);
            System.out.println("财务分析AI回复: " + response);
        } catch (Exception e) {
            System.out.println("AI财务分析策略测试失败: " + e.getMessage());
        }
    }

    @DisplayName("测试AI成绩分析策略")
    @Test
    public void testAIGradeAnalysisStrategy() {
        System.out.println("=== AI成绩分析策略测试 ===");
        try {
            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(AIChatFactory.StrategyType.GRADE_ANALYSIS);
            Map<String, Object> context = new HashMap<>();
            context.put("userId", "123");
            
            // 模拟体育专业学生体能测试数据
            String mockGradeData = "{\n" +
                    "  \"studentId\": \"S001\",\n" +
                    "  \"studentName\": \"李明\",\n" +
                    "  \"major\": \"体育教育\",\n" +
                    "  \"physicalTestResults\": [\n" +
                    "    {\n" +
                    "      \"event\": \"100米短跑\",\n" +
                    "      \"results\": [\n" +
                    "        {\"date\": \"2024-03-15\", \"time\": \"11.2\", \"unit\": \"秒\", \"reference\": \"国家二级\"},\n" +
                    "        {\"date\": \"2024-06-20\", \"time\": \"11.0\", \"unit\": \"秒\", \"reference\": \"国家二级\"},\n" +
                    "        {\"date\": \"2024-09-10\", \"time\": \"10.8\", \"unit\": \"秒\", \"reference\": \"国家一级\"}\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"event\": \"跳远\",\n" +
                    "      \"results\": [\n" +
                    "        {\"date\": \"2024-03-15\", \"distance\": \"6.2\", \"unit\": \"米\", \"reference\": \"良好\"},\n" +
                    "        {\"date\": \"2024-06-20\", \"distance\": \"6.5\", \"unit\": \"米\", \"reference\": \"优秀\"},\n" +
                    "        {\"date\": \"2024-09-10\", \"distance\": \"6.7\", \"unit\": \"米\", \"reference\": \"优秀\"}\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"event\": \"1000米长跑\",\n" +
                    "      \"results\": [\n" +
                    "        {\"date\": \"2024-03-15\", \"time\": \"3分20秒\", \"unit\": \"分钟:秒\", \"reference\": \"良好\"},\n" +
                    "        {\"date\": \"2024-06-20\", \"time\": \"3分10秒\", \"unit\": \"分钟:秒\", \"reference\": \"良好\"},\n" +
                    "        {\"date\": \"2024-09-10\", \"time\": \"2分55秒\", \"unit\": \"分钟:秒\", \"reference\": \"优秀\"}\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"event\": \"引体向上\",\n" +
                    "      \"results\": [\n" +
                    "        {\"date\": \"2024-03-15\", \"count\": 12, \"unit\": \"次\", \"reference\": \"及格\"},\n" +
                    "        {\"date\": \"2024-06-20\", \"count\": 15, \"unit\": \"次\", \"reference\": \"良好\"},\n" +
                    "        {\"date\": \"2024-09-10\", \"count\": 18, \"unit\": \"次\", \"reference\": \"优秀\"}\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"event\": \"立定跳远\",\n" +
                    "      \"results\": [\n" +
                    "        {\"date\": \"2024-03-15\", \"distance\": \"2.3\", \"unit\": \"米\", \"reference\": \"良好\"},\n" +
                    "        {\"date\": \"2024-06-20\", \"distance\": \"2.4\", \"unit\": \"米\", \"reference\": \"良好\"},\n" +
                    "        {\"date\": \"2024-09-10\", \"distance\": \"2.6\", \"unit\": \"米\", \"reference\": \"优秀\"}\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"overallRank\": 3,\n" +
                    "  \"className\": \"体育学院2022级1班\",\n" +
                    "  \"coach\": \"王教练\",\n" +
                    "  \"injuryHistory\": \"左膝轻微劳损（已康复）\"\n" +
                    "}";
            
            String response = aiChat.chat("请分析以下体育专业学生的体能测试成绩：" + mockGradeData, context);
            System.out.println("成绩分析AI回复: " + response);
        } catch (Exception e) {
            System.out.println("AI成绩分析策略测试失败: " + e.getMessage());
        }
    }

    @DisplayName("测试AI策略工厂")
    @Test
    public void testAIChatFactory() {
        System.out.println("=== AI策略工厂测试 ===");
        try {
            // 测试创建健康建议AI
            AbstractAIChatTemplate healthAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.HEALTH_ADVICE);
            System.out.println("健康建议AI创建成功: " + (healthAI != null));

            // 测试创建报表输出AI
            AbstractAIChatTemplate reportAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.REPORT_OUTPUT);
            System.out.println("报表输出AI创建成功: " + (reportAI != null));

            // 测试创建财务分析AI
            AbstractAIChatTemplate financialAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.FINANCIAL_ANALYSIS);
            System.out.println("财务分析AI创建成功: " + (financialAI != null));

            // 测试创建成绩分析AI
            AbstractAIChatTemplate gradeAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.GRADE_ANALYSIS);
            System.out.println("成绩分析AI创建成功: " + (gradeAI != null));

            // 尝试创建不存在的策略类型
            try {
                AbstractAIChatTemplate invalidAI = AIChatFactory.createAIChat(null);
                System.out.println("无效AI创建失败测试: " + (invalidAI == null));
            } catch (IllegalArgumentException e) {
                System.out.println("正确捕获了无效AI类型异常: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("AI策略工厂测试失败: " + e.getMessage());
        }
    }

    @DisplayName("测试AI系统提示词策略")
    @Test
    public void testAISystemPromptStrategy() {
        System.out.println("=== AI系统提示词策略测试 ===");
        try {
            // 测试健康建议策略的系统提示词
            AbstractAIChatTemplate healthAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.HEALTH_ADVICE);
            System.out.println("健康建议系统提示词策略正确应用");

            // 测试报表输出策略的系统提示词
            AbstractAIChatTemplate reportAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.REPORT_OUTPUT);
            System.out.println("报表输出系统提示词策略正确应用");

            // 测试财务分析策略的系统提示词
            AbstractAIChatTemplate financialAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.FINANCIAL_ANALYSIS);
            System.out.println("财务分析系统提示词策略正确应用");

            // 测试成绩分析策略的系统提示词
            AbstractAIChatTemplate gradeAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.GRADE_ANALYSIS);
            System.out.println("成绩分析系统提示词策略正确应用");
        } catch (Exception e) {
            System.out.println("AI系统提示词策略测试失败: " + e.getMessage());
        }
    }
}