package com.hms.main.tbk.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkConvertResult;

/**
 * 大淘客「高效转链」通道
 *
 * 两步拿到全部结果：
 * 1. /goods/get-goods-details 用数字商品ID或新商品ID换商品详情（标题、主图、券后价、新商品ID）；
 * 2. /tb-service/get-privilege-link 用新商品ID换二合一领券链接、推广短链和淘口令。
 *
 * 注意：联盟已不允许数字ID转链，第二步必须传 goodsSign（新商品ID）。
 *
 * 文档：https://www.dataoke.com/pmc/openapi.html
 *
 * add by CYQ 2026-11-03 淘宝客转链模块
 */
@Component
@ConditionalOnProperty(prefix = "tbk", name = "provider", havingValue = "dataoke")
public class DataokeTbkClient implements TbkAggregatorClient {

    private static final Logger log = LoggerFactory.getLogger(DataokeTbkClient.class);

    /** 新商品ID：数字ID以外的长串（联盟升级后的商品标识） */
    private static final java.util.regex.Pattern NEW_ITEM_ID = java.util.regex.Pattern.compile("^[0-9A-Za-z_-]{16,64}$");

    @Autowired
    private TbkProperties properties;

    private final RestTemplate restTemplate;

    public DataokeTbkClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public TbkConvertResult convert(String material, String itemId) {
        TbkProperties.Dataoke dataoke = properties.getDataoke();
        if (StringUtils.isEmpty(dataoke.getAppKey()) || StringUtils.isEmpty(dataoke.getAppSecret())) {
            throw new ServiceException("尚未配置大淘客 appKey/appSecret，请先在 application.yml 的 tbk.dataoke 下填写");
        }
        String pid = StringUtils.isNotEmpty(dataoke.getPid()) ? dataoke.getPid() : properties.getPid();
        if (StringUtils.isEmpty(pid)) {
            throw new ServiceException("尚未配置推广位 pid，请填写 tbk.pid");
        }

        // 1. 万能解析：链接 / 淘口令 / 商品ID 直接出推广链接和淘口令。
        //    大淘客商品库只收录部分商品，走解析接口才不会被"库里没有该商品"挡住。
        String parseContent = buildParseContent(material, itemId);
        if (StringUtils.isNotEmpty(parseContent)) {
            TbkConvertResult parsed = tryParseContent(parseContent, pid);
            if (parsed != null) {
                return parsed;
            }
        }

        // 2. 回退：只有商品ID（含新商品ID）时走「详情 + 高效转链」
        String goodsKey = resolveGoodsKey(material, itemId);

        Map<String, String> detailParams = new LinkedHashMap<>();
        detailParams.put("version", dataoke.getDetailVersion());
        detailParams.put("id", goodsKey);
        JSONObject detail = call("/goods/get-goods-details", detailParams,
                "该商品暂时无法转链：可能已下架，或商家没有开通淘宝客佣金/优惠券，请换一个商品试试");

        String newItemId = firstString(detail, "goodsSign", "goodsId");
        if (StringUtils.isEmpty(newItemId)) {
            throw new ServiceException("该商品暂时无法转链：可能已下架，或商家没有开通淘宝客佣金/优惠券，请换一个商品试试");
        }

        // 高效转链：必须传新商品ID
        Map<String, String> linkParams = new LinkedHashMap<>();
        linkParams.put("version", dataoke.getConvertVersion());
        linkParams.put("goodsId", newItemId);
        linkParams.put("pid", pid);
        JSONObject link = call("/tb-service/get-privilege-link", linkParams);

        TbkConvertResult result = new TbkConvertResult();
        // detail 里的 id 是大淘客内部ID，对用户展示商品编号要用入参的商品ID
        result.setItemId(goodsKey);
        result.setTitle(firstString(detail, "title", "dtitle"));
        result.setPicUrl(firstString(detail, "mainPic", "main_pic", "picUrl"));
        result.setOriginalPrice(firstDecimal(detail, "originalPrice"));
        result.setFinalPrice(firstDecimal(detail, "actualPrice"));
        result.setCouponAmount(firstDecimal(detail, "couponPrice"));
        result.setCommissionRate(firstDecimal(detail, "commissionRate"));
        result.setCommission(estimateCommission(result.getFinalPrice(), result.getCommissionRate()));
        result.setCouponInfo(firstString(link, "couponInfo"));
        // couponClickUrl 是二合一领券链接（打开后是官方领券页），没有券时退回商品推广短链
        result.setShortUrl(firstString(link, "couponClickUrl", "itemUrl"));
        result.setTpwd(firstString(link, "tpwd", "longTpwd"));
        result.setItemUrl(firstString(detail, "itemLink"));
        if (StringUtils.isEmpty(result.getItemUrl()) && StringUtils.isNotEmpty(result.getItemId())) {
            result.setItemUrl("https://item.taobao.com/item.htm?id=" + result.getItemId());
        }
        result.setDemo(false);
        result.setProvider("dataoke");

        if (result.getCouponAmount() == null || result.getCouponAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.setNotice("该商品暂无优惠券，已生成推广链接");
        }
        if (StringUtils.isEmpty(result.getTpwd())) {
            result.setNotice(appendNotice(result.getNotice(), "淘口令生成失败，可直接使用推广链接"));
        }
        return result;
    }

