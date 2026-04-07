package com.hms.ollama;

import com.alibaba.fastjson2.JSONObject;
import com.hms.common.utils.http.HttpUtils;

import java.util.HashMap;

/**
 * Ollama 聊天工具类
 * 通过 HTTP 直接 call Ollama REST API
 * 
 * add by CYQ 2026年4月6日
 */
public class Chat {
    
    /**
     * Ollama服务主机地址
     */
    private static String host = "http://localhost:11434";
    
    /**
     * 默认使用的模型名称
     */
    private static String model = "qwen2";

    /**
     * 生成 AI 响应
     * @param msg 用户输入消息
     * @return AI 响应内容
     * @throws Exception 调用异常
     */
    public static String generate(String msg) throws Exception {
        // 构建请求JSON
        JSONObject requestJson = new JSONObject();
        requestJson.put("model", model);
        requestJson.put("prompt", msg);
        requestJson.put("stream", false);
        
        // 调用Ollama API
        String url = host + "/api/generate";
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        String response = HttpUtils.dopost_OKHttp(url, requestJson.toString(), headers);
        
        // 解析响应
        JSONObject jsonResponse = JSONObject.parseObject(response);
        if (jsonResponse.containsKey("response")) {
            return jsonResponse.getString("response");
        } else {
            return response;
        }
    }
    
    /**
     * 设置 Ollama 服务地址
     * @param host 服务地址
     */
    public static void setHost(String host) {
        Chat.host = host;
    }
    
    /**
     * 设置模型名称
     * @param model 模型名称
     */
    public static void setModel(String model) {
        Chat.model = model;
    }
}
