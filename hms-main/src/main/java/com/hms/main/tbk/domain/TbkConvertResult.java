package com.hms.main.tbk.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 转链结果
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Data
public class TbkConvertResult {

    /** 商品ID */
    private String itemId;

    /** 拼多多新商品标识（订单归因/匹配用） */
    private String goodsSign;

    /** 商品标题 */
    private String title;

    /** 商品主图 */
    private String picUrl;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 优惠券面额 */
    private BigDecimal couponAmount;

    /** 券后价 */
    private BigDecimal finalPrice;

    /** 佣金比例（百分比） */
    private BigDecimal commissionRate;

    /** 预估佣金 */
    private BigDecimal commission;

    /**
     * 返现比例（占佣金百分比，90 表示返 90%）
     * add by CYQ 2026-11-14 平台服务费，返利按 90% 展示
     */
    private BigDecimal cashbackRate;

    /**
     * 预估返利 = 预估佣金 × 返现比例
     * add by CYQ 2026-11-14 平台服务费，返利按 90% 展示
     */
    private BigDecimal cashbackAmount;

    /** 推广链接 */
    private String shortUrl;

    /** 淘口令（纯口令，如 ￥0Pon62KbzEx￥） */
    private String tpwd;

    /**
     * 淘口令完整文案（口令 + m.tb.cn 短链 + 标题），可直接复制发送
     *
     * add by CYQ 2026-11-10 选品复制带淘口令
     */
    private String tpwdModel;

    /** 商品原始链接 */
    private String itemUrl;

    /** 优惠券说明 */
    private String couponInfo;

    /** 是否演示数据 */
    private boolean demo;

    /** 提示信息（如演示模式说明） */
    private String notice;

    /** 转链通道 */
    private String provider;

    /** 可直接复制发送给用户的整段文案 */
    private String copyText;
}
