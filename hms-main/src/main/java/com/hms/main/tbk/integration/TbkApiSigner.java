package com.hms.main.tbk.integration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.hms.common.utils.StringUtils;

/**
 * 淘宝客接口公共签名工具
 * <p>
 * 淘宝开放平台（TOP）与拼多多多多进宝（DDK）的签名规则一致：
 * {@code MD5(secret + 按参数名升序拼接的 k1v1k2v2... + secret)} 转大写，
 * 两边只在公共参数和时间戳格式上不同，这里统一收口，避免两套客户端各写一份。
 * <p>
 * add by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志
 */
public final class TbkApiSigner {

    private TbkApiSigner() {
    }

    /**
     * 接口签名：MD5(secret + 排序后的 k1v1k2v2... + secret) 转大写，签名参数自身不参与
     *
     * @param params 参与签名的参数（可包含 sign，会被跳过）
     * @param secret 应用密钥
     * @return 32 位大写签名
     */
    public static String sign(Map<String, String> params, String secret) {
        return md5Hex(buildSignSource(params, secret)).toUpperCase();
    }

    /**
     * 按签名规则拼接原文（抽出来便于单测和排查验签问题）
     */
    public static String buildSignSource(Map<String, String> params, String secret) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder(secret == null ? "" : secret);
        for (String key : keys) {
            if ("sign".equals(key)) {
                continue;
            }
            String value = params.get(key);
            if (StringUtils.isNotEmpty(value)) {
                sb.append(key).append(value);
            }
        }
        sb.append(secret == null ? "" : secret);
        return sb.toString();
    }

    /**
     * 淘宝 TOP 网关 query string
     * <p>
     * 注意：URLEncoder 会把空格编成 '+'，淘宝会当成字面加号导致 Invalid timestamp(code=31)，
     * 这里统一换成 %20。
     */
    public static String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            String value = URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            sb.append(entry.getKey()).append('=').append(value);
        }
        return sb.toString();
    }

    /**
     * 淘宝 TOP 时间戳：yyyy-MM-dd HH:mm:ss（固定 GMT+8）
     */
    public static String topTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return sdf.format(new Date());
    }

    /**
     * 拼多多时间戳：秒级 unix 时间戳
     */
    public static String unixTimestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    /**
     * 报文脱敏：保留前 4 位和后 4 位，中间用 **** 代替（用于 session / token 等）
     */
    public static String mask(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private static String md5Hex(String source) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                String h = Integer.toHexString(b & 0xFF);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("接口签名计算失败", e);
        }
    }
}
