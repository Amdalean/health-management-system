package com.hms.main.tbk.client;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkConvertResult;

/**
 * 折淘客「高佣转链（淘口令）」通道
 *
 * 一个接口直接吃淘口令 / 长链 / 短链 / 二合一链接 / 喵口令，
 * signurl=5 时同时返回券后价、佣金、推广链接和淘口令。
 *
 * 文档：http://www.zhetaoke.com/user/open/open_gaoyongzhuanlian_tkl.aspx
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Component
@ConditionalOnProperty(prefix = "tbk", name = "provider", havingValue = "ztk")
public class ZtkTbkClient implements TbkAggregatorClient {

    private static final Logger log = LoggerFactory.getLogger(ZtkTbkClient.class);

    @Autowired
    private TbkProperties properties;

    private final RestTemplate restTemplate;

    public ZtkTbkClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public TbkConvertResult convert(String material, String itemId) {
        TbkProperties.Ztk ztk = properties.getZtk();
        String pid = StringUtils.isNotEmpty(ztk.getPid()) ? ztk.getPid() : properties.getPid();
        if (StringUtils.isEmpty(ztk.getAppkey()) || StringUtils.isEmpty(ztk.getSid())) {
            throw new ServiceException("尚未配置折淘客 appkey/sid，请先在 application.yml 的 tbk.ztk 下填写");
        }
        if (StringUtils.isEmpty(pid)) {
            throw new ServiceException("尚未配置推广位 pid，请填写 tbk.pid");
        }

        String url = ztk.getBaseUrl()
                + "?appkey=" + enc(ztk.getAppkey())
                + "&sid=" + enc(ztk.getSid())
                + "&pid=" + enc(pid)
                + "&tkl=" + enc(material)
                // 5 = 转链 + 解析商品编号 + 商品详情 + 淘口令创建，一步到位
                + "&signurl=5";
        if (StringUtils.isNotEmpty(properties.getRelationId())) {
            url += "&relation_id=" + enc(properties.getRelationId());
        }
        if (StringUtils.isNotEmpty(properties.getExternalId())) {
            url += "&external_id=" + enc(properties.getExternalId());
        }

        String body;
        try {
            // 用 URI 对象，避免 RestTemplate 对已编码的 query 二次编码
            body = restTemplate.getForObject(java.net.URI.create(url), String.class);
        } catch (Exception e) {
            log.error("折淘客转链请求失败: {}", e.getMessage(), e);
            throw new ServiceException("转链服务暂时不可用，请稍后重试");
        }
        if (StringUtils.isEmpty(body)) {
            throw new ServiceException("转链服务返回为空，请稍后重试");
        }
        log.info("折淘客转链响应: {}", body);

        JSONObject root;
        try {
            root = JSON.parseObject(body);
        } catch (Exception e) {
            throw new ServiceException("转链服务返回格式异常");
        }

        Integer status = root.getInteger("status");
        if (status != null && status != 200) {
            throw new ServiceException("转链失败：" + root.getString("content"));
        }

        // signurl=5 的 content 可能是数组，也可能是对象，做兼容处理
        JSONObject data = root;
        Object content = root.get("content");
        if (content instanceof JSONArray) {
            JSONArray array = (JSONArray) content;
            if (array.isEmpty()) {
                throw new ServiceException("没有找到该商品的优惠信息，可能已下架或不是淘宝客商品");
            }
            data = array.getJSONObject(0);
        } else if (content instanceof JSONObject) {
            data = (JSONObject) content;
        }

        TbkConvertResult result = new TbkConvertResult();
        result.setItemId(firstString(data, "tao_id", "item_id", "itemId", "num_iid", "goodsId"));
        result.setTitle(firstString(data, "tao_title", "title", "dtitle", "jianjie"));
        result.setPicUrl(firstString(data, "pict_url", "mainPic", "pic", "main_pic"));
        result.setOriginalPrice(firstDecimal(data, "size", "org_price", "original_price", "originalPrice"));
        result.setCouponAmount(firstDecimal(data, "coupon_info_money", "coupon_price", "couponPrice", "couponAmount"));
        result.setFinalPrice(firstDecimal(data, "quanhou_jiage", "actual_price", "actualPrice", "final_price"));
        result.setCommissionRate(firstDecimal(data, "tkrate3", "commission_rate", "commissionRate"));
        result.setCommission(firstDecimal(data, "tkfee3", "commission", "commissionAmount"));
        result.setShortUrl(firstString(data, "result_url", "coupon_click_url", "couponClickUrl", "click_url"));
        result.setTpwd(firstString(data, "result_tkl", "tpwd", "model"));
        result.setCouponInfo(firstString(data, "coupon_info", "couponInfo"));
        result.setItemUrl(result.getItemId() == null ? null
                : "https://item.taobao.com/item.htm?id=" + result.getItemId());
        result.setDemo(false);
        result.setProvider("ztk");

        if (StringUtils.isEmpty(result.getShortUrl()) && StringUtils.isEmpty(result.getTpwd())) {
            throw new ServiceException("没有找到该商品的优惠信息，可能已下架或不是淘宝客商品");
        }
        return result;
    }

    @Override
    public String providerName() {
        return "ztk";
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

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
}
