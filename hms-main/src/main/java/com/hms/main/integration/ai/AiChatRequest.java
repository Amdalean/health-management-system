package com.hms.main.integration.ai;

import java.util.Map;

/**
 * AI 对话请求
 * <p>
 * 承载一次 AI 大模型 HTTP 调用的全部要素（地址、请求体、请求头、服务商、模型），
 * 由 {@link AiChatIntegrationStrategy} 统一发送并记录系统集成日志。
 *
 * add by CYQ 2026-10-19 AI 调用接入系统集成日志
 * @author 崔永琦
 */
public class AiChatRequest
{
    /** 请求地址 */
    private String url;

    /** 请求报文（JSON 字符串） */
    private String body;

    /** 请求头 */
    private Map<String, String> headers;

    /** 服务商标识，如 qwen / opencode-go / ollama */
    private String provider;

    /** 模型名称 */
    private String model;

    /** 单据标识 */
    private String billId;

    public AiChatRequest()
    {
    }

    public AiChatRequest(String url, String body, Map<String, String> headers, String provider, String model)
    {
        this(url, body, headers, provider, model, null);
    }

    public AiChatRequest(String url, String body, Map<String, String> headers, String provider, String model, String billId)
    {
        this.url = url;
        this.body = body;
        this.headers = headers;
        this.provider = provider;
        this.model = model;
        this.billId = billId;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }

    public void setHeaders(Map<String, String> headers)
    {
        this.headers = headers;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getBillId()
    {
        return billId;
    }

    public void setBillId(String billId)
    {
        this.billId = billId;
    }

    @Override
    public String toString()
    {
        return "AiChatRequest{provider='" + provider + "', model='" + model + "', billId='" + billId + "'}";
    }
}
