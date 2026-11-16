package com.hms.main.tbk.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkOrder;

/**
 * 大淘客「订单查询」通道
 *
 * 用途：淘宝联盟官方订单接口（taobao.tbk.order.details.get）是邀请制，应用未拿到权限包时
 * 一直返回 code=11；而大淘客开放平台的订单查询接口用同一个推广位账号即可查到联盟订单
 * （含官方转链产生的订单），因此订单同步默认走本通道，官方通道保留做兜底。
 *
 * 接口：GET {baseUrl}/tb-service/get-order-details
 * 限制：单次时间跨度不超过 3 小时（大促 20 分钟）；最多可查近 3 个月；只返回已付款订单。
 * 签名：signRan = MD5("appKey={appKey}&timer={timer}&nonce={nonce}&key={appSecret}") 转大写。
 *
 * 文档：https://www.dataoke.com/pmc/api-d.html?id=27
 *
 * add by CYQ 2026-11-10 淘宝订单数据源改为大淘客
 */
@Component
public class DataokeOrderClient {

    private static final Logger log = LoggerFactory.getLogger(DataokeOrderClient.class);

    /** 订单查询接口固定版本号 */
    private static final String ORDER_VERSION = "v1.0.0";

    /** 查询时间类型：1-创建时间 2-付款时间 3-结算时间 4-更新时间 */
    public static final String QUERY_BY_CREATE = "1";
    public static final String QUERY_BY_PAY = "2";
    public static final String QUERY_BY_SETTLE = "3";
    public static final String QUERY_BY_UPDATE = "4";

    @Autowired
    private TbkProperties properties;

    private final RestTemplate restTemplate;

    public DataokeOrderClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(20000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 拉取指定时间窗内的订单（自动翻页）
     *
     * @param start     开始时间
     * @param end       结束时间（与 start 跨度不超过 3 小时）
     * @param queryType 1-创建时间 2-付款时间 3-结算时间 4-更新时间
     * @param pageSize  单页条数（1~100）
     * @param maxPages  最大页数，防止异常翻页
     */
    public List<TbkOrder> orderList(Date start, Date end, String queryType, int pageSize, int maxPages) {
        TbkProperties.Dataoke dataoke = properties.getDataoke();
        if (StringUtils.isEmpty(dataoke.getAppKey()) || StringUtils.isEmpty(dataoke.getAppSecret())) {
            throw new ServiceException("尚未配置大淘客 appKey/appSecret，请先在 application.yml 的 tbk.dataoke 下填写");
        }
        List<TbkOrder> orders = new ArrayList<>();
        String positionIndex = null;
        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("version", ORDER_VERSION);
            params.put("startTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", start));
            params.put("endTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", end));
            params.put("queryType", StringUtils.isNotEmpty(queryType) ? queryType : QUERY_BY_PAY);
            params.put("pageSize", String.valueOf(pageSize));
            params.put("pageNo", String.valueOf(pageNo));
            params.put("orderScene", "1");
            if (StringUtils.isNotEmpty(positionIndex)) {
                params.put("positionIndex", positionIndex);
                // 向前/向后翻页必须带 jumpType，这里统一向后翻
                params.put("jumpType", "1");
            }

