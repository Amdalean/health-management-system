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

    /**
     * 是否低延迟场景
     *
     * 结构化输出（如选品打分）不需要长思考，关闭推理过程可把单次调用从 70+ 秒降到几秒。
     * 仅对 Qwen3 系列模型生效。
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    default boolean lowLatency() {
        return false;
    }
}