package com.hms.ai.template;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.common.config.QwenConfig;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.hms.CommonUtil;
import com.hms.common.utils.http.HttpUtils;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通义千问AI对话实现类
 * 集成hms-main/src/main/java/com/hms/ai/qwen/Chat.java的实际实现
 * 使用不同策略处理不同场景（如健康建议、报表输出、财务分析等）
 */
@Log4j2
public class QwenAIChat extends AbstractAIChatTemplate {

    public QwenAIChat(AIResponseStrategy strategy) {
        super(strategy);
    }

    @Override
    protected String preprocessInput(String input) {
        // Qwen特有的预处理逻辑
        // 可以根据策略类型添加特定的预处理步骤
        return input;
    }

    @Override
    protected String postprocessResponse(String response) {
        // Qwen特有的后处理逻辑
        // 在这里可以处理来自Qwen API的实际响应，并可能根据策略类型进行特定处理
        return response;
    }
    
    @Override
    protected String getSystemPrompt() {
        // 根据当前策略返回相应的系统提示词
        // 不同策略（健康建议、报表输出、财务分析、成绩分析等）可以有不同的系统提示词
        return strategy.getStrategyName();
    }

    /**
     * 调用AI服务 - 通用方法
     * @param input 用户输入
     * @return AI响应
     */
    @Override
    protected String callAIService(String input) {
        try {
            // 创建请求JSON
            JSONObject json = new JSONObject();
            json.put("model", QwenConfig.getModel());
            JSONArray messages = new JSONArray();

            // 系统消息 - 可以根据策略定制
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", getSystemPrompt());

            // 用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");

            //add by CYQ 2026年3月1日 增加视频支持
            JSONArray videos = new JSONArray();
            if(this.context!=null){
                String type = CommonUtil.initstr(context.get("type"));
                String filePatch = CommonUtil.initstr(context.get("filePatch"));
                if("video".equals(type)){
                    videos = buildVideoData(input,filePatch);
                }
            }

            if(ObjectUtil.isNotEmpty(videos)){
                userMessage.put("content", videos);
            }else{
                userMessage.put("content", buildTextData(input));
            }
            messages.add(systemMessage);
            messages.add(userMessage);
            json.put("messages", messages);
            json.put("stream", true);

            log.info("请求内容:{}", json.toString());

            String url = QwenConfig.getApiUrl();
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", QwenConfig.getApiKey());

            JSONArray responseArray = HttpUtils.postStreamingResponse(url, json.toString(), headers);

            // 提取思考过程
            String reasoningContent = responseArray.stream()
                    .map(v -> {
                        JSONObject obj = (JSONObject) v;
                        JSONArray choices = (JSONArray) obj.get("choices");
                        JSONObject choice = (JSONObject) choices.get(0);
                        JSONObject delta = (JSONObject) choice.get("delta");
                        Object reasoningContentLine = delta.get("reasoning_content");
                        return reasoningContentLine == null ? null : reasoningContentLine.toString();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining());
            log.info("思考过程:{}", reasoningContent);

            // 提取主要内容
            String content = responseArray.stream()
                    .map(v -> {
                        JSONObject obj = (JSONObject) v;
                        JSONArray choices = (JSONArray) obj.get("choices");
                        JSONObject choice = (JSONObject) choices.get(0);
                        JSONObject delta = (JSONObject) choice.get("delta");
                        Object contentLine = delta.get("content");
                        return contentLine == null ? null : contentLine.toString();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining());
            log.info("回答正文:{}", content);

            return content;
        } catch (BaseException e) {
            log.error("调用AI服务异常", e);
            return "AI服务调用失败: " + e.getMessage();
        }
    }
    //add by CYQ 2026年3月1日 增加视频模式处理逻辑
    private JSONArray buildVideoData(String input,String filePatch){
        Assert.notEmpty(filePatch,"视频模式filePatch不能为空!");
        JSONArray contents = new JSONArray();

        JSONObject videoContent = new JSONObject();
        JSONObject videoUrl = new JSONObject();
        videoUrl.put("url", filePatch);
        videoContent.put("video_url",videoUrl);
        videoContent.put("type","video_url");
        videoContent.put("fps","0.2");

        JSONObject textContent = new JSONObject();
        textContent.put("text",input);
        textContent.put("type","text");

        contents.add(videoContent);
        contents.add(textContent);
        return contents;
    }
    //add by CYQ 2026年3月1日 增加视频模式处理逻辑
    private JSONArray buildTextData(String input){
        JSONArray contents = new JSONArray();
        JSONObject textContent = new JSONObject();
        textContent.put("text",input);
        textContent.put("type","text");
        contents.add(textContent);
        return contents;
    }
}