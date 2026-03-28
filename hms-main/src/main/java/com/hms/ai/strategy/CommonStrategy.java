package com.hms.ai.strategy;

import java.util.Map;

public class CommonStrategy implements AIResponseStrategy {
    @Override
    public String processResponse(String input, Map<String, Object> context) {
        return input;
    }

    @Override
    public String getStrategyName() {
        return "你是客户服务专家，请用温馨，有高智感的语气提醒重要的日期到了。";
    }
}
