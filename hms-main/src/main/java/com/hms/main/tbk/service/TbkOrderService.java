package com.hms.main.tbk.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.client.DataokeOrderClient;
import com.hms.main.tbk.client.PddTbkClient;
import com.hms.main.tbk.client.TopApiException;
import com.hms.main.tbk.client.TopClient;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkOrder;
import com.hms.main.tbk.mapper.TbkOrderMapper;

/**
 * 联盟订单同步服务（淘宝客 + 多多进宝）
 *
 * 淘宝客订单查询是邀请制接口，未开通时只记录提示，不影响拼多多订单同步。
 * 淘宝客查询时间段要求不超过 3 小时，这里统一按 2 小时切片。
 *
 * add by CYQ 2026-11-09 联盟收益 PC 看板
 */
@Service
public class TbkOrderService {

    private static final Logger log = LoggerFactory.getLogger(TbkOrderService.class);

    private static final String PLATFORM_TAOBAO = "taobao";
    private static final String PLATFORM_PDD = "pdd";

    @Autowired
    private TbkOrderMapper orderMapper;

    @Autowired
    private TbkProperties properties;

    @Autowired
    private TopClient topClient;

    @Autowired
    private PddTbkClient pddClient;

    @Autowired
    private TbkLinkService linkService;

    @Autowired
    private DataokeOrderClient dataokeOrderClient;

    // ------------------------------------------------------------------ 查询

    public List<TbkOrder> selectTbkOrderList(TbkOrder query) {
        return orderMapper.selectTbkOrderList(query);
    }

    /**
     * 收益汇总：今日/本月/累计 + 平台拆分
     */
    public Map<String, Object> selectStats(TbkOrder query) {
        Map<String, Object> stats = orderMapper.selectTbkOrderStats(query);
        if (stats == null) {
            stats = new LinkedHashMap<>();
        }
        stats.put("platforms", orderMapper.selectPlatformStats(query));
        return stats;
    }

    // ------------------------------------------------------------------ 同步

    /**
     * 定时任务：回看最近一段时间，覆盖订单状态变化
     *
     * 按「更新时间」查询，订单付款→收货→结算的状态变化才能被回看窗口覆盖。
     */
    public Map<String, Object> syncRecent() {
        int minutes = properties.getOrderSyncLookbackMinutes() > 0
                ? properties.getOrderSyncLookbackMinutes() : 170;
        long endMs = System.currentTimeMillis();
        long startMs = endMs - minutes * 60_000L;
        return syncRange(new Date(startMs), new Date(endMs), DataokeOrderClient.QUERY_BY_UPDATE);
    }

    /**
     * 手动补同步最近 N 天（按平台各自切片）
     *
     * 按「付款时间」查询，用于补齐历史漏单。
     */
    public Map<String, Object> syncDays(int days) {
        int maxDays = properties.getOrderSyncMaxDays() > 0 ? properties.getOrderSyncMaxDays() : 7;
        int safeDays = Math.min(Math.max(days, 1), maxDays);
        long endMs = System.currentTimeMillis();
        long startMs = endMs - safeDays * 86_400_000L;
        return syncRange(new Date(startMs), new Date(endMs), DataokeOrderClient.QUERY_BY_PAY);
    }

