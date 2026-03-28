package com.hms.quartz.task;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.config.WxConfig;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Log4j2
@Component("WxMsgTask")
public class WxMsgTask extends FragonRajaBaseTask{
    @Autowired
    private WxConfig config;
    public String excute() throws IOException {
        String token = getToken();
        String msg_str = getMsg();
        JSONObject msg = covMsg(msg_str);
        sendMsg(token,msg);
        return "执行微信消息任务成功";
    }
    private String getMsg(){
        String gd = getGDAPI();//获取明天天气
        String gptbody = getgptMsg(gd);//通过qwen获取信息
        return removeTags(gptbody);
    }
    private JSONObject covMsg(String msg) {
        JSONObject json = new JSONObject();
        json.put("touser", config.getToUser());
        json.put("template_id", config.getTemplateId());
//        json.put("url", "http://weixin.qq.com/download");
        json.put("topcolor", "#FF0000");

        JSONObject data = new JSONObject();
        JSONObject todayNote = new JSONObject();
        todayNote.put("value", msg);
        todayNote.put("color", "#173177");
        data.put("today_note", todayNote);

        json.put("data", data);
        return json;
    }
    public String getToken() throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(config.getUrl()+"cgi-bin/token?grant_type=client_credential&appid="+config.getAppid()+"&secret="+config.getSecret())
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        JSONObject jsonObject = JSONObject.parseObject(response.body().string());
        String access_token = jsonObject.getString("access_token");
        Assert.notEmpty(access_token);
        return access_token;
    }
    public void sendMsg(String token,JSONObject json) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, json.toString());
        Request request = new Request.Builder()
                .url(config.getUrl()+"cgi-bin/message/template/send?access_token="+token)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        log.info(response.body().string());
    }
}
