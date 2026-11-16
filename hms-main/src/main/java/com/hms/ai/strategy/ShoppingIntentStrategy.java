package com.hms.ai.strategy;

import java.util.Map;

/**
 * 购物需求解析策略
 *
 * 把用户一句自然语言需求拆成「平台搜索词 + 预算 + 偏好 + 大类」，
 * 供选品服务去淘宝/拼多多搜索候选商品。
 *
 * add by CYQ 2026-11-04 优惠查询-AI 选品
 */
public class ShoppingIntentStrategy implements AIResponseStrategy {

    private static final String SYSTEM_PROMPT =
        "你是一个专业的电商选品需求分析助手。用户会用一句自然语言描述想买的东西（可能包含预算、用途、偏好）。\n" +
        "请把它提取成用于电商平台搜索的结构，只输出 JSON，不要输出任何解释：\n" +
        "{\n" +
        "  \"keyword\": \"核心商品词，8-20字\",\n" +
        "  \"fallbackKeyword\": \"更宽泛的商品词，2-6字\",\n" +
        "  \"budget\": 数字或null,\n" +
        "  \"preferences\": [\"偏好点\"],\n" +
        "  \"category\": \"商品大类\"\n" +
        "}\n" +
        "规则：\n" +
        "1. keyword 必须是可以直接在淘宝/拼多多搜索的商品词，例如「降噪蓝牙耳机」，不要包含「想要」「帮我推荐」「200元以内」等词；\n" +
        "2. fallbackKeyword 是去掉修饰词后的宽泛商品词，例如「降噪蓝牙耳机」→「蓝牙耳机」、「静音机械键盘」→「机械键盘」；\n" +
        "3. budget 单位为元：用户说「300左右」取 300，「不超500」取 500，没提到预算则为 null；\n" +
        "4. preferences 最多 4 个，只能来自用户描述中的功能/场景/品牌要求，不要编造；\n" +
        "5. category 用简短的商品大类，如「数码」「家电」「食品」。";

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
