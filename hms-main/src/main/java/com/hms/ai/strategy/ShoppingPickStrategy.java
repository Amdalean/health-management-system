package com.hms.ai.strategy;

import java.util.Map;

/**
 * 跨平台高性价比选品打分策略
 *
 * 输入为用户需求 + 候选商品清单（价格/券/销量/店铺），
 * 输出每个候选的性价比分、推荐理由、标签与风险提示。
 *
 * add by CYQ 2026-11-04 优惠查询-AI 选品
 */
public class ShoppingPickStrategy implements AIResponseStrategy {

    private static final String SYSTEM_PROMPT =
        "你是一名资深购物顾问，擅长判断「高性价比」。你会收到用户需求、预算和一组来自淘宝/拼多多的候选商品。\n" +
        "请按以下维度综合打分（0-100）：需求匹配度 35 分、券后价与同组价格分布 25 分、优惠力度 15 分、\n" +
        "销量与店铺可信度 15 分、风险扣分 10 分。\n" +
        "只输出 JSON，不要输出任何解释：\n" +
        "{\n" +
        "  \"summary\": \"一句话结论，不超过30字\",\n" +
        "  \"bestIndex\": 1,\n" +
        "  \"items\": [\n" +
        "    {\"index\": 1, \"score\": 88, \"reason\": \"不超过40字的推荐理由\", \"tags\": [\"最多3个标签\"], \"caution\": \"风险提示，没有则空字符串\"}\n" +
        "  ],\n" +
        "  \"advice\": \"购买建议，不超过80字\"\n" +
        "}\n" +
        "规则：\n" +
        "1. items 必须覆盖输入的每一个候选，index 严格对应输入编号；\n" +
        "2. 价格明显低于同组、但销量或店铺信息缺失时，要提示风险，不能给高分；\n" +
        "3. 不确定的信息不要编造，可写「需自行核对」；\n" +
        "4. 只输出 JSON 本身。";

    @Override
    public String processResponse(String input, Map<String, Object> context) {
        return input;
    }

    @Override
    public String getStrategyName() {
        return SYSTEM_PROMPT;
    }

    @Override
    public boolean lowLatency() {
        return true;
    }
}
