package com.hms.ai.factory;

import com.hms.ai.ollama.OllamaAIChat;
import com.hms.ai.opencodego.OpenCodeGoAIChat;
import com.hms.ai.qwen.QwenAIChat;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.ai.strategy.BaduanjinScoringStrategy;
import com.hms.ai.strategy.CommonStrategy;
import com.hms.ai.strategy.GradeAnalysisStrategy;
import com.hms.ai.template.AbstractAIChatTemplate;

/**
 * AI 对话工厂类 —— 使用工厂模式，按"服务商 + 业务场景"两个维度创建对话实例。
 *
 * <p>模板方法固定调用骨架（输入预处理 → 上下文注入 → 模型调用 → 响应后处理），
 * 策略接口承载各场景的提示词与输出约束，工厂负责组装。</p>
 *
 * <p>新增一个评分场景只需实现一个 {@link AIResponseStrategy} 并在 {@link StrategyType}
 * 中登记，无需重复编写模型调用代码。</p>
 *
 * @author 崔永琦
 * @date 2026-03-10
 */
public class AIChatFactory {

    /**
     * AI 服务商类型
     */
    public enum ServiceType {
        /** 通义千问 DashScope 兼容接口 */
        QWEN,
        /** OpenAI 兼容代理 */
        OPENCODE_GO,
        /** 本地 Ollama 部署 */
        OLLAMA
    }

    /**
     * 业务场景（策略）类型
     */
    public enum StrategyType {
        /** 成绩智能分析：实践类课程动作逐式评分 */
        BADUANJIN_SCORING,
        /** 成绩智能分析：一般课程成绩数据分析 */
        GRADE_ANALYSIS,
        /** 通用对话 */
        COMMON
    }

    private AIChatFactory() {
    }

    /**
     * 根据策略类型创建 AI 对话实例（默认服务商）
     *
     * @param strategyType 业务场景
     * @return AI 对话实例
     */
    public static AbstractAIChatTemplate createAIChat(StrategyType strategyType) {
        return createAIChat(ServiceType.QWEN, strategyType);
    }

    /**
     * 根据服务商与业务场景创建 AI 对话实例
     *
     * @param serviceType  服务商（QWEN / OPENCODE_GO / OLLAMA）
     * @param strategyType 业务场景
     * @return AI 对话实例
     */
    public static AbstractAIChatTemplate createAIChat(ServiceType serviceType, StrategyType strategyType) {
        if (serviceType == null) {
            throw new IllegalArgumentException("不支持的服务类型: null");
        }
        AIResponseStrategy strategy = createStrategy(strategyType);
        switch (serviceType) {
            case QWEN:
                return new QwenAIChat(strategy);
            case OPENCODE_GO:
                return new OpenCodeGoAIChat(strategy);
            case OLLAMA:
                return new OllamaAIChat(strategy);
            default:
                throw new IllegalArgumentException("不支持的服务类型: " + serviceType);
        }
    }

    /**
     * 根据策略类型创建业务策略实例
     */
    private static AIResponseStrategy createStrategy(StrategyType strategyType) {
        if (strategyType == null) {
            throw new IllegalArgumentException("不支持的策略类型: null");
        }
        switch (strategyType) {
            case BADUANJIN_SCORING:
                return new BaduanjinScoringStrategy();
            case GRADE_ANALYSIS:
                return new GradeAnalysisStrategy();
            case COMMON:
                return new CommonStrategy();
            default:
                throw new IllegalArgumentException("不支持的策略类型: " + strategyType);
        }
    }
}
