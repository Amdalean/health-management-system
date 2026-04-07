package com.hms.ai.factory;

import com.hms.ai.strategy.*;
import com.hms.ai.template.*;

/**
 * AI对话工厂类 - 使用工厂模式创建不同类型的AI对话实例
 * 
 * add by CYQ 2026年4月6日 - 增加 Ollama 服务支持
 */
public class AIChatFactory {

    /**
     * AI 服务类型
     */
    public enum ServiceType {
        QWEN,    // 通义千问
        OLLAMA   // 本地 Ollama
    }

    public enum StrategyType {
        HEALTH_ADVICE,
        REPORT_OUTPUT,
        FINANCIAL_ANALYSIS,
        GRADE_ANALYSIS,
        BADUANJIN_SCORING,
        COMMON,
        DragonRaja,
        Reminder
    }

    /**
     * 根据策略类型创建 Qwen AI对话实例（默认）
     *
     * @param strategyType 策略类型（对应不同场景）
     * @return AI对话实例
     */
    public static AbstractAIChatTemplate createAIChat(StrategyType strategyType) {
        return createAIChat(ServiceType.QWEN, strategyType);
    }

    /**
     * 根据服务类型和策略类型创建AI对话实例
     *
     * @param serviceType 服务类型（QWEN 或 OLLAMA）
     * @param strategyType 策略类型（对应不同场景）
     * @return AI对话实例
     */
    public static AbstractAIChatTemplate createAIChat(ServiceType serviceType, StrategyType strategyType) {
        switch (serviceType) {
            case QWEN:
                return createQwenChat(strategyType);
            case OLLAMA:
                return createOllamaChat(strategyType);
            default:
                throw new IllegalArgumentException("不支持的服务类型: " + serviceType);
        }
    }

    /**
     * 创建 Qwen AI对话实例
     */
    private static AbstractAIChatTemplate createQwenChat(StrategyType strategyType) {
        switch (strategyType) {
            case HEALTH_ADVICE:
                return new QwenAIChat(new HealthAdviceStrategy());
            case REPORT_OUTPUT:
                return new QwenAIChat(new ReportOutputStrategy());
            case FINANCIAL_ANALYSIS:
                return new QwenAIChat(new FinancialAnalysisStrategy());
            case GRADE_ANALYSIS:
                return new QwenAIChat(new GradeAnalysisStrategy());
            case BADUANJIN_SCORING:
                return new QwenAIChat(new BaduanjinScoringStrategy());
            case COMMON:
                return new QwenAIChat(new CommonStrategy());
            case DragonRaja:
                return new QwenAIChat(new DragonRajaStrategy());
            case Reminder:
                return new QwenAIChat(new ReminderStrategy());
            default:
                throw new IllegalArgumentException("不支持的策略类型: " + strategyType);
        }
    }

    /**
     * 创建 Ollama AI对话实例
     * 
     * add by CYQ 2026年4月6日
     */
    private static AbstractAIChatTemplate createOllamaChat(StrategyType strategyType) {
        switch (strategyType) {
            case HEALTH_ADVICE:
                return new OllamaAIChat(new HealthAdviceStrategy());
            case REPORT_OUTPUT:
                return new OllamaAIChat(new ReportOutputStrategy());
            case FINANCIAL_ANALYSIS:
                return new OllamaAIChat(new FinancialAnalysisStrategy());
            case GRADE_ANALYSIS:
                return new OllamaAIChat(new GradeAnalysisStrategy());
            case BADUANJIN_SCORING:
                return new OllamaAIChat(new BaduanjinScoringStrategy());
            case COMMON:
                return new OllamaAIChat(new CommonStrategy());
            case DragonRaja:
                return new OllamaAIChat(new DragonRajaStrategy());
            case Reminder:
                return new OllamaAIChat(new ReminderStrategy());
            default:
                throw new IllegalArgumentException("不支持的策略类型: " + strategyType);
        }
    }
}