    /**
     * 按时间范围同步，内部按平台分别切片
     */
    private Map<String, Object> syncRange(Date start, Date end, String taobaoQueryType) {
        Map<String, Object> total = emptyStat();
        total.put("startTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", start));
        total.put("endTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", end));
        total.put("taobaoSource", isDataokeOrderSource() ? "dataoke" : "taobao");
        syncTaobaoRange(start, end, taobaoQueryType, platformStat(total, "taobao"));
        syncPddRange(start, end, platformStat(total, "pdd"));
        return total;
    }

    /**
     * 淘宝订单同步入口：按配置选择数据源
     *
     * dataoke（默认）：大淘客订单查询，无需联盟订单接口权限，官方转链产生的订单也能查到；
     * taobao：淘宝联盟官方接口（邀请制，权限包未开通时返回 code=11）。
     *
     * add by CYQ 2026-11-10 淘宝订单数据源改为大淘客
     */
    private void syncTaobaoRange(Date start, Date end, String queryType, Map<String, Object> stat) {
        if (isDataokeOrderSource()) {
            syncTaobaoViaDataoke(start, end, queryType, stat);
        } else {
            syncTaobaoByOfficial(start, end, queryType, stat);
        }
    }

    /**
     * 大淘客订单通道：按 3 小时切片（接口限制单次不超过 3 小时），报错后不再尝试后续窗口
     */
    private void syncTaobaoViaDataoke(Date start, Date end, String queryType, Map<String, Object> stat) {
        int chunkMinutes = properties.getOrderSyncDataokeChunkMinutes() > 0
                ? properties.getOrderSyncDataokeChunkMinutes() : 180;
        int pageSize = properties.getOrderSyncPageSize() > 0 ? properties.getOrderSyncPageSize() : 100;
        int maxPages = properties.getOrderSyncMaxPages() > 0 ? properties.getOrderSyncMaxPages() : 50;
        long chunkMs = chunkMinutes * 60_000L;
        long cursor = start.getTime();
        long endMs = end.getTime();
        while (cursor < endMs && stat.get("error") == null) {
            long to = Math.min(cursor + chunkMs, endMs);
            List<TbkOrder> list;
            try {
                list = dataokeOrderClient.orderList(new Date(cursor), new Date(to),
                        queryType, pageSize, maxPages);
            } catch (ServiceException e) {
                stat.put("error", e.getMessage());
                return;
            } catch (Exception e) {
                log.warn("大淘客订单同步异常: {}", e.getMessage());
                stat.put("error", "大淘客订单同步异常：" + e.getMessage());
                return;
            }
            if (list != null && !list.isEmpty()) {
                add(stat, "fetched", list.size());
                for (TbkOrder order : list) {
                    linkService.attribute(order);
                    if (save(order)) {
                        add(stat, "saved", 1);
                    }
                }
            }
            cursor = to;
        }
    }

    /**
     * 淘宝联盟官方通道：按 2 小时切片（接口限制单次不超过 3 小时），报错后不再尝试后续窗口
     */
    private void syncTaobaoByOfficial(Date start, Date end, String queryType, Map<String, Object> stat) {
        int chunkMinutes = properties.getOrderSyncChunkMinutes() > 0
                ? properties.getOrderSyncChunkMinutes() : 120;
        long chunkMs = chunkMinutes * 60_000L;
        long cursor = start.getTime();
        long endMs = end.getTime();
        while (cursor < endMs && stat.get("error") == null) {
            long to = Math.min(cursor + chunkMs, endMs);
            syncTaobao(new Date(cursor), new Date(to), queryType, stat);
            cursor = to;
        }
    }

    /**
     * 淘宝订单数据源：默认大淘客；配置 tbk.order-sync-taobao-provider=taobao 时走联盟官方接口
     */
    private boolean isDataokeOrderSource() {
        return !"taobao".equalsIgnoreCase(properties.getOrderSyncTaobaoProvider());
    }

    /**
     * 拼多多按 23 小时切片（接口限制单次不超过 24 小时），报错后不再尝试后续窗口
     */
    private void syncPddRange(Date start, Date end, Map<String, Object> stat) {
        int chunkMinutes = properties.getOrderSyncPddChunkMinutes() > 0
                ? properties.getOrderSyncPddChunkMinutes() : 1380;
        long chunkMs = chunkMinutes * 60_000L;
        long cursor = start.getTime();
        long endMs = end.getTime();
        while (cursor < endMs && stat.get("error") == null) {
            long to = Math.min(cursor + chunkMs, endMs);
            syncPdd(new Date(cursor), new Date(to), stat);
            cursor = to;
        }
    }

    // ------------------------------------------------------------------ 淘宝客

    private void syncTaobao(Date start, Date end, String queryType, Map<String, Object> stat) {
        TbkProperties.Taobao taobao = properties.getTaobao();
        if (StringUtils.isEmpty(taobao.getAppKey()) || StringUtils.isEmpty(taobao.getAppSecret())) {
            stat.put("error", "未配置淘宝开放平台 appKey/appSecret");
            return;
        }
        int pageSize = properties.getOrderSyncPageSize() > 0 ? properties.getOrderSyncPageSize() : 100;
        int maxPages = properties.getOrderSyncMaxPages() > 0 ? properties.getOrderSyncMaxPages() : 50;

        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            Map<String, String> biz = new LinkedHashMap<>();
            biz.put("start_time", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", start));
            biz.put("end_time", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", end));
            // 1-创建时间 2-付款时间 3-结算时间 4-更新时间
            biz.put("query_type", StringUtils.isNotEmpty(queryType) ? queryType : "2");
            biz.put("page_no", String.valueOf(pageNo));
            biz.put("page_size", String.valueOf(pageSize));

            JSONObject resp;
            try {
                resp = topClient.execute("taobao.tbk.order.details.get", biz, null);
            } catch (TopApiException e) {
                if ("11".equals(e.getCode())) {
                    stat.put("error", "淘宝客订单查询权限未开通（该接口为邀请制，需要推广量达标）");
                } else {
                    stat.put("error", StringUtils.isNotEmpty(e.getMessage()) ? e.getMessage() : "淘宝订单查询失败");
                }
                return;
            } catch (Exception e) {
                log.warn("淘宝订单同步异常: {}", e.getMessage());
                stat.put("error", "淘宝订单同步异常：" + e.getMessage());
                return;
            }

            JSONArray list = extractTaobaoOrders(resp);
            if (list == null || list.isEmpty()) {
                return;
            }
            add(stat, "fetched", list.size());
            for (int i = 0; i < list.size(); i++) {
                TbkOrder order = buildTaobaoOrder(list.getJSONObject(i));
                if (order == null || StringUtils.isEmpty(order.getOrderId())) {
                    continue;
                }
                linkService.attribute(order);
                if (save(order)) {
                    add(stat, "saved", 1);
                }
            }
            if (list.size() < pageSize) {
                return;
            }
        }
    }

    private JSONArray extractTaobaoOrders(JSONObject resp) {
        JSONObject data = resp == null ? null : resp.getJSONObject("data");
        JSONObject results = data == null ? null : data.getJSONObject("results");
        if (results == null) {
            return null;
        }
        Object node = results.get("tbk_order");
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

    private TbkOrder buildTaobaoOrder(JSONObject node) {
        if (node == null) {
            return null;
        }
        String status = node.getString("tk_status");
        BigDecimal estimated = decimal(node, "pub_share_pre_fee");
        BigDecimal settled = decimal(node, "pub_share_fee");
        // 未结算时 pub_share_fee 为 0.00，按 null 处理，避免结算佣金顶掉预估佣金
        if (settled != null && (settled.compareTo(BigDecimal.ZERO) <= 0 || !"3".equals(status))) {
            settled = null;
        }

        TbkOrder order = new TbkOrder();
        order.setPlatform(PLATFORM_TAOBAO);
        order.setOrderId(node.getString("trade_id"));
        order.setItemId(node.getString("num_iid"));
        order.setItemTitle(node.getString("item_title"));
        order.setItemImg(node.getString("item_img"));
        order.setShopName(node.getString("seller_nick"));
        order.setPid(node.getString("adzone_id"));
        order.setStatus(status);
        order.setStatusDesc(DataokeOrderClient.statusDesc(status));
        order.setTotalAmount(decimal(node, "alipay_total_price", "pay_price", "price"));
        order.setPayAmount(decimal(node, "pay_price", "alipay_total_price", "price"));
        order.setEstimatedCommission(estimated);
        order.setSettledCommission(settled);
        order.setCommissionRate(normalizeRate(decimal(node, "commission_rate")));
        order.setPayTime(parseTime(node.getString("tk_paid_time")));
        order.setSettleTime(parseTime(node.getString("tk_earning_time")));
        order.setRefundTag(node.getString("refund_tag"));
        order.setRawData(node.toJSONString());
        return order;
    }

    // ------------------------------------------------------------------ 拼多多

    private void syncPdd(Date start, Date end, Map<String, Object> stat) {
        if (!pddClient.isConfigured()) {
            stat.put("error", "未配置拼多多 client-id/client-secret");
            return;
        }
        int pageSize = properties.getOrderSyncPageSize() > 0 ? properties.getOrderSyncPageSize() : 100;
        int maxPages = properties.getOrderSyncMaxPages() > 0 ? properties.getOrderSyncMaxPages() : 50;
        long startSec = start.getTime() / 1000L;
        long endSec = end.getTime() / 1000L;

        for (int page = 1; page <= maxPages; page++) {
            List<TbkOrder> list;
            try {
                list = pddClient.orderList(startSec, endSec, page, pageSize);
            } catch (ServiceException e) {
                stat.put("error", e.getMessage());
                return;
            } catch (Exception e) {
                log.warn("拼多多订单同步异常: {}", e.getMessage());
                stat.put("error", "拼多多订单同步异常：" + e.getMessage());
                return;
            }
            if (list == null || list.isEmpty()) {
                return;
            }
            add(stat, "fetched", list.size());
            for (TbkOrder order : list) {
                if (StringUtils.isEmpty(order.getOrderId())) {
                    continue;
                }
                linkService.attribute(order);
                if (save(order)) {
                    add(stat, "saved", 1);
                }
            }
            if (list.size() < pageSize) {
                return;
            }
        }
    }

    // ------------------------------------------------------------------ 工具

    private boolean save(TbkOrder order) {
        try {
            orderMapper.upsertTbkOrder(order);
            return true;
        } catch (Exception e) {
            log.warn("写入订单失败 {} {}: {}", order.getPlatform(), order.getOrderId(), e.getMessage());
            return false;
        }
    }

    private Map<String, Object> emptyStat() {
        Map<String, Object> total = new LinkedHashMap<>();
        total.put("taobao", newPlatformStat());
        total.put("pdd", newPlatformStat());
        return total;
    }

    private Map<String, Object> newPlatformStat() {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("fetched", 0);
        stat.put("saved", 0);
        stat.put("error", null);
        return stat;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> platformStat(Map<String, Object> total, String platform) {
        Object stat = total.get(platform);
        return stat instanceof Map ? (Map<String, Object>) stat : newPlatformStat();
    }

    private void add(Map<String, Object> stat, String key, int delta) {
        stat.put(key, intValue(stat.get(key)) + delta);
    }

    private int intValue(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private BigDecimal decimal(JSONObject node, String... keys) {
        if (node == null) {
            return null;
        }
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
     * 淘宝佣金比例可能是 15.5 或 1550 两种口径，统一成百分比
     */
    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        if (rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            return rate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return rate;
    }

    private Date parseTime(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return DateUtils.dateTime("yyyy-MM-dd HH:mm:ss", value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
