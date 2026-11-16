package com.hms.ai.util;

/**
 * AI 响应清洗工具
 * <p>
 * 大模型返回的文本常被 Markdown 代码块包裹（如 ```json ... ```），直接交给 JSON 解析器会抛出异常。
 * 抽出为独立的静态工具，便于业务代码与单元测试共用同一套清洗规则。
 *
 * @author CYQ 2026-11-16 从 ScoreAnalysisTask 抽出，供成绩智能分析模块与单元测试复用
 */
public class AiJsonCleaner {

    private AiJsonCleaner() {
    }

    /**
     * 清理 AI 响应中的 Markdown 代码块标记，并裁剪首尾空白
     *
     * @param response AI 原始响应
     * @return 清理后的 JSON 字符串；入参为 null 时返回 null
     */
    public static String clean(String response) {
        if (response == null) {
            return null;
        }

        // 移除 ```json 和 ``` 标记
        String cleaned = response.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*", "");

        return cleaned.trim();
    }
}
