package com.hms.main.tbk.client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;

/**
 * 美团联盟「自助取链接口」通道
 *
 * 接口：GET https://openapi.meituan.com/api/generateLink
 * 入参：appkey / actId / sid / linkType / sign
 * 签名：sign = MD5(secret + 参数名升序拼接的 key1value1key2value2... + secret)，不含 sign 本身
 *
 * 注意：actId 必须用「我要推广-活动总览」里的当前有效活动，
 * 老的 33（外卖）/4（闪购）等活动已下线，会返回「活动已下线」。
 *
 * add by CYQ 2026-11-05 接入美团联盟
 */
@Component
public class MeituanTbkClient {

    private static final Logger log = LoggerFactory.getLogger(MeituanTbkClient.class);

    @Autowired
    private TbkProperties properties;

    private final RestTemplate restTemplate;

    public MeituanTbkClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String providerName() {
        return "meituan";
    }

    /**
     * 是否已配置美团应用
     */
    public boolean isConfigured() {
        TbkProperties.Meituan meituan = properties.getMeituan();
        return StringUtils.isNotEmpty(meituan.getAppkey())
                && StringUtils.isNotEmpty(meituan.getSecret())
                && StringUtils.isNotEmpty(meituan.getSid());
    }

    /**
     * 生成推广链接（使用配置里的默认活动与链接类型）
     */
    public String generateLink() {
        return generateLink(null, null);
    }

    /**
     * 生成推广链接
     *
     * @param actId    活动ID，为空时用 tbk.meituan.act-id
     * @param linkType 链接类型，为空时用 tbk.meituan.link-type
     * @return 推广长链
     */
    public String generateLink(String actId, Integer linkType) {
        TbkProperties.Meituan meituan = properties.getMeituan();
        if (!isConfigured()) {
            throw new ServiceException("尚未配置美团联盟，请填写 tbk.meituan.appkey / secret / sid");
        }
        String resolvedActId = StringUtils.isNotEmpty(actId) ? actId.trim() : meituan.getActId();
        if (StringUtils.isEmpty(resolvedActId)) {
            throw new ServiceException("尚未配置美团活动ID，请填写 tbk.meituan.act-id（我要推广-活动总览第一列）");
        }
        int resolvedLinkType = linkType != null ? linkType : meituan.getLinkType();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("appkey", meituan.getAppkey());
        params.put("actId", resolvedActId);
        params.put("sid", meituan.getSid());
        params.put("linkType", String.valueOf(resolvedLinkType));
        params.put("sign", sign(params, meituan.getSecret()));

        String url = meituan.getGateway() + "?" + toQueryString(params);
        String body;
        try {
            body = restTemplate.getForObject(URI.create(url), String.class);
        } catch (Exception e) {
            log.error("美团联盟取链请求失败 actId={}: {}", resolvedActId, e.getMessage(), e);
            throw new ServiceException("美团联盟服务暂时不可用，请稍后重试");
        }
        if (StringUtils.isEmpty(body)) {
            throw new ServiceException("美团联盟返回为空，请稍后重试");
        }
        log.info("美团联盟取链响应 actId={}: {}", resolvedActId, body);

        JSONObject json;
        try {
            json = JSON.parseObject(body);
        } catch (Exception e) {
            throw new ServiceException("美团联盟返回格式异常");
        }
        int status = json.getIntValue("status");
        String data = json.getString("data");
        if (status != 0 || StringUtils.isEmpty(data)) {
            String des = json.getString("des");
            throw new ServiceException("美团联盟取链失败：" + (StringUtils.isNotEmpty(des) ? des : "未知错误"));
        }
        return data;
    }

    /**
     * 当前生效的美团配置（排查 actId / sid 用）
     */
    public Map<String, Object> configInfo() {
        TbkProperties.Meituan meituan = properties.getMeituan();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("configured", isConfigured());
        info.put("media", mask(meituan.getAppkey()));
        info.put("sid", meituan.getSid());
        info.put("actId", meituan.getActId());
        info.put("linkType", meituan.getLinkType());
        info.put("gateway", meituan.getGateway());
        return info;
    }

    // ------------------------------------------------------------------ 签名

    /**
     * sign = MD5(secret + 按参数名升序拼接的 key1value1key2value2... + secret)
     */
    public static String sign(Map<String, String> params, String secret) {
        StringBuilder sb = new StringBuilder(secret == null ? "" : secret);
        Map<String, String> sorted = new TreeMap<>(params);
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if ("sign".equals(entry.getKey()) || StringUtils.isEmpty(entry.getValue())) {
                continue;
            }
            sb.append(entry.getKey()).append(entry.getValue());
        }
        sb.append(secret == null ? "" : secret);
        return DigestUtil.md5Hex(sb.toString());
    }

    private String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=')
              .append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(),
                      StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }

    private String mask(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if (value.length() <= 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
