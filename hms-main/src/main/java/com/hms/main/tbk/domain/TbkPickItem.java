package com.hms.main.tbk.domain;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 跨平台选品候选商品
 *
 * 同时承载平台原始信息（价格/券/销量/店铺）与 AI 评分结果。
 *
 * add by CYQ 2026-11-04 优惠查询-AI 选品
 */
@Data
public class TbkPickItem {

    /** 平台标识：taobao / pdd */
    private String platform;

    /** 平台名称：淘宝 / 拼多多 */
    private String platformName;

    /** 商品ID */
    private String itemId;

    /** 拼多多新商品标识（一键转链用，淘宝为空） */
    private String goodsSign;

    /** 商品标题 */
    private String title;

    /** 商品主图 */
    private String picUrl;

    /** 原价（无真实原价时为空） */
    private BigDecimal originalPrice;

    /** 优惠券面额 */
    private BigDecimal couponAmount;

    /** 券后价 */
    private BigDecimal finalPrice;

    /** 优惠券说明 */
    private String couponInfo;

    /** 销量（平台口径，可能为空） */
    private Long sales;

    /** 店铺名称 */
    private String shopName;

    /** 推广链接 */
    private String shortUrl;

    /** 淘口令（拼多多为空） */
    private String tpwd;

    /** 商品原始链接 */
    private String itemUrl;

    /** 佣金比例（百分比） */
    private BigDecimal commissionRate;

    /**
     * 返现比例（占佣金百分比，取自 tbk.cashback-rate）
     * add by CYQ 2026-11-14 平台服务费，返现比例 90%
     */
    private BigDecimal cashbackRate;

    /**
     * 预估返利（后端按返现比例算好，前端直接展示）
     * add by CYQ 2026-11-14 平台服务费，返现比例 90%
     */
    private BigDecimal cashbackAmount;

    /** AI 性价比分 0-100 */
    private Integer score;

    /** AI 推荐理由 */
    private String reason;

    /** AI 标签 */
    private List<String> tags;

    /** AI 风险提示 */
    private String caution;

    /** 规则兜底分（AI 不可用时展示） */
    private Integer ruleScore;
}
