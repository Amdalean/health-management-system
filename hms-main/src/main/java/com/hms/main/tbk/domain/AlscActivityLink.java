package com.hms.main.tbk.domain;

import lombok.Data;

/**
 * 淘宝闪购活动推广链接
 *
 * add by CYQ 2026-11-05 接入淘宝闪购
 */
@Data
public class AlscActivityLink {

    /** 活动ID */
    private String activityId;

    /** 活动标题 */
    private String title;

    /** 活动描述 */
    private String description;

    /** 活动图片 */
    private String picture;

    /** 活动开始时间（秒） */
    private Long startTime;

    /** 活动结束时间（秒） */
    private Long endTime;

    /** H5 推广长链 */
    private String h5Url;

    /** H5 推广短链 */
    private String h5ShortLink;

    /** 微信小程序 appId */
    private String wxAppId;

    /** 微信小程序 path */
    private String wxPath;

    /** 微信推广二维码 */
    private String wxQrCode;

    /** 淘口令 */
    private String taobaoWord;

    /** 淘宝唤端链接 */
    private String taobaoScheme;

    /** 饿了么口令 / 闪购口令 */
    private String elemeWord;

    /** App deeplink */
    private String deepLink;
}
