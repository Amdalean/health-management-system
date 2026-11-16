package com.hms.main.tbk.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkConvertResult;
import com.hms.main.tbk.domain.TbkLink;
import com.hms.main.tbk.domain.TbkOrder;
import com.hms.main.tbk.domain.TbkPickItem;
import com.hms.main.tbk.mapper.TbkLinkMapper;

/**
 * 推广链接台账与订单返现估算
 *
 * 现阶段不实际打款，只记录：
 * - 谁在什么时候为哪个商品生成了推广链接；
 * - 订单同步后按「平台 + 商品 + 时间窗」做近似归属；
 * - 按配置比例计算「应返现金额」，并标记归因方式/返现状态。
 *
 * 拼多多官方精确归因需要 pid + custom_parameters 逐组备案，未备案时只能近似匹配。
 *
 * add by CYQ 2026-11-09 链接台账与返现估算
 */
@Service
public class TbkLinkService {

    private static final Logger log = LoggerFactory.getLogger(TbkLinkService.class);

    @Autowired
    private TbkLinkMapper linkMapper;

    @Autowired
    private TbkProperties properties;

    /**
     * 记录一次转链（用户点了「生成口令/复制链接」）
     */
    public void saveConvertLink(TbkConvertResult result, String provider, String userKey, String source) {
        if (result == null || StringUtils.isEmpty(result.getShortUrl())) {
            return;
        }
        try {
            TbkLink link = new TbkLink();
            applyUser(link, userKey);
            link.setPlatform(normalizePlatform(provider));
            link.setItemId(result.getItemId());
            link.setGoodsSign(result.getGoodsSign());
            link.setPid("pdd".equals(link.getPlatform())
                    ? properties.getPdd().getPid() : properties.resolveAdzoneId());
            link.setTitle(result.getTitle());
            link.setShortUrl(result.getShortUrl());
            link.setTpwd(result.getTpwd());
            link.setSource(source);
            linkMapper.insertTbkLink(link);
        } catch (Exception e) {
            log.warn("记录转链台账失败（不影响转链）: {}", e.getMessage());
        }
    }