    @Override
    public String providerName() {
        return "dataoke";
    }

    // ------------------------------------------------------------------ 万能解析

    /**
     * 构造「淘系万能解析」入参：
     * - 链接 / 淘口令 / 二合一链接：原样传入；
     * - 纯数字商品ID：拼成商品详情页链接（接口不认裸数字ID）；
     * - 新商品ID：接口不认，返回 null 走详情+转链回退。
     */
    private String buildParseContent(String material, String itemId) {
        String text = StringUtils.isNotEmpty(material) ? material.trim() : null;
        if (StringUtils.isEmpty(text)) {
            return StringUtils.isNotEmpty(itemId) && isNumericId(itemId) ? itemDetailUrl(itemId) : null;
        }
        if (text.startsWith("http") || text.startsWith("￥") || text.startsWith("$") || text.startsWith("¥")) {
            return text;
        }
        if (isNumericId(text)) {
            return itemDetailUrl(text);
        }
        if (StringUtils.isNotEmpty(itemId) && isNumericId(itemId)) {
            return itemDetailUrl(itemId);
        }
        return null;
    }

    /**
     * 调用淘系万能解析，解析不到或接口异常返回 null，由调用方回退
     */
    private TbkConvertResult tryParseContent(String content, String pid) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("content", content);
        params.put("pid", pid);
        JSONObject data;
        try {
            data = call("/tb-service/parse-content", params, "没有解析到商品信息，请确认链接是否有效");
        } catch (ServiceException e) {
            log.info("万能解析未命中，回退商品详情转链: {}", e.getMessage());
            return null;
        }
        String shortUrl = firstString(data, "shortUrl", "cpsLongUrl");
        String tpwd = firstString(data, "shortTpwd");
        if (StringUtils.isEmpty(shortUrl) && StringUtils.isEmpty(tpwd)) {
            return null;
        }
        return buildFromParse(data);
    }

    /**
     * 万能解析结果 → 统一返回结构
     */
    private TbkConvertResult buildFromParse(JSONObject data) {
        JSONObject origin = data.getJSONObject("originInfo");
        String itemId = firstString(data, "itemId", "goodsId");

        TbkConvertResult result = new TbkConvertResult();
        result.setItemId(itemId);
        String title = origin == null ? null : firstString(origin, "title");
        result.setTitle(StringUtils.isNotEmpty(title) ? title : firstString(data, "itemName"));
        result.setPicUrl(firstString(data, "mainPic"));
        // 解析接口只给当前售价，拿不到券面额；原价能从分享链接的 price 参数兜底
        BigDecimal price = origin == null ? null : firstDecimal(origin, "price");
        BigDecimal originalPrice = parseUrlPrice(firstString(data, "originUrl"));
        if (originalPrice != null && price != null && originalPrice.compareTo(price) <= 0) {
            originalPrice = null;
        }
        result.setFinalPrice(price);
        result.setOriginalPrice(originalPrice);
        result.setCommissionRate(firstDecimal(data, "commissionRate"));
        result.setCommission(estimateCommission(price, result.getCommissionRate()));
        result.setShortUrl(firstString(data, "shortUrl", "cpsLongUrl"));
        result.setTpwd(firstString(data, "shortTpwd"));
        result.setItemUrl(isNumericId(itemId)
                ? itemDetailUrl(itemId) : firstString(data, "itemLink"));
        result.setDemo(false);
        result.setProvider("dataoke");

        if (StringUtils.isEmpty(result.getTpwd())) {
            result.setNotice("淘口令暂时不可用，可直接使用推广链接");
        }
        return result;
    }

    private boolean isNumericId(String value) {
        return StringUtils.isNotEmpty(value) && value.matches("\\d{6,20}");
    }

    /**
     * 从分享链接的 price 参数里取原价（e.tb.cn 落地页会带）
     */
    private BigDecimal parseUrlPrice(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("[?&]price=(\\d+(?:\\.\\d+)?)").matcher(url);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String itemDetailUrl(String itemId) {
        return "https://item.taobao.com/item.htm?id=" + itemId;
    }

    // ------------------------------------------------------------------ 商品标识

    /**
     * 把用户输入统一成「商品详情接口能吃的 id」：数字ID / 新商品ID
     */
    private String resolveGoodsKey(String material, String itemId) {
        if (StringUtils.isNotEmpty(itemId)) {
            return itemId;
        }
        if (StringUtils.isEmpty(material)) {
            throw new ServiceException("请先粘贴淘宝链接或商品ID");
        }
        String text = material.trim();
        if (text.matches("\\d{6,20}") || NEW_ITEM_ID.matcher(text).matches()) {
            return text;
        }
        if (text.startsWith("http")) {
            String id = TbkLinkParser.extractItemId(text);
            if (StringUtils.isNotEmpty(id)) {
                return id;
            }
            // 短链（m.tb.cn / e.tb.cn 等）里没有商品ID，跟随跳转、必要时读落地页 HTML 再取
            id = TbkShortLinkResolver.resolveItemId(text);
            if (StringUtils.isNotEmpty(id)) {
                return id;
            }
            throw new ServiceException("短链里没有解析到商品ID，请在手机淘宝打开商品后复制完整链接，或直接粘贴商品ID");
        }
        throw new ServiceException("大淘客通道暂不支持直接解析淘口令，请粘贴商品链接或商品ID");
    }

    // ------------------------------------------------------------------ HTTP

    /**
     * 调用大淘客接口，自动补公共参数和签名
     */
    private JSONObject call(String path, Map<String, String> params) {
        return call(path, params, "没有找到该商品的优惠信息，可能已下架或不是淘宝客商品");
    }

    /**
     * 调用大淘客接口，data 为空时抛指定文案（不同接口的“查不到”含义不同）
     */
    private JSONObject call(String path, Map<String, String> params, String emptyMessage) {
        TbkProperties.Dataoke dataoke = properties.getDataoke();
        String timer = String.valueOf(System.currentTimeMillis());
        String nonce = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        String sign = DigestUtil.md5Hex("appKey=" + dataoke.getAppKey()
                + "&timer=" + timer
                + "&nonce=" + nonce
                + "&key=" + dataoke.getAppSecret()).toUpperCase();

        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("appKey", dataoke.getAppKey());
        all.put("timer", timer);
        all.put("nonce", nonce);
        all.put("signRan", sign);

        StringBuilder url = new StringBuilder(dataoke.getBaseUrl()).append(path).append('?');
        boolean first = true;
        for (Map.Entry<String, String> entry : all.entrySet()) {
            if (StringUtils.isEmpty(entry.getValue())) {
                continue;
            }
            if (!first) {
                url.append('&');
            }
            url.append(entry.getKey()).append('=').append(enc(entry.getValue()));
            first = false;
        }

        String body;
        try {
            // 用 URI 对象，避免 RestTemplate 对已编码的 query 二次编码
            body = restTemplate.getForObject(URI.create(url.toString()), String.class);
        } catch (Exception e) {
            log.error("大淘客接口请求失败 {}: {}", path, e.getMessage(), e);
            throw new ServiceException("转链服务暂时不可用，请稍后重试");
        }
        if (StringUtils.isEmpty(body)) {
            throw new ServiceException("转链服务返回为空，请稍后重试");
        }
        log.info("大淘客接口响应 {}: {}", path, body.length() > 500 ? body.substring(0, 500) : body);

        JSONObject json;
        try {
            json = JSON.parseObject(body);
        } catch (Exception e) {
            throw new ServiceException("转链服务返回格式异常");
        }
        Integer code = json.getInteger("code");
        if (code == null || code != 0) {
            throw new ServiceException("转链失败：" + json.getString("msg"));
        }
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            throw new ServiceException(emptyMessage);
        }
        return data;
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ------------------------------------------------------------------ 取值

    private String firstString(JSONObject data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && StringUtils.isNotEmpty(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private BigDecimal firstDecimal(JSONObject data, String... keys) {
        String value = firstString(data, keys);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").replace("￥", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal estimateCommission(BigDecimal finalPrice, BigDecimal rate) {
        if (finalPrice == null || rate == null) {
            return null;
        }
        return finalPrice.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String appendNotice(String notice, String extra) {
        return StringUtils.isEmpty(notice) ? extra : notice + "；" + extra;
    }
}
