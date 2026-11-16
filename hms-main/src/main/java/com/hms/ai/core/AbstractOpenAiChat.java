package com.hms.ai.core;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.ai.template.AbstractAIChatTemplate;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.hms.CommonUtil;
import com.hms.main.integration.ai.AiChatRequest;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI 兼容协议对话抽象基类
 * <p>
 * 封装 chat/completions 协议的公共请求构建、发送与响应解析（含文本/图片/视频），
 * 具体服务商（Qwen、OpenCode Go 等）只需提供 apiUrl / model / apiKey 以及额外请求头。
 *
 * add by CYQ 2026-09-21 AI 模块重构：服务商拆分
 */
@Log4j2
public abstract class AbstractOpenAiChat extends AbstractAIChatTemplate {

    public AbstractOpenAiChat(AIResponseStrategy strategy) {
        super(strategy);
    }

    /** API 地址 */
    protected abstract String getApiUrl();

    /** 模型名称 */
    protected abstract String getModel();

    /** API 密钥 */
    protected abstract String getApiKey();

    /**
     * 服务商标识，用于集成日志接收方展示
     *
     * add by CYQ 2026-10-19 AI 调用接入系统集成日志
     */
    protected abstract String getProvider();

    /** 额外请求头（如 OpenCode Go 的会话标识），默认无 */
    protected Map<String, String> extraHeaders() {
        return Collections.emptyMap();
    }

    @Override
    protected String preprocessInput(String input) {
        return input;
    }

    @Override
    protected String postprocessResponse(String response) {
        return response;
    }

    @Override
    protected String getSystemPrompt() {
        return strategy.getStrategyName();
    }

    /**
     * 策略声明低延迟时关闭模型的推理过程
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    protected boolean disableThinking() {
        return strategy != null && strategy.lowLatency();
    }

    /**
     * 调用 AI 服务 - OpenAI 兼容协议通用实现
     */
    @Override
    protected String callAIService(String input) {
        try {
            JSONObject json = new JSONObject();
            json.put("model", getModel());
            JSONArray messages = new JSONArray();

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", getSystemPrompt());

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");

            // add by CYQ 2026年3月1日 增加视频支持
            JSONArray videos = new JSONArray();
            JSONArray images = new JSONArray();
            if (this.context != null) {
                String type = CommonUtil.initstr(context.get("type"));
                String filePatch = CommonUtil.initstr(context.get("filePatch"));
                if ("video".equals(type)) {
                    videos = buildVideoData(input, filePatch);
                } else if ("image".equals(type)) {
                    images = buildImageData(input, filePatch);
                }
            }

            if (ObjectUtil.isNotEmpty(videos)) {
                userMessage.put("content", videos);
            } else if (ObjectUtil.isNotEmpty(images)) {
                userMessage.put("content", images);
            } else {
                userMessage.put("content", buildTextData(input));
            }
            messages.add(systemMessage);
            messages.add(userMessage);
            json.put("messages", messages);
            json.put("stream", false);

            // add by CYQ 2026-11-04 结构化输出场景关闭 Qwen3 推理过程，避免单次调用 70+ 秒
            if (disableThinking() && getModel() != null
                    && getModel().toLowerCase().contains("qwen")) {
                json.put("enable_thinking", false);
            }

            log.info("请求内容:{}", json.toString());

            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + getApiKey());
            Map<String, String> extra = extraHeaders();
            if (extra != null && !extra.isEmpty()) {
                headers.putAll(extra);
            }

            // add by CYQ 2026-10-19 统一走 AI 集成策略发送，自动记录系统集成日志
            AiChatRequest request = new AiChatRequest(getApiUrl(), json.toString(), headers, getProvider(), getModel());
            request.setBillId(resolveBillId());
            String ret = sendByIntegration(request);
            JSONObject obj = JSONObject.parseObject(ret);

            // 检查API是否返回错误
            JSONArray choices = obj.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                JSONObject error = obj.getJSONObject("error");
                String errMsg = error != null ? error.getString("message") : ret;
                log.error("AI服务返回异常响应:{}", ret);
                throw new BaseException("AI服务返回异常: " + errMsg);
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String reasoningContent = message.getString("reasoning_content");
            log.info("思考过程:{}", reasoningContent);
            String content = message.getString("content");
            log.info("回答正文:{}", content);

            return content;
        } catch (Exception e) {
            log.error("调用AI服务异常", e);
            throw new BaseException("AI服务调用失败: " + e.getMessage());
        }
    }

    //add by CYQ 2026年3月1日 增加视频模式处理逻辑
    private JSONArray buildVideoData(String input, String filePatch) {
        Assert.notEmpty(filePatch, "视频模式filePatch不能为空!");
        JSONArray contents = new JSONArray();

        JSONObject videoContent = new JSONObject();
        JSONObject videoUrl = new JSONObject();
        videoUrl.put("url", filePatch);
        videoContent.put("video_url", videoUrl);
        videoContent.put("type", "video_url");
        videoContent.put("fps", "0.2");

        JSONObject textContent = new JSONObject();
        textContent.put("text", input);
        textContent.put("type", "text");

        contents.add(videoContent);
        contents.add(textContent);
        return contents;
    }

    //add by CYQ 2026年6月1日 增加图片模式处理逻辑
    private JSONArray buildImageData(String input, String filePatch) {
        Assert.notEmpty(filePatch, "图片模式filePatch不能为空!");
        JSONArray contents = new JSONArray();

        JSONObject imageContent = new JSONObject();
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", filePatch);
        imageContent.put("image_url", imageUrl);
        imageContent.put("type", "image_url");

        JSONObject textContent = new JSONObject();
        textContent.put("text", input);
        textContent.put("type", "text");

        contents.add(imageContent);
        contents.add(textContent);
        return contents;
    }

    private JSONArray buildTextData(String input) {
        JSONArray contents = new JSONArray();
        JSONObject textContent = new JSONObject();
        textContent.put("text", input);
        textContent.put("type", "text");
        contents.add(textContent);
        return contents;
    }
}
