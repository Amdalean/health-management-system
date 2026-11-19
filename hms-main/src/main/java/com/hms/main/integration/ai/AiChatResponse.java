package com.hms.main.integration.ai;

/**
 * AI 对话响应
 *
 * add by CYQ 2026-10-19 AI 调用接入系统集成日志
 * @author 崔永琦
 */
public class AiChatResponse
{
    /** 原始响应报文，由调用方自行解析 */
    private String raw;

    public AiChatResponse()
    {
    }

    public AiChatResponse(String raw)
    {
        this.raw = raw;
    }

    public String getRaw()
    {
        return raw;
    }

    public void setRaw(String raw)
    {
        this.raw = raw;
    }

    @Override
    public String toString()
    {
        return "AiChatResponse{raw=" + (raw == null ? "null" : raw.length() + " 字符") + "}";
    }
}
