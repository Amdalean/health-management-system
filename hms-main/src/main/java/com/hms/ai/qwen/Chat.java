package com.hms.ai.qwen;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.config.QwenConfig;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.http.HttpUtils;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class Chat {
    public String getBody(String req) throws BaseException {
        //请将XX，YY替换为身份和昵称
        JSONObject json = new JSONObject();
        json.put("model", QwenConfig.getModel());
        JSONArray messages = new JSONArray();

        JSONObject message1 = new JSONObject();
        message1.put("role", "system");
        message1.put("content", "伟大的多肉小姐姐收留了你,你必须写出真挚动人宠溺的情话来打动她!");
        JSONObject message2 = new JSONObject();
        message2.put("role", "user");
        message2.put("content", req);
        messages.add(message1);
        messages.add(message2);
        json.put("messages", messages);
        json.put("stream", true);
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
        System.out.println(reasoning_content);
        String content = array.stream().map(v->{
                    JSONObject obj = (JSONObject)v;
                    JSONArray choices = (JSONArray)obj.get("choices");
                    JSONObject choice = (JSONObject)choices.get(0);
                    JSONObject delta = (JSONObject)choice.get("delta");
                    Object content_line = delta.get("content");
                    return content_line== null ? null : content_line.toString();
                }).filter(Objects::nonNull) // 使用标准空值过滤
                .collect(Collectors.joining());
        System.out.println(content);




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
