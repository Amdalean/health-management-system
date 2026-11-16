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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkConvertResult;
import com.hms.main.tbk.domain.TbkPickItem;
import com.hms.main.tbk.service.TaobaoAuthService;

/**
 * 淘宝联盟官方通道
 *
 * 接口组合：
 * - taobao.tbk.dg.material.optional.upgrade 物料搜索升级版：按标题找商品，返回绑定自己推广位的 click_url（当前主通道）
 * - taobao.tbk.item.info.get     商品信息（必须传新商品ID，数字ID 会报 -4003）
 * - taobao.tbk.coupon.convert    单品券高效转链（无需 session，权限包未开通）
 * - taobao.tbk.privilege.get     单品券高效转链（需 OAuth session，权限包未开通）
 * - taobao.tbk.spread.get        长链转短链
 * - taobao.tbk.tpwd.create       淘口令
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Component
@ConditionalOnProperty(prefix = "tbk", name = "provider", havingValue = "taobao")
public class TbktbClient implements TbkAggregatorClient {

    private static final Logger log = LoggerFactory.getLogger(TbktbClient.class);

    private static final Pattern COUPON_AMOUNT_PATTERN = Pattern.compile("减\\s*(\\d+(?:\\.\\d+)?)\\s*元");

    @Autowired
    private TbkProperties properties;

    @Autowired
    private TopClient topClient;

    @Autowired
    private TaobaoAuthService authService;

    /**
     * 带标题的转链：优先用「物料搜索升级版」按标题找到商品，
     * 拿到绑定自己推广位的 click_url，再转短链 + 淘口令。
     *
     * 这样即使单品券高效转链（privilege.get）权限没开通、或商品只有新商品ID，也能转。
     *
     * add by CYQ 2026-11-03
     */
    @Override
    public TbkConvertResult convert(String material, String itemId, String title) {
        String adzoneId = properties.resolveAdzoneId();
        if (StringUtils.isNotEmpty(title) && StringUtils.isNotEmpty(adzoneId)) {
            try {
                TbkConvertResult result = convertByTitle(title, adzoneId);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("官方物料搜索转链失败，回退单品转链: {}", e.getMessage());
            }
        }
        // 官方接口没有"数字ID反查新商品ID"的能力，只有题目搜索和 privilege.get 两条路，
        // 没标题又拿不到新商品ID时直接给出可操作提示，避免报成权限问题
        if (StringUtils.isEmpty(title) && !isNewItemId(material)) {
            throw new ServiceException("官方通道需要完整的淘宝分享文案（含商品标题），请把「【淘宝】…「商品标题」…」整段一起粘贴进来");
        }
        try {
            return convert(material, itemId);
        } catch (ServiceException e) {
            // 标题搜索没匹配到 + privilege.get 没权限时，真实原因是商品不在淘客物料库
            if (StringUtils.isNotEmpty(title) && e.getMessage() != null
                    && e.getMessage().contains("单品券高效转链")) {
                throw new ServiceException("没有在淘宝客物料库找到该商品，可能未参加淘宝客推广（无佣金/优惠券），请换一个商品");
            }
            throw e;
        }
    }

    /**
     * 联盟新商品ID：数字ID以外的长串
     */
    private boolean isNewItemId(String value) {
        return StringUtils.isNotEmpty(value) && value.matches("^[0-9A-Za-z_-]{16,64}$");
    }

    @Override
    public TbkConvertResult convert(String material, String itemId) {
        String adzoneId = properties.resolveAdzoneId();
        String siteId = properties.resolveSiteId();
        if (StringUtils.isEmpty(adzoneId) || StringUtils.isEmpty(siteId)) {
            throw new ServiceException("尚未配置推广位 pid（tbk.pid）");
        }

        String resolvedItemId = StringUtils.isNotEmpty(itemId) ? itemId : resolveItemId(material);
        if (StringUtils.isEmpty(resolvedItemId)) {
            throw new ServiceException("无法识别商品ID，请粘贴淘宝商品详情页链接或商品ID");
        }

        // 商品信息拿不到不阻断主流程：部分商品已切换到"新商品ID"，数字ID 会返回 -4003
        JSONObject item = null;
        String itemWarn = null;
        try {
            item = getItemInfo(resolvedItemId);
        } catch (ServiceException e) {
            itemWarn = e.getMessage();
            log.warn("商品信息获取失败 itemId={}: {}", resolvedItemId, itemWarn);
        }

        JSONObject link;
        String linkApi;
        try {
            link = couponConvert(resolvedItemId, adzoneId, siteId);
            linkApi = "coupon.convert";
        } catch (TopApiException e) {
            if (!isPermissionDenied(e)) {
                throw mapError(e);
            }
            log.info("taobao.tbk.coupon.convert 无权限，改用 privilege.get 通道");
            link = privilegeGet(resolvedItemId, adzoneId, siteId);
            linkApi = "privilege.get";
        }

        String clickUrl = firstString(link, "coupon_share_url", "coupon_click_url", "click_url");
        if (StringUtils.isEmpty(clickUrl)) {
            throw new ServiceException("没有获取到推广链接，该商品可能不是淘宝客商品");
        }

        String title = item == null ? null : item.getString("title");
        String shortUrl = spread(clickUrl);

        TbkConvertResult result = new TbkConvertResult();
        result.setItemId(resolvedItemId);
        result.setTitle(title);
        result.setPicUrl(item == null ? null : item.getString("pict_url"));
        result.setItemUrl(item == null ? null : firstString(item, "item_url", "detail_url"));
        if (StringUtils.isEmpty(result.getItemUrl())) {
            result.setItemUrl("https://item.taobao.com/item.htm?id=" + resolvedItemId);
        }
        result.setCouponInfo(firstString(link, "coupon_info"));
        result.setCommissionRate(firstDecimal(link, "max_commission_rate"));
        if (result.getCommissionRate() == null && item != null) {
            result.setCommissionRate(firstDecimal(item, "commission_rate"));
        }
        fillPrice(result, item, link);
        result.setShortUrl(StringUtils.isNotEmpty(shortUrl) ? shortUrl : clickUrl);
        fillTpwd(result, clickUrl, title);
        result.setDemo(false);
        result.setProvider("taobao");

        if (StringUtils.isEmpty(result.getTpwd())) {
            result.setNotice("淘口令暂时不可用，请先复制推广链接（接口：" + linkApi + "）");
        }
        if (itemWarn != null) {
            result.setNotice((result.getNotice() == null ? "" : result.getNotice() + "；")
                    + "商品详情获取失败：" + itemWarn);
        }
        return result;
    }

    @Override
    public String providerName() {
        return "taobao";
    }

    // ------------------------------------------------------------------ 关键词搜索（AI 选品）

    /**
     * 按关键词搜索淘宝客商品，供 AI 选品比价使用
     *
     * 复用「物料搜索升级版」，返回的结果天然带绑定自己推广位的 click_url，
     * 不需要再逐个转链，避免选品一次触发十几次接口调用。
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    @Override
    public List<TbkPickItem> search(String keyword, int limit) {
        List<TbkPickItem> items = new ArrayList<>();
        String adzoneId = properties.resolveAdzoneId();
        if (StringUtils.isEmpty(keyword) || StringUtils.isEmpty(adzoneId)) {
            return items;
        }
        int pageSize = Math.min(Math.max(limit, 1), 100);
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("adzone_id", adzoneId);
        biz.put("q", keyword.length() > 60 ? keyword.substring(0, 60) : keyword);
        biz.put("page_size", String.valueOf(pageSize));
        biz.put("page_no", "1");
        biz.put("has_coupon", "false");
        biz.put("biz_scene_id", "1");

        JSONObject resp;
        try {
            resp = topClient.execute("taobao.tbk.dg.material.optional.upgrade", biz, null);
        } catch (TopApiException e) {
            if (isPermissionDenied(e)) {
                throw new ServiceException("淘宝物料搜索接口权限未开通，暂时无法参与比价");
            }
            throw mapError(e);
        }
        JSONObject resultList = resp.getJSONObject("result_list");
        JSONArray mapData = resultList == null ? null : resultList.getJSONArray("map_data");
        if (mapData == null || mapData.isEmpty()) {
            return items;
        }
        for (int i = 0; i < mapData.size(); i++) {
            TbkPickItem item = buildPickItem(mapData.getJSONObject(i));
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * 物料搜索结果 → 选品候选
     */
    private TbkPickItem buildPickItem(JSONObject node) {
        if (node == null) {
            return null;
        }
        JSONObject basic = node.getJSONObject("item_basic_info");
        JSONObject publish = node.getJSONObject("publish_info");
        JSONObject priceInfo = node.getJSONObject("price_promotion_info");

        String title = firstString(basic, "title", "short_title");
        if (StringUtils.isEmpty(title)) {
            title = node.getString("title");
        }
        String clickUrl = publish == null ? null : firstString(publish, "coupon_share_url", "click_url");
        if (StringUtils.isEmpty(clickUrl)) {
            clickUrl = firstString(node, "coupon_share_url", "click_url");
        }
        if (StringUtils.isNotEmpty(clickUrl) && clickUrl.startsWith("//")) {
            clickUrl = "https:" + clickUrl;
        }
        if (StringUtils.isEmpty(title)) {
            return null;
        }

        String itemId = node.getString("item_id");
        BigDecimal coupon = firstDecimal(node, "coupon_amount");
        String couponInfo = firstString(node, "coupon_info");
        // 升级版把券放在 price_promotion_info.final_promotion_path_list 里
        JSONObject promotion = firstPromotion(priceInfo);
        if (promotion != null) {
            if (coupon == null) {
                coupon = firstDecimal(promotion, "promotion_fee");
            }
            String desc = firstString(promotion, "promotion_desc");
            if (StringUtils.isNotEmpty(desc)) {
                couponInfo = desc;
            }
        }
        if (coupon == null) {
            coupon = parseCouponAmount(couponInfo);
        }
        if (coupon == null) {
            coupon = BigDecimal.ZERO;
        }

        BigDecimal zkPrice = firstDecimal(priceInfo, "zk_final_price");
        if (zkPrice == null) {
            zkPrice = firstDecimal(node, "zk_final_price");
        }
        BigDecimal promoPrice = firstDecimal(priceInfo, "final_promotion_price");
        BigDecimal reservePrice = firstDecimal(priceInfo, "reserve_price");
        if (reservePrice == null) {
            reservePrice = firstDecimal(node, "reserve_price");
        }
        // final_promotion_price 已是券后到手价，不能再减一次券
        BigDecimal basePrice;
        BigDecimal finalPrice;
        if (promoPrice != null) {
            finalPrice = promoPrice;
            basePrice = zkPrice != null ? zkPrice : reservePrice;
        } else {
            basePrice = zkPrice;
            finalPrice = basePrice == null ? null
                    : basePrice.subtract(coupon).max(BigDecimal.ONE);
        }

        TbkPickItem item = new TbkPickItem();
        item.setPlatform("taobao");
        item.setPlatformName("淘宝");
        item.setItemId(itemId);
        item.setTitle(title);
        item.setPicUrl(firstString(basic, "pict_url"));
        if (StringUtils.isEmpty(item.getPicUrl())) {
            item.setPicUrl(node.getString("pict_url"));
        }
        item.setCouponAmount(coupon.compareTo(BigDecimal.ZERO) > 0 ? coupon : null);
        item.setCouponInfo(couponInfo);
        item.setFinalPrice(finalPrice);
        // 划线价优先用券前价（zk_final_price），比 reserve_price 更接近用户实际比价基准
        if (basePrice != null && finalPrice != null && basePrice.compareTo(finalPrice) > 0) {
            item.setOriginalPrice(basePrice);
        } else if (reservePrice != null && finalPrice != null && reservePrice.compareTo(finalPrice) > 0) {
            item.setOriginalPrice(reservePrice);
        }
        item.setSales(firstLong(node, "volume"));
        if (item.getSales() == null) {
            // 升级版 volume 可能恒为 0，用年度销量/淘客30天推广量兜底
            item.setSales(parseVolText(firstString(basic, "annual_vol")));
        }
        if (item.getSales() == null) {
            item.setSales(parseVolText(firstString(basic, "tk_total_sales")));
        }
        item.setShopName(firstString(basic, "shop_title", "nick"));
        if (StringUtils.isEmpty(item.getShopName())) {
            item.setShopName(node.getString("shop_title"));
        }
        item.setCommissionRate(publish == null ? firstDecimal(node, "commission_rate")
                : firstDecimal(publish, "income_rate"));
        item.setShortUrl(clickUrl);
        item.setItemUrl(firstString(basic, "item_url"));
        if (StringUtils.isEmpty(item.getItemUrl()) && StringUtils.isNotEmpty(itemId)) {
            item.setItemUrl("https://item.taobao.com/item.htm?id=" + itemId);
        }
        return item;
    }

    private Long firstLong(JSONObject data, String... keys) {
        if (data == null) {
            return null;
        }
        for (String key : keys) {
            Long value = data.getLong(key);
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    /**
     * 「1万+」「500+」这类销量文案转数字
     */
    private Long parseVolText(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            double num = Double.parseDouble(matcher.group(1));
            if (text.contains("万")) {
                num *= 10000;
            }
            return (long) num;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 取第一条优惠信息（升级版把券放在 final_promotion_path_list 里，兼容单对象/数组两种结构）
     */
    private JSONObject firstPromotion(JSONObject priceInfo) {
        if (priceInfo == null) {
            return null;
        }
        JSONObject list = priceInfo.getJSONObject("final_promotion_path_list");
        if (list == null) {
            return null;
        }
        Object mapData = list.get("final_promotion_path_map_data");
        if (mapData instanceof JSONArray) {
            JSONArray array = (JSONArray) mapData;
            if (array.isEmpty()) {
                return null;
            }
            for (int i = 0; i < array.size(); i++) {
                JSONObject promotion = array.getJSONObject(i);
                String title = promotion.getString("promotion_title");
                if (StringUtils.isEmpty(title) || title.contains("券")) {
                    return promotion;
                }
            }
            return array.getJSONObject(0);
        }
        if (mapData instanceof JSONObject) {
            return (JSONObject) mapData;
        }
        return null;
    }

    // ------------------------------------------------------------------ 物料搜索转链

    /**
     * 用「物料搜索升级版」按标题找商品并转链
     *
     * 返回 null 表示没搜到完全匹配的商品，交给上层的单品转链回退。
     */
    private TbkConvertResult convertByTitle(String title, String adzoneId) {
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("adzone_id", adzoneId);
        biz.put("q", title.length() > 60 ? title.substring(0, 60) : title);
        biz.put("page_size", "10");
        biz.put("page_no", "1");
        biz.put("has_coupon", "false");
        biz.put("biz_scene_id", "1");

        JSONObject resp;
        try {
            resp = topClient.execute("taobao.tbk.dg.material.optional.upgrade", biz, null);
        } catch (TopApiException e) {
            log.info("物料搜索升级版不可用: code={} {}", e.getCode(), e.getMessage());
            return null;
        }
        JSONObject resultList = resp.getJSONObject("result_list");
        JSONArray items = resultList == null ? null : resultList.getJSONArray("map_data");
        if (items == null || items.isEmpty()) {
            return null;
        }

        String expect = normalizeTitle(title);
        JSONObject matched = null;
        for (int i = 0; i < items.size(); i++) {
            JSONObject candidate = items.getJSONObject(i);
            JSONObject basic = candidate.getJSONObject("item_basic_info");
            if (basic == null) {
                continue;
            }
            if (expect.equals(normalizeTitle(basic.getString("title")))
                    || expect.equals(normalizeTitle(basic.getString("short_title")))) {
                matched = candidate;
                break;
            }
        }
        if (matched == null) {
            log.info("物料搜索未找到完全匹配的商品: {}", title);
            return null;
        }

        JSONObject basic = matched.getJSONObject("item_basic_info");
        JSONObject publish = matched.getJSONObject("publish_info");
        JSONObject priceInfo = matched.getJSONObject("price_promotion_info");
        String clickUrl = publish == null ? null : firstString(publish, "click_url");
        if (StringUtils.isEmpty(clickUrl)) {
            return null;
        }
        if (clickUrl.startsWith("//")) {
            clickUrl = "https:" + clickUrl;
        }

        TbkConvertResult result = new TbkConvertResult();
        String newItemId = matched.getString("item_id");
        result.setItemId(newItemId);
        result.setTitle(basic.getString("title"));
        result.setPicUrl(basic.getString("pict_url"));

        BigDecimal reserve = priceInfo == null ? null : firstDecimal(priceInfo, "reserve_price");
        BigDecimal zkPrice = priceInfo == null ? null : firstDecimal(priceInfo, "zk_final_price");
        BigDecimal promoPrice = priceInfo == null ? null : firstDecimal(priceInfo, "final_promotion_price");
        BigDecimal finalPrice = promoPrice != null ? promoPrice : zkPrice;
        result.setOriginalPrice(reserve != null ? reserve : zkPrice);
        result.setFinalPrice(finalPrice);

        BigDecimal rate = firstDecimal(publish, "income_rate");
        result.setCommissionRate(rate);
        JSONObject income = publish.getJSONObject("income_info");
        if (income != null) {
            result.setCommission(firstDecimal(income, "commission_amount"));
        }
        if (result.getCommission() == null && finalPrice != null && rate != null) {
            result.setCommission(finalPrice.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        String shortUrl = spread(clickUrl);
        result.setShortUrl(StringUtils.isNotEmpty(shortUrl) ? shortUrl : clickUrl);
        fillTpwd(result, clickUrl, basic.getString("title"));
        result.setItemUrl(StringUtils.isNotEmpty(newItemId)
                ? "https://uland.taobao.com/item/edetail?id=" + newItemId : null);
        result.setDemo(false);
        result.setProvider("taobao");
        if (StringUtils.isEmpty(result.getTpwd())) {
            result.setNotice("淘口令暂时不可用，可直接使用推广链接");
        }
        return result;
    }

    /**
     * 标题归一化，避免空格/全角括号差异导致匹配不上
     */
    private String normalizeTitle(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        return value.replaceAll("[\\s\\u3000]+", "")
                .replace('【', '[').replace('】', ']')
                .toLowerCase();
    }

    // ------------------------------------------------------------------ 商品信息

    private JSONObject getItemInfo(String itemId) {
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("num_iids", itemId);
        biz.put("platform", "2");
        JSONObject resp;
        try {
            resp = topClient.execute("taobao.tbk.item.info.get", biz, null);
        } catch (TopApiException e) {
            if (isPermissionDenied(e)) {
                throw new ServiceException("商品信息接口权限未开通，请到淘宝联盟开放平台申请「物料信息查询」");
            }
            throw mapError(e);
        }
        JSONObject results = resp.getJSONObject("results");
        JSONArray items = results == null ? null : results.getJSONArray("n_tbk_item");
        if (items == null || items.isEmpty()) {
            throw new ServiceException("商品不存在或已下架，请换一个商品链接");
        }
        return items.getJSONObject(0);
    }

    // ------------------------------------------------------------------ 转链

    private JSONObject couponConvert(String itemId, String adzoneId, String siteId) {
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("item_id", itemId);
        biz.put("adzone_id", adzoneId);
        biz.put("site_id", siteId);
        biz.put("platform", "2");
        if (StringUtils.isNotEmpty(properties.getRelationId())) {
            biz.put("relation_id", properties.getRelationId());
        }
        JSONObject resp = topClient.execute("taobao.tbk.coupon.convert", biz, null);
        return resultNode(resp);
    }

    private JSONObject privilegeGet(String itemId, String adzoneId, String siteId) {
        String session = authService.getSession();
        if (StringUtils.isEmpty(session)) {
            throw new ServiceException("转链接口需要淘宝账号授权，请先在浏览器打开 /tbk/auth/start 完成授权");
        }
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("item_id", itemId);
        biz.put("adzone_id", adzoneId);
        biz.put("site_id", siteId);
        biz.put("platform", "2");
        if (StringUtils.isNotEmpty(properties.getRelationId())) {
            biz.put("relation_id", properties.getRelationId());
        }
        try {
            return resultNode(topClient.execute("taobao.tbk.privilege.get", biz, session));
        } catch (TopApiException e) {
            if (isSessionInvalid(e)) {
                log.info("淘宝 session 失效，尝试续期后重试");
                String newSession = authService.refreshToken();
                return resultNode(topClient.execute("taobao.tbk.privilege.get", biz, newSession));
            }
            if (isPermissionDenied(e)) {
                throw new ServiceException("转链接口权限未开通，请申请「单品券高效转链」权限包");
            }
            throw mapError(e);
        }
    }

    private JSONObject resultNode(JSONObject resp) {
        JSONObject result = resp.getJSONObject("result");
        return result == null ? resp : result;
    }

    // ------------------------------------------------------------------ 淘口令 / 短链

    /**
     * 生成淘口令，同时保留「口令 + m.tb.cn 短链 + 标题」整段文案。
     *
     * password_simple 是纯口令，复制后打开淘宝会自动识别并跳到该商品；
     * model 里额外带 m.tb.cn 短链，发给微信/QQ 好友时点链接也能唤起淘宝，两个都留着由前端决定用哪个。
     *
     * add by CYQ 2026-11-10 选品复制带淘口令
     */
    private void fillTpwd(TbkConvertResult result, String url, String text) {
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("url", url);
        if (StringUtils.isNotEmpty(text)) {
            biz.put("text", text.length() > 30 ? text.substring(0, 30) : text);
        }
        try {
            JSONObject resp = topClient.execute("taobao.tbk.tpwd.create", biz, null);
            JSONObject data = resp.getJSONObject("data");
            if (data == null) {
                return;
            }
            result.setTpwd(firstString(data, "password_simple"));
            result.setTpwdModel(firstString(data, "model"));
            if (StringUtils.isEmpty(result.getTpwd())) {
                result.setTpwd(result.getTpwdModel());
            }
        } catch (TopApiException e) {
            log.warn("淘口令生成失败（可忽略）: code={} {}", e.getCode(), e.getMessage());
        }
    }

    private String spread(String url) {
        JSONArray requests = new JSONArray();
        JSONObject req = new JSONObject();
        req.put("url", url);
        requests.add(req);

        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("requests", requests.toJSONString());
        try {
            JSONObject resp = topClient.execute("taobao.tbk.spread.get", biz, null);
            JSONObject results = resp.getJSONObject("results");
            JSONArray arr = results == null ? null : results.getJSONArray("tbk_spread");
            if (arr == null || arr.isEmpty()) {
                return null;
            }
            JSONObject first = arr.getJSONObject(0);
            return "OK".equalsIgnoreCase(first.getString("err_msg")) ? first.getString("content") : null;
        } catch (TopApiException e) {
            log.warn("长链转短链失败（可忽略）: {}", e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ 素材解析

    /**
     * 把用户粘贴的素材解析成商品ID
     */
    private String resolveItemId(String material) {
        if (StringUtils.isEmpty(material)) {
            return null;
        }
        String text = material.trim();
        if (text.matches("^\\d{6,20}$")) {
            return text;
        }
        if (text.startsWith("http")) {
            String id = TbkLinkParser.extractItemId(text);
            return StringUtils.isNotEmpty(id) ? id : TbkShortLinkResolver.resolveItemId(text);
        }
        if (text.startsWith("￥") || text.startsWith("$") || text.startsWith("¥")) {
            return resolveFromTpwd(text);
        }
        return null;
    }

    /**
     * 淘口令解析（需要「淘口令」权限包里的 taobao.tbk.tpwd.convert）
     */
    private String resolveFromTpwd(String tpwd) {
        String adzoneId = properties.resolveAdzoneId();
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("password_content", tpwd);
        biz.put("adzone_id", adzoneId);
        try {
            JSONObject resp = topClient.execute("taobao.tbk.tpwd.convert", biz, null);
            JSONObject data = resp.getJSONObject("data");
            return data == null ? null : data.getString("num_iid");
        } catch (TopApiException e) {
            if (isPermissionDenied(e)) {
                throw new ServiceException("淘口令解析权限未开通，请先粘贴商品链接，或申请「淘口令」权限包");
            }
            throw mapError(e);
        }
    }

    // ------------------------------------------------------------------ 价格与工具

    private void fillPrice(TbkConvertResult result, JSONObject item, JSONObject link) {
        BigDecimal zk = item == null ? null : firstDecimal(item, "zk_final_price");
        BigDecimal reserve = item == null ? null : firstDecimal(item, "reserve_price");
        BigDecimal coupon = firstDecimal(link, "coupon_amount");
        if (coupon == null) {
            coupon = parseCouponAmount(firstString(link, "coupon_info"));
        }
        if (coupon == null) {
            coupon = BigDecimal.ZERO;
        }
        BigDecimal finalPrice = zk == null ? null
                : zk.subtract(coupon).max(BigDecimal.ONE);
        BigDecimal original = (reserve != null && zk != null && reserve.compareTo(zk) > 0) ? reserve : zk;

        result.setCouponAmount(coupon);
        result.setFinalPrice(finalPrice);
        result.setOriginalPrice(original);

        if (finalPrice != null && result.getCommissionRate() != null) {
            // add by CYQ 2026-11-10 commission_rate/max_commission_rate 均为百分数
            // （0.75 表示 0.75%），不能再按 >1 猜小数，否则低佣金商品返利放大 100 倍
            BigDecimal commission = finalPrice.multiply(result.getCommissionRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            result.setCommission(commission);
        }
    }

    private BigDecimal parseCouponAmount(String couponInfo) {
        if (StringUtils.isEmpty(couponInfo)) {
            return null;
        }
        Matcher matcher = COUPON_AMOUNT_PATTERN.matcher(couponInfo);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isPermissionDenied(TopApiException e) {
        return "11".equals(e.getCode());
    }

    private boolean isSessionInvalid(TopApiException e) {
        return "26".equals(e.getCode()) || "27".equals(e.getCode());
    }

    private ServiceException mapError(TopApiException e) {
        if (isSessionInvalid(e)) {
            return new ServiceException("淘宝账号授权已失效，请在浏览器打开 /tbk/auth/start 重新授权");
        }
        if ("15".equals(e.getCode())) {
            return new ServiceException("淘宝接口返回异常：" + e.getMessage());
        }
        return new ServiceException(StringUtils.isNotEmpty(e.getMessage()) ? e.getMessage() : "淘宝接口调用失败");
    }

    private String firstString(JSONObject data, String... keys) {
        if (data == null) {
            return null;
        }
        for (String key : keys) {
            String value = data.getString(key);
            if (StringUtils.isNotEmpty(value)) {
                return value;
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
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
