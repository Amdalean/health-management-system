package com.hms.main.tbk.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * AI 选品请求
 *
 * 两种用法：
 * - 选品：传 content（自然语言需求），可选 budget / maxItems；
 * - 候选转链：传 platform + itemId + goodsSign + title（由选品结果原样回传）。
 *
 * add by CYQ 2026-11-04 优惠查询-AI 选品
 */
@Data
public class TbkPickRequest {

    /** 自然语言需求，如「200元以内的降噪蓝牙耳机，通勤用」 */
    private String content;

    /** 匿名用户标识（登录态下忽略，后端从 token 解析用户ID） */
    private String userKey;

    /** 预算上限（元），用户显式填写时优先于 AI 解析结果 */
    private BigDecimal budget;

    /** 返回条数上限，默认取配置值 */
    private Integer maxItems;

    /** 候选转链：平台（taobao / pdd） */
    private String platform;

    /** 候选转链：商品ID */
    private String itemId;

    /** 候选转链：拼多多 goods_sign */
    private String goodsSign;

    /** 候选转链：商品标题（淘宝按标题精确搜索需要） */
    private String title;

    /** 候选转链：商品原始链接 */
    private String itemUrl;
}
