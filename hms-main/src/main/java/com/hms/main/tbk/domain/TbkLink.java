package com.hms.main.tbk.domain;

import java.util.Date;
import lombok.Data;

/**
 * 推广链接台账
 *
 * 每次生成推广链接都记一条，订单同步时按「平台 + 商品 + 时间窗」做近似归属。
 *
 * add by CYQ 2026-11-09 链接台账与返现估算
 */
@Data
public class TbkLink {

    private Long id;

    /** HMS 用户ID */
    private Long userId;

    /** 匿名标识 */
    private String anonKey;

    /** 平台：taobao / pdd */
    private String platform;

    /** 商品ID */
    private String itemId;

    /** 拼多多 goods_sign */
    private String goodsSign;

    /** 推广位 */
    private String pid;

    /** 商品标题 */
    private String title;

    /** 推广链接 */
    private String shortUrl;

    /** 淘口令 */
    private String tpwd;

    /** 来源：convert / pick */
    private String source;

    /** 生成时间 */
    private Date createTime;
}
