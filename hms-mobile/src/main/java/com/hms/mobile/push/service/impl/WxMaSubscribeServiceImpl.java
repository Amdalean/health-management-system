package com.hms.mobile.push.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.mobile.push.config.WxMaProperties;
import com.hms.mobile.push.service.IWxMaSubscribeService;

/**
 * 微信小程序订阅消息服务实现
 * add by CYQ 2026-06-10
 */
@SuppressWarnings("unchecked")
@Service
public class WxMaSubscribeServiceImpl implements IWxMaSubscribeService {

    private static final Logger log = LoggerFactory.getLogger(WxMaSubscribeServiceImpl.class);

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token"
            + "?grant_type=client_credential&appid=%s&secret=%s";

    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send"
            + "?access_token=%s";

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session"
            + "?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    @Autowired
    private WxMaProperties wxMaProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 缓存的 access_token */
    private volatile String cachedToken;

    /** token 过期时间戳（提前 5 分钟刷新） */
    private volatile long tokenExpireTime;

    @Override
    public synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpireTime) {
            return cachedToken;
        }
        String appId = wxMaProperties.getAppId();
        String secret = wxMaProperties.getSecret();
        if (appId == null || secret == null) {
            log.error("微信小程序 AppID 或 AppSecret 未配置");
            return null;
        }
        try {
            String url = String.format(TOKEN_URL, appId, secret);
            String response = httpGet(url);
            Map<String, Object> map = objectMapper.readValue(response, Map.class);
            if (map.containsKey("access_token")) {
                cachedToken = (String) map.get("access_token");
                Integer expiresIn = (Integer) map.get("expires_in");
                tokenExpireTime = now + ((expiresIn != null ? expiresIn : 7200) - 300) * 1000L;
                log.info("微信 access_token 刷新成功");
                return cachedToken;
            } else {
                log.error("获取微信 access_token 失败: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("获取微信 access_token 异常", e);
            return null;
        }
    }

    @Override
    public String sendSubscribeMessage(String openId, String page, Map<String, Object> data) {
        String token = getAccessToken();
        if (token == null) {
            return "{\"errcode\":-1,\"errmsg\":\"access_token 获取失败\"}";
        }
        try {
            String url = String.format(SEND_URL, token);
            Map<String, Object> body = new HashMap<>();
            body.put("touser", openId);
            body.put("template_id", wxMaProperties.getTemplateId());
            body.put("page", page != null ? page : "");
            body.put("data", data);
            body.put("miniprogram_state", "formal");

            String json = objectMapper.writeValueAsString(body);
            String response = httpPost(url, json);
            log.info("微信订阅消息发送: openId={}, response={}", openId, response);
            return response;
        } catch (Exception e) {
            log.error("发送微信订阅消息异常: openId={}", openId, e);
            return "{\"errcode\":-1,\"errmsg\":\"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public String sendClockinRemind(String openId, String name, String date, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("name3", mapValue(name));
        data.put("date4", mapValue(date));
        data.put("phrase6", mapValue(status));
        return sendSubscribeMessage(openId, "pages/clockin/index", data);
    }

    /**
     * 通过 wx.login() code 换取 openid
     * add by CYQ 2026-06-10
     */
    @Override
    public Map<String, Object> code2session(String code) {
        Map<String, Object> result = new HashMap<>();
        try {
            String appId = wxMaProperties.getAppId();
            String secret = wxMaProperties.getSecret();
            String url = String.format(CODE2SESSION_URL, appId, secret, code);
            String response = httpGet(url);
            Map<String, Object> map = objectMapper.readValue(response, Map.class);
            if (map.containsKey("openid")) {
                result.put("openid", map.get("openid"));
                result.put("session_key", map.get("session_key"));
                if (map.containsKey("unionid")) {
                    result.put("unionid", map.get("unionid"));
                }
                log.info("code2session 成功: openid={}", map.get("openid"));
            } else {
                result.put("errcode", map.getOrDefault("errcode", -1));
                result.put("errmsg", map.getOrDefault("errmsg", "未知错误"));
                log.error("code2session 失败: {}", response);
            }
        } catch (Exception e) {
            log.error("code2session 异常: code={}", code, e);
            result.put("errcode", -1);
            result.put("errmsg", e.getMessage());
        }
        return result;
    }

    private Map<String, String> mapValue(String value) {
        Map<String, String> m = new HashMap<>();
        m.put("value", value != null ? value : "");
        return m;
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return readResponse(conn);
    }

    private String httpPost(String urlStr, String json) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("UTF-8"));
        }
        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
