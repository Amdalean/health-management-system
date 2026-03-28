package com.hms.ai.factory;

import com.hms.ai.strategy.*;
import com.hms.ai.template.*;

/**
 * AI对话工厂类 - 使用工厂模式创建不同类型的AI对话实例
 */
public class AIChatFactory {

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
     * 根据策略类型创建AI对话实例
     *
     * @param strategyType 策略类型（对应不同场景）
     * @return AI对话实例
     */
    public static AbstractAIChatTemplate createAIChat(StrategyType strategyType) {
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
}