    /**
     * 记录 AI 选品批量生成的候选链接
     */
    public void savePickLinks(List<TbkPickItem> items, String userKey) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Set<String> saved = new LinkedHashSet<>();
        for (TbkPickItem item : items) {
            if (item == null || StringUtils.isEmpty(item.getShortUrl())) {
                continue;
            }
            String key = item.getPlatform() + ":" + item.getItemId() + ":" + item.getShortUrl();
            if (!saved.add(key)) {
                continue;
            }
            try {
                TbkLink link = new TbkLink();
                applyUser(link, userKey);
                link.setPlatform(item.getPlatform());
                link.setItemId(item.getItemId());
                link.setGoodsSign(item.getGoodsSign());
                link.setPid(item.getPlatform() != null && "pdd".equals(item.getPlatform())
                        ? properties.getPdd().getPid() : properties.resolveAdzoneId());
                link.setTitle(item.getTitle());
                link.setShortUrl(item.getShortUrl());
                link.setTpwd(item.getTpwd());
                link.setSource("pick");
                linkMapper.insertTbkLink(link);
            } catch (Exception e) {
                log.warn("记录选品链接台账失败（不影响选品）: {}", e.getMessage());
            }
        }
    }

    /**
     * 订单归因 + 计算应返现金额
     */
    public void attribute(TbkOrder order) {
        if (order == null) {
            return;
        }
        // 返现比例统一走 tbk.cashback-rate（转链/选品展示的预估返利用的是同一个配置）
        // modify by CYQ 2026-11-14 平台服务费，返现比例 90%
        BigDecimal rate = properties.currentCashbackRate();
        order.setCashbackRate(rate);

        if (order.getPayTime() == null) {
            // 未支付订单不参与归因/返现
            order.setMatchType("none");
            order.setCashbackAmount(BigDecimal.ZERO);
            order.setCashbackStatus("unpaid");
            return;
        }

        int windowDays = properties.getAttributionWindowDays() > 0
                ? properties.getAttributionWindowDays() : 7;
        List<TbkLink> candidates = null;
        try {
            candidates = linkMapper.selectCandidates(order, windowDays);
        } catch (Exception e) {
            log.warn("查询链接台账失败，订单按未归因处理 {}: {}", order.getOrderId(), e.getMessage());
        }

        if (order.getUserId() != null) {
            // 平台回传了确定性用户标识（拼多多 custom_parameters 备案后）
            order.setMatchType("deterministic");
            order.setLinkId(findLinkId(candidates, order.getUserId()));
        } else if (candidates == null || candidates.isEmpty()) {
            order.setMatchType("none");
        } else {
            Set<Long> userIds = new LinkedHashSet<>();
            for (TbkLink link : candidates) {
                if (link.getUserId() != null) {
                    userIds.add(link.getUserId());
                }
            }
            TbkLink latest = candidates.get(0);
            order.setLinkId(latest.getId());
            if (userIds.size() == 1) {
                order.setUserId(latest.getUserId());
                order.setMatchType("approx");
            } else if (userIds.size() > 1) {
                // 多个用户都为同一商品生成过链接，不能确定是谁买的
                order.setMatchType("ambiguous");
            } else {
                order.setMatchType("anon");
            }
        }

        // 只有「已结算且金额大于 0」才用结算佣金，未结算订单（结算佣金为空或 0）按预估佣金算
        BigDecimal settled = order.getSettledCommission();
        boolean settledOrder = settled != null && settled.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal commission = settledOrder ? settled : order.getEstimatedCommission();
        if (commission == null) {
            commission = BigDecimal.ZERO;
        }
        BigDecimal cashback = commission.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (isRefund(order)) {
            order.setCashbackAmount(BigDecimal.ZERO);
            order.setCashbackStatus("invalid");
            return;
        }
        order.setCashbackAmount(cashback);
        if ("ambiguous".equals(order.getMatchType())) {
            order.setCashbackStatus("ambiguous");
        } else if (order.getUserId() == null) {
            order.setCashbackStatus("unattributed");
        } else if (settledOrder) {
            order.setCashbackStatus("settled");
        } else {
            order.setCashbackStatus("estimated");
        }
    }

    private Long findLinkId(List<TbkLink> candidates, Long userId) {
        if (candidates == null || userId == null) {
            return null;
        }
        for (TbkLink link : candidates) {
            if (userId.equals(link.getUserId())) {
                return link.getId();
            }
        }
        return null;
    }

    private boolean isRefund(TbkOrder order) {
        if ("taobao".equals(order.getPlatform())) {
            // 淘客订单 13=关闭/失效（已付款后全额退款会变成关闭），部分退款看 refund_tag=1
            return "1".equals(order.getRefundTag()) || "13".equals(order.getStatus());
        }
        // 拼多多：refund_tag 列存的是 order_status（1=已成团），不能当维权标记用，沿用原判断
        String desc = order.getStatusDesc();
        if (desc != null && desc.contains("退款")) {
            return true;
        }
        String status = order.getStatus();
        return "8".equals(status) || "14".equals(status);
    }

    private void applyUser(TbkLink link, String userKey) {
        if (StringUtils.isEmpty(userKey)) {
            return;
        }
        if (userKey.matches("\\d{1,20}")) {
            link.setUserId(Long.valueOf(userKey));
        } else {
            link.setAnonKey(userKey.length() > 64 ? userKey.substring(0, 64) : userKey);
        }
    }

    private String normalizePlatform(String provider) {
        if ("pdd".equals(provider)) {
            return "pdd";
        }
        if ("alsc".equals(provider)) {
            return "alsc";
        }
        return "taobao";
    }
}
