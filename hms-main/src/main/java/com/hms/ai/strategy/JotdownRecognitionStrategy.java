package com.hms.ai.strategy;

import java.util.Map;

/**
 * 随手记图片识别策略实现
 * 从账单/发票图片中提取：说明、金额、日期、费用类型、备注
 *
 * add by CYQ 2026年6月1日
 */
public class JotdownRecognitionStrategy implements AIResponseStrategy {

    private static final String SYSTEM_PROMPT =
        "你是一个专业的票据识别助手，能够从账单、发票、收据等图片中提取关键财务信息。" +
        "请严格按以下JSON格式返回识别结果（只返回JSON，不要包含其他文字）：\n" +
        "{\n" +
        "  \"description\": \"账单描述或项目名称\",\n" +
        "  \"amount\": \"金额数字(如 128.50)\",\n" +
        "  \"date\": \"日期(格式: yyyy-MM-dd)\",\n" +
        "  \"expenseType\": \"费用类型(必须是以下之一: 1-餐饮, 2-交通, 3-购物, 4-娱乐, 5-医疗, 6-住房, 9-其他)\",\n" +
        "  \"remarks\": \"备注信息\"\n" +
        "}\n" +
        "识别规则：\n" +
        "1. 费用类型使用数字编码：餐饮=1, 交通=2, 购物=3, 娱乐=4, 医疗=5, 住房=6, 其他=9\n" +
        "2. 无法识别的字段设为 null\n" +
        "3. 金额只保留数字和小数点\n" +
        "4. 日期统一为 yyyy-MM-dd 格式";

    @Override
    public String processResponse(String input, Map<String, Object> context) {
        return input;
    }

    @Override
    public String getStrategyName() {
        return SYSTEM_PROMPT;
    }
}
