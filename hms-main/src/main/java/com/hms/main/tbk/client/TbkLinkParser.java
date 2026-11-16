package com.hms.main.tbk.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.hms.main.tbk.domain.TbkMaterial;

/**
 * 淘宝链接/淘口令识别
 *
 * 支持：淘宝/天猫商品链接、m.tb.cn 短链、uland 二合一链接、s.click 推广链、
 *       ￥xxxx￥ 淘口令、纯商品ID。
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
public final class TbkLinkParser {

    /** 淘口令：￥AbCd1234￥ / $AbCd1234$ / ¥AbCd1234¥ */
    private static final Pattern TKL = Pattern.compile("[￥¥$][0-9A-Za-z]{8,16}[￥¥$]");

    /** URL */
    private static final Pattern URL = Pattern.compile("https?://[^\\s\"'<>，。；、）】]+", Pattern.CASE_INSENSITIVE);

    /** 链接中的商品ID */
    private static final Pattern ITEM_ID_IN_URL = Pattern.compile("[?&](?:id|itemId|item_id)=(\\d{6,20})");

    /** 纯商品ID */
    private static final Pattern PURE_ID = Pattern.compile("^(\\d{6,20})$");

    private TbkLinkParser() {
    }

    /**
     * 从任意文本中识别转链素材
     *
     * @param content 用户粘贴的原始文案
     * @return 识别结果，无法识别时返回 null
     */
    public static TbkMaterial parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        String text = content.trim();

        TbkMaterial material = new TbkMaterial();
        material.setRawContent(text);

        // 1. 优先取 URL（短链/长链/二合一链接都能直接交给聚合平台）
        Matcher urlMatcher = URL.matcher(text);
        if (urlMatcher.find()) {
            String url = trimTrailingPunctuation(urlMatcher.group());
            material.setMaterial(url);
            material.setType("url");
            material.setItemId(extractItemId(url));
            return material;
        }

        // 2. 淘口令
        Matcher tklMatcher = TKL.matcher(text);
        if (tklMatcher.find()) {
            material.setMaterial(tklMatcher.group());
            material.setType("tkl");
            return material;
        }

        // 3. 纯商品ID
        Matcher idMatcher = PURE_ID.matcher(text);
        if (idMatcher.matches()) {
            material.setMaterial(idMatcher.group(1));
            material.setItemId(idMatcher.group(1));
            material.setType("id");
            return material;
        }

        return null;
    }

    /**
     * 从链接中解析商品ID，解析不到返回 null
     */
    public static String extractItemId(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = ITEM_ID_IN_URL.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 去掉粘在链接尾部的标点，避免短链带脏字符
     */
    private static String trimTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0) {
            char c = url.charAt(end - 1);
            if (c == ',' || c == '.' || c == ';' || c == ')' || c == ']' || c == '，' || c == '。') {
                end--;
            } else {
                break;
            }
        }
        return url.substring(0, end);
    }
}
