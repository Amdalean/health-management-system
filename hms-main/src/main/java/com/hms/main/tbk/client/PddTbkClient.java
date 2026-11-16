package com.hms.main.tbk.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkConvertResult;
import com.hms.main.tbk.domain.TbkOrder;
import com.hms.main.tbk.domain.TbkPickItem;
import com.hms.main.tbk.integration.PddDdkApiRequest;
import com.hms.main.tbk.integration.PddDdkApiStrategy;
import com.hms.main.tbk.integration.TbkApiSigner;

/**
 * 拼多多「多多进宝」转链通道
 *
 * 接口：
 * - pdd.ddk.goods.detail                    商品详情（标题、主图、拼团价、券、佣金比例）
 * - pdd.ddk.goods.promotion.url.generate    推广链接（必须传推广位 p_id）
 *
 * 签名：sign = MD5(client_secret + 按参数名排序拼接的 k1v1k2v2... + client_secret) 转大写。
 *
 * 注意：拼多多开放平台的应用必须先绑定多多进宝账号（duoId），否则所有 ddk 接口返回 20001。
 *
 * add by CYQ 2026-11-03 转链模块支持拼多多
 *
 * modify by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志：
 * 真正的公共参数组装、签名、HTTP 调用与集成日志已收敛到 {@link PddDdkApiStrategy}，
 * 本类只负责业务参数组装、响应解析与错误映射。
 */
@Component
public class PddTbkClient {

    private static final Logger log = LoggerFactory.getLogger(PddTbkClient.class);

    @Autowired
    private TbkProperties properties;

    @Autowired
    private PddDdkApiStrategy pddDdkApiStrategy;

    public String providerName() {
        return "pdd";
    }

    /**
     * 是否配置了拼多多应用
     */
    public boolean isConfigured() {
        TbkProperties.Pdd pdd = properties.getPdd();
        return StringUtils.isNotEmpty(pdd.getClientId()) && StringUtils.isNotEmpty(pdd.getClientSecret());
    }

    /**
     * 拼多多转链
     *
     * @param material 拼多多商品链接（短链/商品页/二合一都行）
     * @param title    分享文案里的商品标题，链接只有数字ID时用来搜索 goods_sign
     */
    public TbkConvertResult convert(String material, String title) {
        return convert(material, title, null);
    }

    /**
     * 拼多多转链（带用户归因）
     *
     * @param userKey HMS 用户ID 或匿名标识，会写入 custom_parameters.uid，
     *                订单同步时从 custom_parameters 反解出用户，供后续返现使用
     */
    public TbkConvertResult convert(String material, String title, String userKey) {
        TbkProperties.Pdd pdd = properties.getPdd();
        if (!isConfigured()) {
            throw new ServiceException("尚未配置拼多多应用，请填写 tbk.pdd.client-id / client-secret");
        }
        checkPid(pdd.getPid());

        PddLinkParser.PddGoodsRef ref = PddLinkParser.resolve(material);
        if (ref == null) {
            throw new ServiceException("链接里没有解析到拼多多商品ID，请打开商品页后复制完整链接");
        }
        // 数字 ID 链接：拼多多搜索接口支持直接把「终端用户的原始链接」当 keyword，能换出 goods_sign
        if (StringUtils.isEmpty(ref.getGoodsSign())) {
            PddLinkParser.PddGoodsRef byLink = resolveByOriginalLink(material, ref.getGoodsId(), pdd.getPid());
            if (byLink != null) {
                ref = new PddLinkParser.PddGoodsRef(ref.getGoodsId(), byLink.getGoodsSign());
            }
        }
        // 兜底 1：商品详情接口换 goods_sign
        if (StringUtils.isEmpty(ref.getGoodsSign()) && StringUtils.isNotEmpty(ref.getGoodsId())) {
            PddLinkParser.PddGoodsRef byDetail = resolveByGoodsDetail(ref.getGoodsId(), pdd.getPid());
            if (byDetail != null) {
                ref = byDetail;
            }
        }
        // 兜底 2：用分享文案里的标题搜索
        if (StringUtils.isEmpty(ref.getGoodsSign())) {
            PddLinkParser.PddGoodsRef byTitle = searchGoodsByTitle(title, pdd.getPid());
            if (byTitle != null) {
                ref = new PddLinkParser.PddGoodsRef(byTitle.getGoodsId(), byTitle.getGoodsSign());
            }
        }
        if (StringUtils.isEmpty(ref.getGoodsSign())) {
            throw new ServiceException("该链接只有数字商品ID，且缺少分享用户参数（share_uin），"
                    + "请粘贴拼多多 App 复制的完整分享链接，不要删掉后面的参数");
        }

        JSONObject detail = getDetail(ref);
        JSONObject link = generateLink(ref, pdd.getPid(), userKey);
        return buildResult(detail, link, ref);
    }

