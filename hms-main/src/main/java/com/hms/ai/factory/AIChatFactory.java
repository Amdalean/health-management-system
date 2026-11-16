package com.hms.ai.factory;

import com.hms.ai.ollama.OllamaAIChat;
import com.hms.ai.opencodego.OpenCodeGoAIChat;
import com.hms.ai.qwen.QwenAIChat;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.ai.strategy.BaduanjinScoringStrategy;
import com.hms.ai.strategy.CommonStrategy;
import com.hms.ai.strategy.DragonRajaStrategy;
import com.hms.ai.strategy.DragonRajaWeatherStrategy;
import com.hms.ai.strategy.FinancialAnalysisStrategy;
import com.hms.ai.strategy.GradeAnalysisStrategy;
import com.hms.ai.strategy.HealthAdviceStrategy;
import com.hms.ai.strategy.JotdownRecognitionStrategy;
import com.hms.ai.strategy.ReminderStrategy;
import com.hms.ai.strategy.ReportOutputStrategy;
import com.hms.ai.strategy.ShoppingIntentStrategy;
import com.hms.ai.strategy.ShoppingPickStrategy;
import com.hms.ai.strategy.WeatherSkyStrategy;
import com.hms.ai.template.AbstractAIChatTemplate;

/**
 * AI对话工厂类 - 使用工厂模式创建不同类型的AI对话实例
 *
 * add by CYQ 2026年4月6日 - 增加 Ollama 服务支持
 * update by CYQ 2026-09-21 AI 模块重构：服务商按包拆分（qwen / opencodego / ollama）
 */
public class AIChatFactory {

    /**
     * AI 服务类型（服务商）
     */
    public enum ServiceType {
        QWEN,         // 通义千问 DashScope
        OPENCODE_GO,  // OpenCode Go 代理
        OLLAMA        // 本地 Ollama
    }

    public enum StrategyType {
        HEALTH_ADVICE,
        REPORT_OUTPUT,
        FINANCIAL_ANALYSIS,
        GRADE_ANALYSIS,
        BADUANJIN_SCORING,
        COMMON,
        DragonRaja,
        WEATHER_SKY,
        // add by CYQ 2026-11-09 天气播报龙族角色每日随机（楚子航/凯撒/路明非/芬格尔/诺诺/诺玛/绘梨衣/源稚生/昂热）
        DRAGON_RAJA_WEATHER,
        Reminder,
        JOTDOWN_RECOGNITION,
        // add by CYQ 2026-11-04 优惠查询-AI 选品：需求解析 / 性价比打分
        SHOPPING_INTENT,
        SHOPPING_PICK
    }

    /**
     * 根据策略类型创建 AI对话实例（默认 Qwen）
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
     * @param serviceType  服务商（QWEN / OPENCODE_GO / OLLAMA）
     * @param strategyType 策略类型（对应不同场景）
     * @return AI对话实例
     */
    public static AbstractAIChatTemplate createAIChat(ServiceType serviceType, StrategyType strategyType) {
        if (serviceType == null) {
            throw new IllegalArgumentException("不支持的服务类型: " + serviceType);
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
            throw new IllegalArgumentException("不支持的策略类型: " + strategyType);
        }
        switch (strategyType) {
            case HEALTH_ADVICE:
                return new HealthAdviceStrategy();
            case REPORT_OUTPUT:
                return new ReportOutputStrategy();
            case FINANCIAL_ANALYSIS:
                return new FinancialAnalysisStrategy();
            case GRADE_ANALYSIS:
                return new GradeAnalysisStrategy();
            case BADUANJIN_SCORING:
                return new BaduanjinScoringStrategy();
            case COMMON:
                return new CommonStrategy();
            case DragonRaja:
                return new DragonRajaStrategy();
            case WEATHER_SKY:
                return new WeatherSkyStrategy();
            case DRAGON_RAJA_WEATHER:
                return new DragonRajaWeatherStrategy();
            case Reminder:
                return new ReminderStrategy();
            case JOTDOWN_RECOGNITION:
                return new JotdownRecognitionStrategy();
            case SHOPPING_INTENT:
                return new ShoppingIntentStrategy();
            case SHOPPING_PICK:
                return new ShoppingPickStrategy();
            default:
                throw new IllegalArgumentException("不支持的策略类型: " + strategyType);
        }
    }
}
