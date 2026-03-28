package com.hms.ai.qwen;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.annotation.Log;
import com.hms.common.config.QwenConfig;
import com.hms.common.enums.BusinessType;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.http.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class Chat {
    private static final Logger log = LoggerFactory.getLogger(Chat.class);

    @Log(title = "获取qwen响应", businessType = BusinessType.OTHER)
    public String getBody(String req) throws BaseException {
        //请将XX，YY替换为身份和昵称
        JSONObject json = new JSONObject();
        json.put("model", QwenConfig.getModel());
        JSONArray messages = new JSONArray();

        JSONObject message1 = new JSONObject();
        message1.put("role", "system");
        message1.put("content", "你的名字是诺玛,卡塞尔学院参考作者江南的《龙族》系列小说中的人工智能诺玛设计了你。你现在需要为我们的明日新星,干员多肉小姐提供帮助。回复请使用markdown语法。");
        JSONObject message2 = new JSONObject();
        message2.put("role", "user");
        message2.put("content", req);
        messages.add(message1);
        messages.add(message2);
        json.put("messages", messages);
        json.put("stream", true);
        log.info("请求内容:{}",json.toString());
//        stream: true
//        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation?version-id=v1&task-group=aigc&task=text-generation&function-call=generation";
        String url = QwenConfig.getApiUrl();
        HashMap<String,String> head = new HashMap();
        head.put("Authorization", QwenConfig.getApiKey());

        JSONArray array = HttpUtils.postStreamingResponse(url, json.toString(), head);
        /**
         *
         StringBuffer sb = new StringBuffer(msg);
         sb.delete(sb.length()-1, sb.length());
         sb.delete(0, 1);

         //解码unicode字符
         String text = unicodeDecode(sb.toString());
         Logger.error("getGPTbody...msg="+text);
         * */
//        StringBuffer content = new StringBuffer();
        String reasoning_content = array.stream().map(v->{
            JSONObject obj = (JSONObject)v;
            JSONArray choices = (JSONArray)obj.get("choices");
            JSONObject choice = (JSONObject)choices.get(0);
            JSONObject delta = (JSONObject)choice.get("delta");
            Object reasoning_content_line = delta.get("reasoning_content");
            return reasoning_content_line== null ? null : reasoning_content_line.toString();
        }).filter(Objects::nonNull) // 使用标准空值过滤
        .collect(Collectors.joining());
        log.info("思考过程:{}",reasoning_content);
        String content = array.stream().map(v->{
                    JSONObject obj = (JSONObject)v;
                    JSONArray choices = (JSONArray)obj.get("choices");
                    JSONObject choice = (JSONObject)choices.get(0);
                    JSONObject delta = (JSONObject)choice.get("delta");
                    Object content_line = delta.get("content");
                    return content_line== null ? null : content_line.toString();
                }).filter(Objects::nonNull) // 使用标准空值过滤
                .collect(Collectors.joining());
        log.info("回答正文:{}",content);




//        Map map = CommonUtil.initMap(msg);
//        String code = CommonUtil.initstr(map.get("code"));
//        if(!"".equals(code)) {
//            throw new BaseException("调用通义千问异常，异常消息为，"+msg);
//        }
//        Map output = CommonUtil.initMap(map.get("output"));
//        String text = CommonUtil.initstr(output.get("text"));
        return content;
    }
}