    /**
     * 兼容旧调用
     */
    public TbkConvertResult convert(String material) {
        return convert(material, null);
    }

    // ------------------------------------------------------------------ 订单增量查询（收益看板）

    /**
     * 多多进宝订单增量查询
     *
     * 接口：pdd.ddk.order.list.increment.get，按 update_time 增量拉取；
     * 金额单位为分，已统一换算成元。
     *
     * add by CYQ 2026-11-09 联盟收益 PC 看板
     *
     * @param startUpdateTime 开始更新时间（秒）
     * @param endUpdateTime   结束更新时间（秒）
     * @param page            页码，从 1 开始
     * @param pageSize        每页条数
     */
    public List<TbkOrder> orderList(long startUpdateTime, long endUpdateTime, int page, int pageSize) {
        List<TbkOrder> orders = new ArrayList<>();
        if (!isConfigured()) {
            throw new ServiceException("尚未配置拼多多应用，请填写 tbk.pdd.client-id / client-secret");
        }
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("start_update_time", String.valueOf(startUpdateTime));
        biz.put("end_update_time", String.valueOf(endUpdateTime));
        biz.put("page", String.valueOf(Math.max(page, 1)));
        biz.put("page_size", String.valueOf(Math.min(Math.max(pageSize, 1), 100)));
        biz.put("return_count", "true");

        JSONObject resp = call("pdd.ddk.order.list.increment.get", biz);
        JSONObject node = resp.getJSONObject("order_list_get_response");
        JSONArray list = node == null ? null : node.getJSONArray("order_list");
        if (list == null || list.isEmpty()) {
            return orders;
        }
        for (int i = 0; i < list.size(); i++) {
            TbkOrder order = buildOrder(list.getJSONObject(i));
            if (order != null && StringUtils.isNotEmpty(order.getOrderId())) {
                orders.add(order);
            }
        }
        return orders;
    }

