package com.hms.ai.strategy;

import java.util.Map;

/**
 * AI响应策略接口 - 不同策略对应不同场景
 */
public interface AIResponseStrategy {
    /**
     * 处理AI响应
     * @param input 用户输入
     * @param context 上下文信息
     * @return 处理后的响应
     */
    String processResponse(String input, Map<String, Object> context);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
}