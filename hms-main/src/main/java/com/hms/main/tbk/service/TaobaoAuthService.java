package com.hms.main.tbk.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.constant.CacheConstants;
import com.hms.common.core.redis.RedisCache;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;

/**
 * 淘宝开放平台 OAuth 授权
 *
 * taobao.tbk.privilege.get 需要 session，走本服务完成一次授权后自动续期。
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Service
public class TaobaoAuthService {

    private static final Logger log = LoggerFactory.getLogger(TaobaoAuthService.class);

    private static final String TOKEN_KEY = CacheConstants.TBK_TAOBAO_TOKEN_KEY;

    /** 提前 5 分钟续期 */
    private static final long REFRESH_AHEAD_MS = 5 * 60 * 1000L;

    @Autowired
    private TbkProperties properties;

    @Autowired
    private RedisCache redisCache;

    private final RestTemplate restTemplate;

    public TaobaoAuthService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(20000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 生成授权页地址
     */
    public String buildAuthorizeUrl() {
        TbkProperties.Taobao taobao = properties.getTaobao();
        if (StringUtils.isEmpty(taobao.getRedirectUri())) {
            throw new ServiceException("尚未配置 OAuth 回调地址 tbk.taobao.redirect-uri");
        }
        return taobao.getOauthAuthorizeUrl()
                + "?response_type=code"
                + "&client_id=" + taobao.getAppKey()
                + "&redirect_uri=" + urlEncode(taobao.getRedirectUri())
                + "&state=" + UUID.randomUUID().toString().replace("-", "")
                + "&view=web";
    }

    /**
     * 回调：用 code 换 token
     */
    public String exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getTaobao().getAppKey());
        form.add("client_secret", properties.getTaobao().getAppSecret());
        form.add("code", code);
        form.add("redirect_uri", properties.getTaobao().getRedirectUri());
        return requestToken(form, "授权");
    }

    /**
     * 刷新 token
     */
    public String refreshToken() {
        JSONObject token = loadToken();
        if (token == null || StringUtils.isEmpty(token.getString("refreshToken"))) {
            throw new ServiceException("没有可用的 refresh_token，请重新访问 /tbk/auth/start 授权");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.getTaobao().getAppKey());
        form.add("client_secret", properties.getTaobao().getAppSecret());
        form.add("refresh_token", token.getString("refreshToken"));
        return requestToken(form, "续期");
    }

    private String requestToken(MultiValueMap<String, String> form, String action) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body;
        try {
            body = restTemplate.postForObject(properties.getTaobao().getOauthTokenUrl(),
                    new HttpEntity<>(form, headers), String.class);
        } catch (Exception e) {
            log.error("淘宝 OAuth {} 请求失败: {}", action, e.getMessage(), e);
            throw new ServiceException("淘宝" + action + "请求失败，请稍后重试");
        }
        if (StringUtils.isEmpty(body)) {
            throw new ServiceException("淘宝" + action + "返回为空");
        }
        log.info("淘宝 OAuth {} 响应: {}", action, body);

        JSONObject json = JSON.parseObject(body);
        String accessToken = json.getString("access_token");
        if (StringUtils.isEmpty(accessToken)) {
            String desc = json.getString("error_description");
            throw new ServiceException(action + "失败：" + (StringUtils.isNotEmpty(desc) ? desc : body));
        }

        long expiresIn = json.getLongValue("expires_in", 86400L);
        JSONObject token = new JSONObject();
        token.put("accessToken", accessToken);
        token.put("refreshToken", json.getString("refresh_token"));
        token.put("expiresAt", System.currentTimeMillis() + expiresIn * 1000L);
        token.put("nick", json.getString("taobao_user_nick"));
        token.put("userId", json.getString("taobao_user_id"));

        JSONObject old = loadToken();
        if (old != null && StringUtils.isEmpty(token.getString("refreshToken"))) {
            token.put("refreshToken", old.getString("refreshToken"));
        }
        saveToken(token);
        return accessToken;
    }

    /**
     * 获取可用 session，没有则返回 null
     */
    public String getSession() {
        TbkProperties.Taobao taobao = properties.getTaobao();
        if (StringUtils.isNotEmpty(taobao.getSession())) {
            return taobao.getSession();
        }
        JSONObject token = loadToken();
        if (token == null) {
            return null;
        }
        long expiresAt = token.getLongValue("expiresAt");
        if (expiresAt - System.currentTimeMillis() < REFRESH_AHEAD_MS
                && StringUtils.isNotEmpty(token.getString("refreshToken"))) {
            try {
                return refreshToken();
            } catch (Exception e) {
                log.warn("淘宝 token 续期失败，尝试使用旧 token: {}", e.getMessage());
            }
        }
        return token.getString("accessToken");
    }

    /**
     * 当前授权状态（不返回 token 明文）
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        JSONObject token = loadToken();
        if (token == null) {
            status.put("authorized", false);
            status.put("tip", "未授权，请访问 /tbk/auth/start");
            return status;
        }
        long expiresAt = token.getLongValue("expiresAt");
        status.put("authorized", expiresAt > System.currentTimeMillis());
        status.put("nick", token.getString("nick"));
        status.put("expiresAt", expiresAt);
        return status;
    }

    private JSONObject loadToken() {
        try {
            String json = redisCache.getCacheObject(TOKEN_KEY);
            return StringUtils.isEmpty(json) ? null : JSON.parseObject(json);
        } catch (Exception e) {
            log.debug("读取淘宝 token 失败: {}", e.getMessage());
            return null;
        }
    }

    private void saveToken(JSONObject token) {
        try {
            // 兜底 25 天，refresh_token 失效后需要重新授权
            redisCache.setCacheObject(TOKEN_KEY, token.toJSONString(), 25, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new ServiceException("保存淘宝授权信息失败: " + e.getMessage());
        }
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
