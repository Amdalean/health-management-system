package com.hms.test;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.ai.strategy.BaduanjinScoringStrategy;
import com.hms.ai.template.AbstractAIChatTemplate;
import com.hms.ai.util.AiJsonCleaner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 八段锦打分模块单元测试
 * <p>
 * 只校验提示词构建、工厂装配与输出清洗，**不发起网络请求、不依赖 Spring 上下文与数据库**，
 * 因此整个类可在毫秒级完成，并允许并行执行（见 junit-platform.properties）。
 *
 * @author 崔永琦 2026-11-16
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("八段锦打分单元测试")
public class BaduanjinScoringTest {

    /** 八段锦九式（含预备势与收势），与提示词内标准动作知识库的条目一致 */
    private static final String[] BADUANJIN_MOVEMENTS = {
            "预备势", "双手托天理三焦", "左右开弓似射雕", "调理脾胃须单举", "五劳七伤往后瞧",
            "摇头摆尾去心火", "双手攀足固肾腰", "攒拳怒目增气力", "背后七颠百病消", "收势"
    };

    /** 直接调用策略构建评分提示词，不发起网络请求 */
    private static String buildBaduanjinPrompt() {
        AIResponseStrategy strategy = new BaduanjinScoringStrategy();
        String input = "学生八段锦练习视频：/profile/upload/2026/11/15/demo.mp4";
        return strategy.processResponse(input, new HashMap<String, Object>());
    }

    @DisplayName("测试八段锦评分维度与分值")
    @Test
    public void testBaduanjinScoringDimensions() {
        System.out.println("=== 八段锦评分维度测试 ===");
        String prompt = buildBaduanjinPrompt();

        assertNotNull(prompt, "八段锦评分提示词不应为空");
        // 四个评分维度合计 100 分
        assertTrue(prompt.contains("动作准确度 (40 分)"), "缺少「动作准确度」维度");
        assertTrue(prompt.contains("身法与姿态 (30 分)"), "缺少「身法与姿态」维度");
        assertTrue(prompt.contains("连贯性与节奏 (20 分)"), "缺少「连贯性与节奏」维度");
        assertTrue(prompt.contains("精气神 (10 分)"), "缺少「精气神」维度");

        System.out.println("四个评分维度校验通过，总分 100");
    }

    @DisplayName("测试八段锦动作要领覆盖")
    @Test
    public void testBaduanjinScoringMovements() {
        System.out.println("=== 八段锦动作要领覆盖测试 ===");
        String prompt = buildBaduanjinPrompt();

        for (String movement : BADUANJIN_MOVEMENTS) {
            assertTrue(prompt.contains(movement), "提示词缺少动作要领：" + movement);
        }
        assertTrue(prompt.contains("common_mistakes"), "提示词缺少常见错误说明");

        System.out.println("预备势 + 八式 + 收势共 " + BADUANJIN_MOVEMENTS.length + " 个部分均覆盖");
    }

    @DisplayName("测试八段锦输出格式约束")
    @Test
    public void testBaduanjinScoringOutputSchema() {
        System.out.println("=== 八段锦输出格式约束测试 ===");
        String prompt = buildBaduanjinPrompt();

        // 与 ScoreAnalysisTask 解析时读取的字段一一对应
        String[] fields = {"overall_score", "grade_level", "movement_analysis",
                "move_name", "score", "feedback_keep", "feedback_improve",
                "top_3_issues", "coach_message"};
        for (String field : fields) {
            assertTrue(prompt.contains("\"" + field + "\""), "输出 Schema 缺少字段：" + field);
        }
        assertTrue(prompt.contains("严禁"), "提示词缺少「严禁」类约束");
        assertTrue(prompt.contains("Markdown"), "提示词缺少禁止 Markdown 包裹的约束");

        System.out.println("输出 Schema 与解析字段一致，Markdown 约束已声明");
    }

    @DisplayName("测试八段锦策略工厂创建")
    @Test
    public void testBaduanjinScoringFactory() {
        System.out.println("=== 八段锦策略工厂测试 ===");

        Object[][] cases = {
                {AIChatFactory.ServiceType.QWEN, "QwenAIChat"},
                {AIChatFactory.ServiceType.OPENCODE_GO, "OpenCodeGoAIChat"},
                {AIChatFactory.ServiceType.OLLAMA, "OllamaAIChat"}
        };
        for (Object[] c : cases) {
            AbstractAIChatTemplate ai = AIChatFactory.createAIChat(
                    (AIChatFactory.ServiceType) c[0], AIChatFactory.StrategyType.BADUANJIN_SCORING);
            assertNotNull(ai, "创建失败：" + c[1]);
            assertEquals(c[1], ai.getClass().getSimpleName(), "服务商实现类不匹配");
        }

        AbstractAIChatTemplate defaultAI = AIChatFactory.createAIChat(AIChatFactory.StrategyType.BADUANJIN_SCORING);
        assertEquals("QwenAIChat", defaultAI.getClass().getSimpleName(), "默认服务商应为 Qwen");

        System.out.println("Qwen / OpenCodeGo / Ollama 三种服务商创建均通过");
    }

    @DisplayName("测试八段锦响应清洗与解析")
    @Test
    public void testBaduanjinResponseCleanAndParse() {
        System.out.println("=== 八段锦响应清洗与解析测试 ===");

        // 模拟大模型返回被 ```json 包裹、且包含 10 个动作的响应
        StringBuilder sb = new StringBuilder("```json\n{");
        sb.append("\"overall_score\":86,");
        sb.append("\"grade_level\":\"良好\",");
        sb.append("\"movement_analysis\":[");
        for (int i = 0; i < BADUANJIN_MOVEMENTS.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"move_name\":\"").append(BADUANJIN_MOVEMENTS[i]).append("\"")
                    .append(",\"score\":").append(9 - (i % 3))
                    .append(",\"feedback_keep\":\"动作舒展\"")
                    .append(",\"feedback_improve\":\"马步略高，大腿未到水平\"}");
        }
        sb.append("],");
        sb.append("\"top_3_issues\":[\"第二式马步深度不足\",\"第四式转头幅度偏小\",\"第六式屈膝\"],");
        sb.append("\"coach_message\":\"整体动作连贯，继续保持\"}");
        sb.append("\n```");

        String cleaned = AiJsonCleaner.clean(sb.toString());
        assertFalse(cleaned.contains("```"), "清洗后不应残留 Markdown 标记");

        JSONObject result = JSONObject.parseObject(cleaned);
        assertNotNull(result, "清洗后的文本应能被解析为 JSON");
        assertEquals(86, result.getIntValue("overall_score"));
        assertEquals("良好", result.getString("grade_level"));

        JSONArray analysis = result.getJSONArray("movement_analysis");
        assertEquals(BADUANJIN_MOVEMENTS.length, analysis.size(), "逐式分析应包含全部动作");
        assertEquals("预备势", analysis.getJSONObject(0).getString("move_name"));
        assertEquals(3, result.getJSONArray("top_3_issues").size());
        assertNotNull(result.getString("coach_message"));

        // 幂等：已清洗的文本再清洗一次结果不变；null 原样返回
        assertEquals(cleaned, AiJsonCleaner.clean(cleaned));
        assertNull(AiJsonCleaner.clean(null));

        System.out.println("Markdown 包裹剥离、JSON 解析与幂等性均通过");
    }
}