    /**
     * 多多进宝订单 → 统一订单结构
     */
    private TbkOrder buildOrder(JSONObject node) {
        if (node == null) {
            return null;
        }
        String status = node.getString("order_status");
        BigDecimal estimated = yuan(node, "promotion_amount");
        BigDecimal settled = yuan(node, "settle_amount");
        if (settled == null && "10".equals(status)) {
            settled = estimated;
        }
        Long ratePerMille = node.getLong("promotion_rate");

        TbkOrder order = new TbkOrder();
        order.setPlatform("pdd");
        order.setOrderId(node.getString("order_sn"));
        order.setItemId(node.getString("goods_id"));
        order.setGoodsSign(node.getString("goods_sign"));
        order.setItemTitle(node.getString("goods_name"));
        order.setItemImg(firstString(node, "goods_thumbnail_url", "goods_image_url"));
        order.setShopName(node.getString("mall_name"));
        order.setPid(node.getString("p_id"));
        // custom_parameters.uid 是转链时写入的用户标识，用于返现归因
        String uid = parseCustomUid(node.getString("custom_parameters"));
        order.setExternalUser(uid);
        if (uid != null && uid.matches("\\d{1,20}")) {
            order.setUserId(Long.valueOf(uid));
        }
        order.setStatus(status);
        order.setStatusDesc(node.getString("order_status_desc"));
        order.setTotalAmount(yuan(node, "order_amount"));
        BigDecimal payAmount = yuan(node, "order_pay_amount");
        order.setPayAmount(payAmount != null ? payAmount : order.getTotalAmount());
        order.setEstimatedCommission(estimated);
        order.setSettledCommission(settled);
        order.setCommissionRate(ratePerMille == null ? null
                : BigDecimal.valueOf(ratePerMille).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP));
        order.setPayTime(unixDate(node.getLong("order_pay_time")));
        order.setSettleTime(unixDate(node.getLong("order_settle_time")));
        order.setRefundTag(status);
        order.setRawData(node.toJSONString());
        return order;
    }

    private java.util.Date unixDate(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        return new java.util.Date(seconds * 1000L);
    }

    /**
     * 解析订单里的 custom_parameters，取出 uid
     *
     * 拼多多可能原样返回 JSON，也可能做 URL 编码，这里都兼容。
     */
    private String parseCustomUid(String customParameters) {
        if (StringUtils.isEmpty(customParameters)) {
            return null;
        }
        String text = customParameters.trim();
        try {
            text = java.net.URLDecoder.decode(text, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            // 不是 URL 编码时按原样处理
        }
        try {
            JSONObject json = JSON.parseObject(text);
            String uid = json == null ? null : json.getString("uid");
            if (StringUtils.isNotEmpty(uid)) {
                return uid;
            }
        } catch (Exception ignored) {
            // 不是标准 JSON，走正则兜底
        }
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\"uid\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    // ------------------------------------------------------------------ 关键词搜索（AI 选品）

    /**
     * 按关键词搜索多多进宝商品，供 AI 选品比价使用
     *
     * 搜到候选后批量生成推广链接（一次请求覆盖 20 个 goods_sign），
     * 比逐个转链省调用次数，也能保证候选点开就是自己的推广链接。
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    public List<TbkPickItem> search(String keyword, int limit) {
        return search(keyword, limit, null);
    }

    /**
     * 按关键词搜索多多进宝商品（带用户归因）
     */
    public List<TbkPickItem> search(String keyword, int limit, String userKey) {
        List<TbkPickItem> items = new ArrayList<>();
        if (StringUtils.isEmpty(keyword) || !isConfigured()) {
            return items;
        }
        TbkProperties.Pdd pdd = properties.getPdd();
        checkPid(pdd.getPid());

        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("keyword", keyword.length() > 60 ? keyword.substring(0, 60) : keyword);
        biz.put("pid", pdd.getPid());
        biz.put("page", "1");
        biz.put("page_size", String.valueOf(Math.min(Math.max(limit, 1), 100)));

        JSONObject resp = call("pdd.ddk.goods.search", biz);
        JSONObject node = resp.getJSONObject("goods_search_response");
        JSONArray list = node == null ? null : node.getJSONArray("goods_list");
        if (list == null || list.isEmpty()) {
            return items;
        }
        for (int i = 0; i < list.size(); i++) {
            TbkPickItem item = buildPickItem(list.getJSONObject(i));
            if (item != null) {
                items.add(item);
            }
        }
        attachPromotionLinks(items, pdd.getPid(), node == null ? null : node.getString("search_id"), userKey);
        return items;
    }

    /**
     * goods_list 单条 → 选品候选
     */
    private TbkPickItem buildPickItem(JSONObject goods) {
        String title = goods.getString("goods_name");
        String goodsSign = goods.getString("goods_sign");
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(goodsSign)) {
            return null;
        }
        BigDecimal groupPrice = yuan(goods, "min_group_price");
        BigDecimal coupon = yuan(goods, "coupon_discount");
        if (coupon == null) {
            coupon = BigDecimal.ZERO;
        }
        BigDecimal finalPrice = groupPrice == null ? null
                : groupPrice.subtract(coupon).max(BigDecimal.ZERO);

        String goodsId = goods.getString("goods_id");
        TbkPickItem item = new TbkPickItem();
        item.setPlatform("pdd");
        item.setPlatformName("拼多多");
        item.setItemId(goodsId);
        item.setGoodsSign(goodsSign);
        item.setTitle(title);
        item.setPicUrl(firstString(goods, "goods_image_url", "goods_thumbnail_url"));
        // 没有券时用户就是按拼单价买，不展示假原价
        item.setOriginalPrice(coupon.compareTo(BigDecimal.ZERO) > 0 ? groupPrice : null);
        item.setCouponAmount(coupon.compareTo(BigDecimal.ZERO) > 0 ? coupon : null);
        item.setCouponInfo(couponInfo(goods, coupon));
        item.setFinalPrice(finalPrice);
        Long sales = goods.getLong("sold_quantity");
        if (sales == null) {
            sales = parseSalesTip(goods.getString("sales_tip"));
        }
        item.setSales(sales);
        item.setShopName(goods.getString("mall_name"));
        Long ratePerMille = goods.getLong("promotion_rate");
        item.setCommissionRate(ratePerMille == null ? null
                : BigDecimal.valueOf(ratePerMille).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP));
        item.setItemUrl(itemUrl(new PddLinkParser.PddGoodsRef(goodsId, goodsSign)));
        return item;
    }

    /**
     * 批量生成推广短链，失败不影响候选返回（前端可回退到商品原始链接）
     */
    private void attachPromotionLinks(List<TbkPickItem> items, String pid, String searchId, String userKey) {
        if (items.isEmpty()) {
            return;
        }
        Map<String, TbkPickItem> bySign = new LinkedHashMap<>();
        for (TbkPickItem item : items) {
            if (StringUtils.isNotEmpty(item.getGoodsSign())) {
                bySign.put(item.getGoodsSign(), item);
            }
        }
        if (bySign.isEmpty()) {
            return;
        }
        List<String> signs = new ArrayList<>(bySign.keySet());
        String customParameters = buildCustomParameters(userKey);
        boolean customRejected = false;
        try {
            // 单次最多 20 个 goods_sign，超出的分批
            for (int from = 0; from < signs.size(); from += 20) {
                List<String> chunk = signs.subList(from, Math.min(from + 20, signs.size()));
                Map<String, String> biz = new LinkedHashMap<>();
                biz.put("goods_sign_list", JSON.toJSONString(chunk));
                biz.put("p_id", pid);
                biz.put("generate_short_url", "true");
                biz.put("generate_we_app", "false");
                if (StringUtils.isNotEmpty(searchId)) {
                    // 带上 search_id 有助于提升收益归因
                    biz.put("search_id", searchId);
                }
                if (!customRejected && StringUtils.isNotEmpty(customParameters)) {
                    biz.put("custom_parameters", customParameters);
                }
                JSONObject resp;
                try {
                    resp = call("pdd.ddk.goods.promotion.url.generate", biz);
                } catch (ServiceException e) {
                    if (!customRejected && isCustomParamError(e.getMessage())) {
                        log.warn("拼多多自定义参数未授权，批量转链回退无归因: {}", e.getMessage());
                        customRejected = true;
                        biz.remove("custom_parameters");
                        resp = call("pdd.ddk.goods.promotion.url.generate", biz);
                    } else {
                        throw e;
                    }
                }
                JSONObject node = resp.getJSONObject("goods_promotion_url_generate_response");
                JSONArray urls = node == null ? null : node.getJSONArray("goods_promotion_url_list");
                if (urls == null) {
                    continue;
                }
                for (int i = 0; i < urls.size(); i++) {
                    JSONObject url = urls.getJSONObject(i);
                    TbkPickItem item = bySign.get(url.getString("goods_sign"));
                    if (item == null) {
                        continue;
                    }
                    item.setShortUrl(firstString(url, "short_url", "mobile_short_url", "url", "mobile_url"));
                }
            }
        } catch (Exception e) {
            log.warn("拼多多批量转链失败，回退商品原始链接: {}", e.getMessage());
        }
    }

    /**
     * 用「终端用户的原始分享链接」当 keyword 调拼多多搜索，换 goods_sign
     *
     * 拼多多搜索接口对原始链接做了特殊解析（只传数字 ID 会报 40009），
     * 传完整链接可以直接返回商品和 goods_sign，这是免费方案的关键。
     */
    private PddLinkParser.PddGoodsRef resolveByOriginalLink(String material, String goodsId, String pid) {
        if (StringUtils.isEmpty(material)) {
            return null;
        }
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("keyword", material);
        biz.put("pid", pid);
        biz.put("page", "1");
        biz.put("page_size", "10");
        JSONObject resp;
        try {
            resp = call("pdd.ddk.goods.search", biz);
        } catch (ServiceException e) {
            log.info("拼多多原始链接搜索失败: {}", e.getMessage());
            if (isPidError(e.getMessage())) {
                throw e;
            }
            return null;
        }
        JSONObject node = resp.getJSONObject("goods_search_response");
        JSONArray list = node == null ? null : node.getJSONArray("goods_list");
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (StringUtils.isNotEmpty(goodsId)) {
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);
                if (goodsId.equals(item.getString("goods_id"))) {
                    String sign = item.getString("goods_sign");
                    if (StringUtils.isNotEmpty(sign)) {
                        return new PddLinkParser.PddGoodsRef(goodsId, sign);
                    }
                }
            }
        }
        if (list.size() == 1) {
            String sign = list.getJSONObject(0).getString("goods_sign");
            if (StringUtils.isNotEmpty(sign)) {
                return new PddLinkParser.PddGoodsRef(list.getJSONObject(0).getString("goods_id"), sign);
            }
        }
        log.info("拼多多原始链接搜索到多个商品但未匹配 goods_id: {}", goodsId);
        return null;
    }

    /**
     * 用商品详情接口（pdd.goods.detail.get）把数字 goods_id 换成 goods_sign
     *
     * 该接口需要单独申请权限，未开通时返回 20031，静默失败由上层兜底。
     * 如果返回里没有 goods_sign，就用返回的商品标题去多多进宝搜索。
     */
    private PddLinkParser.PddGoodsRef resolveByGoodsDetail(String goodsId, String pid) {
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("goods_id", goodsId);
        JSONObject resp;
        try {
            resp = call("pdd.goods.detail.get", biz);
        } catch (ServiceException e) {
            log.info("拼多多商品详情接口不可用（可申请 pdd.goods.detail.get 权限）: {}", e.getMessage());
            return null;
        }
        String sign = findStringRecursively(resp, "goods_sign");
        if (StringUtils.isNotEmpty(sign)) {
            return new PddLinkParser.PddGoodsRef(goodsId, sign);
        }
        String title = findStringRecursively(resp, "goods_name");
        if (StringUtils.isEmpty(title)) {
            title = findStringRecursively(resp, "title");
        }
        if (StringUtils.isNotEmpty(title)) {
            return searchGoodsByTitle(title, pid);
        }
        return null;
    }

    /**
     * 在不确定层级的 JSON 里递归找字段
     */
    private String findStringRecursively(Object node, String key) {
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            for (String name : object.keySet()) {
                Object value = object.get(name);
                if (key.equals(name) && value != null && StringUtils.isNotEmpty(String.valueOf(value))) {
                    return String.valueOf(value);
                }
                String found = findStringRecursively(value, key);
                if (StringUtils.isNotEmpty(found)) {
                    return found;
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.size(); i++) {
                String found = findStringRecursively(array.get(i), key);
                if (StringUtils.isNotEmpty(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 按标题在多多进宝里搜商品，拿到 goods_sign
     */
    private PddLinkParser.PddGoodsRef searchGoodsByTitle(String title, String pid) {
        if (StringUtils.isEmpty(title)) {
            return null;
        }
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("keyword", title.length() > 60 ? title.substring(0, 60) : title);
        biz.put("pid", pid);
        biz.put("page", "1");
        biz.put("page_size", "10");
        JSONObject resp;
        try {
            resp = call("pdd.ddk.goods.search", biz);
        } catch (ServiceException e) {
            log.info("拼多多按标题搜索失败: {}", e.getMessage());
            if (isPidError(e.getMessage())) {
                throw e;
            }
            return null;
        }
        JSONObject node = resp.getJSONObject("goods_search_response");
        JSONArray list = node == null ? null : node.getJSONArray("goods_list");
        if (list == null || list.isEmpty()) {
            return null;
        }
        String expect = normalizeTitle(title);
        for (int i = 0; i < list.size(); i++) {
            JSONObject candidate = list.getJSONObject(i);
            if (expect.equals(normalizeTitle(candidate.getString("goods_name")))) {
                String sign = candidate.getString("goods_sign");
                if (StringUtils.isNotEmpty(sign)) {
                    return new PddLinkParser.PddGoodsRef(candidate.getString("goods_id"), sign);
                }
            }
        }
        log.info("拼多多按标题未找到完全匹配的商品: {}", title);
        return null;
    }

    private boolean isPidError(String message) {
        if (StringUtils.isEmpty(message)) {
            return false;
        }
        return message.contains("PID") || message.contains("pid") || message.contains("推广位");
    }

    /**
     * 校验推广位格式：必须是 媒体ID_推广位ID
     *
     * YAML 里不加引号时，REDACTED 会被解析成 44947695319016706。
     */
    private void checkPid(String pid) {
        if (StringUtils.isEmpty(pid)) {
            throw new ServiceException("尚未配置拼多多推广位 tbk.pdd.pid（多多进宝-推广管理-推广位ID）");
        }
        if (!pid.matches("\\d+_\\d+")) {
            throw new ServiceException("tbk.pdd.pid 格式不正确（当前值：" + pid + "），应为 123456_789012；"
                    + "YAML 配置里请给 pid 加引号，避免下划线被当成数字分隔符");
        }
    }

    private String normalizeTitle(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        return value.replaceAll("[\\s\\u3000]+", "")
                .replace('【', '[').replace('】', ']')
                .toLowerCase();
    }

    /**
     * 搜索接口有时只返回「已拼10万+件」这类文案，转成可比较的数字
     */
    private Long parseSalesTip(String tip) {
        if (StringUtils.isEmpty(tip)) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(tip);
        if (!matcher.find()) {
            return null;
        }
        try {
            double num = Double.parseDouble(matcher.group(1));
            if (tip.contains("万")) {
                num *= 10000;
            }
            return (long) num;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ 接口调用

    private JSONObject getDetail(PddLinkParser.PddGoodsRef ref) {
        if (StringUtils.isEmpty(ref.getGoodsSign())) {
            throw new ServiceException("该链接只有数字商品ID，拼多多已下线数字ID查询；请从拼多多 App 重新复制商品分享链接（新版链接会带 goods_sign）");
        }
        // 商品详情接口用的是单数 goods_sign
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("goods_sign", ref.getGoodsSign());
        JSONObject resp = call("pdd.ddk.goods.detail", biz);
        JSONObject node = resp.getJSONObject("goods_detail_response");
        JSONArray goods = node == null ? null : node.getJSONArray("goods_details");
        if (goods == null || goods.isEmpty()) {
            throw new ServiceException("拼多多商品不存在或已下架，请换一个商品链接");
        }
        return goods.getJSONObject(0);
    }

    private JSONObject generateLink(PddLinkParser.PddGoodsRef ref, String pid) {
        return generateLink(ref, pid, null);
    }

    private JSONObject generateLink(PddLinkParser.PddGoodsRef ref, String pid, String userKey) {
        if (StringUtils.isEmpty(ref.getGoodsSign())) {
            throw new ServiceException("该链接只有数字商品ID，拼多多已下线数字ID转链；请从拼多多 App 重新复制商品分享链接");
        }
        Map<String, String> biz = new LinkedHashMap<>();
        // 转链接口用的是 goods_sign_list
        biz.put("goods_sign_list", "[\"" + ref.getGoodsSign() + "\"]");
        biz.put("p_id", pid);
        biz.put("generate_short_url", "true");
        biz.put("generate_we_app", "false");
        String customParameters = buildCustomParameters(userKey);
        JSONObject resp = null;
        if (StringUtils.isNotEmpty(customParameters)) {
            biz.put("custom_parameters", customParameters);
            try {
                resp = call("pdd.ddk.goods.promotion.url.generate", biz);
            } catch (ServiceException e) {
                if (isCustomParamError(e.getMessage())) {
                    // 自定义参数未授权备案时不能把转链整个堵死，回退到无归因链接
                    log.warn("拼多多自定义参数未授权，回退无归因转链: {}", e.getMessage());
                    biz.remove("custom_parameters");
                    resp = call("pdd.ddk.goods.promotion.url.generate", biz);
                } else {
                    throw e;
                }
            }
        } else {
            resp = call("pdd.ddk.goods.promotion.url.generate", biz);
        }
        JSONObject node = resp.getJSONObject("goods_promotion_url_generate_response");
        JSONArray urls = node == null ? null : node.getJSONArray("goods_promotion_url_list");
        if (urls == null || urls.isEmpty()) {
            throw new ServiceException("没有获取到拼多多推广链接，该商品可能不支持推广");
        }
        return urls.getJSONObject(0);
    }

    /**
     * 是否自定义参数未授权（60001）
     */
    private boolean isCustomParamError(String message) {
        if (StringUtils.isEmpty(message)) {
            return false;
        }
        return message.contains("custom_parameters") || message.contains("授权备案") || message.contains("60001");
    }

    /**
     * 拼多多自定义参数：uid 用于用户归因（返现基础），未登录/未传时回退到全局配置
     */
    private String buildCustomParameters(String userKey) {
        if (!properties.getPdd().isUserAttributionEnabled()) {
            return properties.getPdd().getCustomParameters();
        }
        if (StringUtils.isNotEmpty(userKey)) {
            String uid = userKey.replace("\\", "").replace("\"", "").trim();
            if (uid.length() > 64) {
                uid = uid.substring(0, 64);
            }
            return "{\"uid\":\"" + uid + "\"}";
        }
        return properties.getPdd().getCustomParameters();
    }

    /**
     * 调用多多进宝接口
     *
     * 公共参数、签名、HTTP 调用与集成日志统一由 {@link PddDdkApiStrategy} 处理，
     * 这里只做响应解析和错误码到用户提示的映射。
     */
    private JSONObject call(String type, Map<String, String> biz) {
        String body;
        try {
            body = pddDdkApiStrategy.execute(new PddDdkApiRequest(type, biz));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("拼多多DDK接口调用失败 type={}: {}", type, e.getMessage());
            throw new ServiceException("拼多多转链服务暂时不可用，请稍后重试");
        }

        JSONObject root;
        try {
            root = JSON.parseObject(body);
        } catch (Exception e) {
            throw new ServiceException("拼多多接口返回格式异常");
        }
        if (root == null) {
            throw new ServiceException("拼多多接口返回为空，请稍后重试");
        }
        JSONObject error = root.getJSONObject("error_response");
        if (error != null) {
            throw mapError(error);
        }
        return root;
    }

    private ServiceException mapError(JSONObject error) {
        String subCode = error.getString("sub_code");
        if ("20001".equals(subCode)) {
            return new ServiceException("拼多多账号未绑定多多进宝：请用创建应用的拼多多账号登录 jinbao.pinduoduo.com，"
                    + "完成 duoId 与 clientId 绑定后再试");
        }
        if ("60001".equals(subCode)) {
            return new ServiceException("拼多多推广位未完成授权备案：请用手机打开授权链接完成一次备案"
                    + "（访问 /tbk/api/pdd/authorize 可生成授权链接）");
        }
        if ("40001".equals(subCode)) {
            return new ServiceException("该拼多多商品没有参加多多进宝推广（无佣金/无优惠券），无法生成推广链接，请换一个商品");
        }
        String msg = error.getString("sub_msg");
        if (StringUtils.isEmpty(msg)) {
            msg = error.getString("error_msg");
        }
        return new ServiceException(StringUtils.isNotEmpty(msg) ? msg : "拼多多接口调用失败");
    }

    // ------------------------------------------------------------------ 推广位授权备案

    /**
     * 当前生效的拼多多配置（用于排查线上 pid 配错）
     */
    public java.util.Map<String, Object> configInfo() {
        TbkProperties.Pdd pdd = properties.getPdd();
        java.util.Map<String, Object> info = new LinkedHashMap<>();
        info.put("configured", isConfigured());
        info.put("clientId", mask(pdd.getClientId()));
        info.put("pid", pdd.getPid());
        info.put("gateway", pdd.getGateway());
        return info;
    }

    /**
     * 查询当前多多进宝账号下有哪些推广位（pid）
     */
    public java.util.List<java.util.Map<String, Object>> listPids() {
        if (!isConfigured()) {
            throw new ServiceException("尚未配置拼多多 client-id / client-secret");
        }
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("page", "1");
        biz.put("page_size", "100");
        JSONObject resp = call("pdd.ddk.goods.pid.query", biz);
        JSONObject node = resp.getJSONObject("p_id_query_response");
        JSONArray list = node == null ? null : node.getJSONArray("p_id_list");
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        if (list == null) {
            return result;
        }
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            java.util.Map<String, Object> row = new LinkedHashMap<>();
            row.put("pid", item.getString("p_id"));
            row.put("name", item.getString("pid_name"));
            row.put("mediaId", item.getString("media_id"));
            row.put("status", item.get("status"));
            result.add(row);
        }
        return result;
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

    /**
     * 生成推广位授权备案链接（60001 时用手机打开一次即可）
     */
    public String buildAuthorityUrl() {
        TbkProperties.Pdd pdd = properties.getPdd();
        if (!isConfigured()) {
            throw new ServiceException("请先配置 tbk.pdd.client-id / client-secret");
        }
        checkPid(pdd.getPid());
        String goodsSign = firstGoodsSign(pdd.getPid());
        if (StringUtils.isEmpty(goodsSign)) {
            throw new ServiceException("获取拼多多商品失败，无法生成授权链接，请稍后重试");
        }
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("p_id", pdd.getPid());
        biz.put("goods_sign_list", "[\"" + goodsSign + "\"]");
        biz.put("generate_short_url", "true");
        biz.put("generate_authority_url", "true");
        JSONObject resp = call("pdd.ddk.goods.promotion.url.generate", biz);
        JSONObject node = resp.getJSONObject("goods_promotion_url_generate_response");
        JSONArray urls = node == null ? null : node.getJSONArray("goods_promotion_url_list");
        if (urls == null || urls.isEmpty()) {
            throw new ServiceException("生成拼多多授权链接失败，请稍后重试");
        }
        JSONObject first = urls.getJSONObject(0);
        String url = firstString(first, "short_url", "mobile_short_url", "url", "mobile_url");
        if (StringUtils.isEmpty(url)) {
            throw new ServiceException("生成拼多多授权链接失败，请稍后重试");
        }
        return url;
    }

    /**
     * 查推广位是否已完成授权备案
     */
    public boolean isAuthorityBound() {
        TbkProperties.Pdd pdd = properties.getPdd();
        if (!isConfigured()) {
            return false;
        }
        checkPid(pdd.getPid());
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("pid", pdd.getPid());
        JSONObject resp = call("pdd.ddk.member.authority.query", biz);
        JSONObject node = resp.getJSONObject("authority_query_response");
        return node != null && node.getIntValue("bind") == 1;
    }

    /**
     * 取一个商品 goods_sign 用来生成备案链接
     */
    private String firstGoodsSign(String pid) {
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("pid", pid);
        biz.put("page", "1");
        biz.put("page_size", "10");
        JSONObject resp = call("pdd.ddk.goods.recommend.get", biz);
        JSONObject node = resp.getJSONObject("goods_basic_detail_response");
        JSONArray list = node == null ? null : node.getJSONArray("list");
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getJSONObject(0).getString("goods_sign");
    }

    // ------------------------------------------------------------------ 结果组装

    private TbkConvertResult buildResult(JSONObject detail, JSONObject link, PddLinkParser.PddGoodsRef ref) {
        BigDecimal groupPrice = yuan(detail, "min_group_price");
        BigDecimal coupon = yuan(detail, "coupon_discount");
        if (coupon == null) {
            coupon = BigDecimal.ZERO;
        }

        BigDecimal finalPrice = groupPrice == null ? null
                : groupPrice.subtract(coupon).max(BigDecimal.ZERO);
        // 拼多多的 min_normal_price 是"单独购买价"，不是本单原价；
        // 没有券时用户就是按拼单价买，不能再算"原价→券后"的差价
        BigDecimal originalPrice = coupon.compareTo(BigDecimal.ZERO) > 0 ? groupPrice : null;

        Long ratePerMille = detail.getLong("promotion_rate");
        BigDecimal commissionRate = ratePerMille == null ? null
                : BigDecimal.valueOf(ratePerMille).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
        BigDecimal commission = (ratePerMille == null || finalPrice == null) ? null
                : finalPrice.multiply(BigDecimal.valueOf(ratePerMille))
                        .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        TbkConvertResult result = new TbkConvertResult();
        result.setItemId(StringUtils.isNotEmpty(ref.getGoodsId()) ? ref.getGoodsId() : ref.getGoodsSign());
        result.setGoodsSign(ref.getGoodsSign());
        result.setTitle(detail.getString("goods_name"));
        result.setPicUrl(firstString(detail, "goods_image_url", "goods_thumbnail_url"));
        result.setOriginalPrice(originalPrice);
        result.setFinalPrice(finalPrice);
        result.setCouponAmount(coupon.compareTo(BigDecimal.ZERO) > 0 ? coupon : null);
        result.setCommissionRate(commissionRate);
        result.setCommission(commission);
        result.setCouponInfo(couponInfo(detail, coupon));
        result.setShortUrl(firstString(link, "short_url", "url", "mobile_short_url", "mobile_url"));
        result.setItemUrl(itemUrl(ref));
        // 拼多多没有淘口令
        result.setTpwd(null);
        result.setDemo(false);
        result.setProvider("pdd");
        if (result.getCouponAmount() == null) {
            result.setNotice("该商品暂无优惠券，已生成推广链接");
        }
        return result;
    }

    private String couponInfo(JSONObject detail, BigDecimal coupon) {
        if (coupon == null || coupon.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal minOrder = yuan(detail, "coupon_min_order_amount");
        if (minOrder != null && minOrder.compareTo(BigDecimal.ZERO) > 0) {
            return "满" + minOrder.stripTrailingZeros().toPlainString()
                    + "减" + coupon.stripTrailingZeros().toPlainString();
        }
        return "减" + coupon.stripTrailingZeros().toPlainString() + "元";
    }

    private String itemUrl(PddLinkParser.PddGoodsRef ref) {
        if (StringUtils.isNotEmpty(ref.getGoodsId())) {
            return "https://mobile.yangkeduo.com/goods.html?goods_id=" + ref.getGoodsId();
        }
        return "https://mobile.yangkeduo.com/goods.html?goods_sign=" + ref.getGoodsSign();
    }

    /**
     * 分 → 元
     */
    private BigDecimal yuan(JSONObject data, String key) {
        Long cents = data.getLong(key);
        if (cents == null || cents <= 0) {
            return null;
        }
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String firstString(JSONObject data, String... keys) {
        for (String key : keys) {
            String value = data.getString(key);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 签名

    /**
     * 多多进宝签名：MD5(secret + 排序后的 k1v1k2v2... + secret) 转大写
     *
     * 保留静态方法兼容老调用与单元测试，实际实现已收敛到 {@link TbkApiSigner}。
     */
    public static String sign(Map<String, String> params, String secret) {
        return TbkApiSigner.sign(params, secret);
    }
}