            JSONObject data = call("/tb-service/get-order-details", params);
            JSONArray list = extractOrderArray(data);
            if (list == null || list.isEmpty()) {
                break;
            }
            for (int i = 0; i < list.size(); i++) {
                TbkOrder order = toOrder(list.getJSONObject(i));
                if (order != null && StringUtils.isNotEmpty(order.getOrderId())) {
                    orders.add(order);
                }
            }
            // has_next 为 false 或本页不满，说明没有下一页
            if (!data.getBooleanValue("has_next") || list.size() < pageSize) {
                break;
            }
            positionIndex = data.getString("position_index");
        }
        return orders;
    }

    /**
     * 从响应里取订单数组：data.results.publisher_order_dto
     */
    private JSONArray extractOrderArray(JSONObject data) {
        JSONObject results = data == null ? null : data.getJSONObject("results");
        if (results == null) {
            return null;
        }
        Object node = results.get("publisher_order_dto");
        if (node instanceof JSONArray) {
            return (JSONArray) node;
        }
        if (node instanceof JSONObject) {
            JSONArray array = new JSONArray();
            array.add(node);
            return array;
        }
        return null;
    }

    /**
     * 调用大淘客接口（GET），自动补公共参数和签名
     */
    private JSONObject call(String path, Map<String, String> params) {
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
            log.error("大淘客订单查询失败 {}: {}", path, e.getMessage(), e);
            throw new ServiceException("大淘客订单查询服务暂时不可用，请稍后重试");
        }
        if (StringUtils.isEmpty(body)) {
            throw new ServiceException("大淘客订单查询返回为空，请稍后重试");
        }
        log.info("大淘客订单查询响应 length={} body={}", body.length(), body.length() > 500 ? body.substring(0, 500) : body);

        JSONObject json;
        try {
            json = JSON.parseObject(body);
        } catch (Exception e) {
            throw new ServiceException("大淘客订单查询返回格式异常");
        }
        if (json.getInteger("code") == null || json.getInteger("code") != 0) {
            throw new ServiceException("大淘客订单查询失败：" + safeMsg(json.getString("msg")));
        }
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            throw new ServiceException("大淘客订单查询返回数据为空");
        }
        // 大淘客把业务错误（时间跨度超限、权限、提量不足等）放在 data.code/data.sub_msg 里
        Integer bizCode = data.getInteger("code");
        if (bizCode != null && bizCode != 0) {
            String msg = StringUtils.isNotEmpty(data.getString("sub_msg"))
                    ? data.getString("sub_msg") : data.getString("msg");
            throw new ServiceException("大淘客订单查询失败：" + safeMsg(msg));
        }
        return data;
    }

    /**
     * 大淘客订单报文 → HMS 订单模型
     *
     * 字段对照：trade_id=子订单号、alipay_total_price=实付、pub_share_pre_fee=预估佣金、
     * pub_share_fee=结算佣金、income_rate=佣金比例、tk_status=订单状态、tk_paid_time=付款时间。
     */
    public static TbkOrder toOrder(JSONObject node) {
        if (node == null) {
            return null;
        }
        String status = node.getString("tk_status");
        BigDecimal total = decimal(node, "alipay_total_price", "item_price");
        BigDecimal estimated = decimal(node, "pub_share_pre_fee");
        BigDecimal settled = decimal(node, "pub_share_fee");
        // 未结算的订单 pub_share_fee 是 0.00，不能当成「结算佣金 0 元」，否则会顶掉预估佣金
        // 并让收益统计/返现状态误判，这里统一置空（只有已结算且金额大于 0 才保留）
        if (settled != null && (settled.compareTo(BigDecimal.ZERO) <= 0 || !"3".equals(status))) {
            settled = null;
        }

        TbkOrder order = new TbkOrder();
        order.setPlatform("taobao");
        order.setOrderId(node.getString("trade_id"));
        order.setItemId(node.getString("item_id"));
        order.setItemTitle(node.getString("item_title"));
        order.setItemImg(node.getString("item_img"));
        order.setShopName(firstString(node, "seller_shop_title", "seller_nick"));
        order.setPid(node.getString("adzone_id"));
        order.setStatus(status);
        order.setStatusDesc(statusDesc(status));
        // 大淘客没有联盟接口的 pay_price（结算金额）字段，实付金额统一取 alipay_total_price
        order.setTotalAmount(total);
        order.setPayAmount(total);
        order.setEstimatedCommission(estimated);
        order.setSettledCommission(settled);
        order.setCommissionRate(normalizeRate(decimal(node, "income_rate", "tk_total_rate", "total_commission_rate")));
        order.setPayTime(parseTime(firstString(node, "tk_paid_time", "tb_paid_time")));
        order.setSettleTime(parseTime(node.getString("tk_earning_time")));
        order.setRefundTag(node.getString("refund_tag"));
        order.setRawData(node.toJSONString());
        return order;
    }

    /**
     * 淘客订单状态码说明：11-拍下未付款 12-付款 13-关闭/失效 14-确认收货 3-结算成功
     *
     * 注意：大淘客与联盟官方订单接口的状态码一致，订单同步两个通道共用这一套映射。
     */
    public static String statusDesc(String status) {
        if (StringUtils.isEmpty(status)) {
            return null;
        }
        switch (status) {
            case "11":
                return "拍下未付款";
            case "12":
                return "已付款";
            case "13":
                return "已关闭/失效";
            case "14":
                return "已确认收货";
            case "3":
                return "已结算";
            default:
                return "状态" + status;
        }
    }

    private static String firstString(JSONObject node, String... keys) {
        for (String key : keys) {
            String value = node.getString(key);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private static BigDecimal decimal(JSONObject node, String... keys) {
        for (String key : keys) {
            String value = node.getString(key);
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            try {
                return new BigDecimal(value.replace(",", "").trim());
            } catch (NumberFormatException ignored) {
                // 继续尝试下一个字段
            }
        }
        return null;
    }

    /**
     * 佣金比例统一成百分比：正常是 0.45（=0.45%），个别口径会返回 45，超过 100 的按万分数处理
     */
    private static BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        if (rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            return rate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return rate;
    }

    private static Date parseTime(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return DateUtils.dateTime("yyyy-MM-dd HH:mm:ss", value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeMsg(String msg) {
        return StringUtils.isNotEmpty(msg) ? msg : "未知错误";
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
