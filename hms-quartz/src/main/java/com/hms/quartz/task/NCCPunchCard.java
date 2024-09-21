package com.hms.quartz.task;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * add by CYQ 2024年9月12日
 * */
@Component("NCCPunchCard")
public class NCCPunchCard {
    /** 打卡 */
    public void task() throws Exception {
        System.out.println("正在调用打卡功能");
        String token = login();
        String ret = clock(token);
        ret = "nccDev打卡服务访问成功,"+ret;
        System.out.println("ret：" + ret);
    }

    private static CommonUtil util = new CommonUtil();
    /** 打卡 */
    private static String clock(String token) throws Exception {
        Map addHeader = new HashMap();
        addHeader.put("authority", "nccdev.yonyou.com");
        addHeader.put("method", "POST");
        addHeader.put("path", "/market-api/system/clock/clock");
        addHeader.put("scheme", "https");
        addHeader.put("accept", "*/*");
        addHeader.put("accept-encoding", "gzip, deflate, br");
        addHeader.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        addHeader.put("accept-charset", "utf-8");
        addHeader.put("authorization","Bearer "+token);
        addHeader.put("origin", "https://nccdev.yonyou.com");
        addHeader.put("referer", "https://nccdev.yonyou.com/profile/index");
        addHeader.put("sec-ch-ua", "`Not.A/Brand`;v=`8`, `Chromium`;v=`114`, `Google Chrome`;v=`114`");
        addHeader.put("sec-ch-ua-mobile", "?0");
        addHeader.put("sec-ch-ua-platform", "`Windows`");
        addHeader.put("sec-fetch-dest", "empty");
        addHeader.put("sec-fetch-mode", "cors");;
        addHeader.put("sec-fetch-site", "same-origin");
        String ret = dopost_OKHttp("https://nccdev.yonyou.com/market-api/system/clock/clock", "", addHeader);
//        Logger.error("clockret="+ret);
        return ret;
    }
    /**
     * 登录
     * */
    private static String login() throws Exception {
        try {
            if(1==1){
                //先用前台token试试
                return "eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6IjA4ODI3NGM3LTQ5ZmMtNGZkZC1iNWUwLTU4M2VmM2U5NDFhNSJ9.kbBv5KayFSCTurHVoYEm2f8sl_YtrBksAa073zVUH9bLNFaBz1_1f3WG6ZtTOjZVPhXjN9uPo6iuVInqCQQa5Q; Hm_lpvt_79cba97204629d9fcf1ed81661fa43d3=1726197569";
            }
            String user = util.initstr("15286891906");
            String pwd = util.initstr("pwd");
            String loginjson = "{\"username\":\""+user+"\",\"password\":\""+pwd+"\"}    ";
            String loginret = dopost_OKHttp("https://nccdev.yonyou.com/market-api/login", loginjson, null);
            Map map = util.initMap(loginret);
            System.out.println("loginret="+loginret);
            String code = util.initstr(map.get("code"));
            String msg = util.initstr(map.get("msg"));
            String token = util.initstr(map.get("token"));
            if (!"200".equals(code)) {
                throw new Exception("登录失败," + msg);
            }
            return token;
        } catch (Exception e) {
            // TODO: handle exception
            throw new Exception("login失败," + e.getMessage());
        }

    }
    /**
     * 调用OKhttp_Post请求
     * @return
     * @throws IOException
     */
    private static String dopost_OKHttp(String url, String json, Map<String, String> head) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/json");
        if(json==null || json.isEmpty()) {
            json = "";
        }
        RequestBody body = RequestBody.create(mediaType, json);
        Builder build = new Request.Builder().url(url).method("POST", body).addHeader("Content-Type",
                "application/json");
        if (head != null && head.size() > 0) {
            for (String key : head.keySet()) {
                String value = head.get(key);
                build.addHeader(key, value);
            }
        }
        Request request = build.build();
        Response response = client.newCall(request).execute();
        String ret = response.body().string();
        return ret;
    }
}
