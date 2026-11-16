package com.hms.main.tbk.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 联盟订单（淘宝客 / 多多进宝）
 *
 * 通过定时任务增量同步，按 平台 + 订单号 幂等 upsert。
 *
 * add by CYQ 2026-11-09 联盟收益 PC 看板
 */
@Data
public class TbkOrder {

    /** 主键 */
    private Long id;

    /** 平台：taobao / pdd */
    private String platform;

    /** 平台订单号 */
    private String orderId;

    /** 商品ID */
    private String itemId;

    /** 商品标题 */
    private String itemTitle;

    /** 商品主图 */
    private String itemImg;

    /** 店铺名称 */
    private String shopName;

    /** 推广位（淘宝 adzone_id / 拼多多 p_id） */
    private String pid;

    /** HMS 用户ID（登录态下由后端解析，用于后续返现） */
    private Long userId;

    /** 用户名称（关联 sys_user 查询，仅展示用） */
    private String userName;

    /** 平台侧归因标识（拼多多 custom_parameters.uid / 淘宝 special_id / 闪购 sid） */
    private String externalUser;

    /** 拼多多 goods_sign（链接匹配用） */
    private String goodsSign;

    /** 关联的链接台账ID */
    private Long linkId;

    /** 归因方式：deterministic/approx/ambiguous/none */
    private String matchType;

    /** 返现比例（占佣金百分比） */
    private BigDecimal cashbackRate;

    /** 应返现金额 */
    private BigDecimal cashbackAmount;

    /** 返现状态：estimated/settled/invalid */
    private String cashbackStatus;

    /** 平台订单状态码 */
    private String status;

    /** 平台订单状态说明 */
    private String statusDesc;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 预估佣金 */
    private BigDecimal estimatedCommission;

    /** 结算佣金 */
    private BigDecimal settledCommission;

    /** 佣金比例（百分比） */
    private BigDecimal commissionRate;

    /** 付款时间 */
    private Date payTime;

    /** 结算时间 */
    private Date settleTime;

    /** 退款标记：淘宝 refund_tag / 拼多多 order_status */
    private String refundTag;

    /** 平台原始报文 */
    private String rawData;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    // ------------------------------------------------------------------ 查询条件（非表字段）

    /** 下单开始时间 yyyy-MM-dd */
    private String beginTime;

    /** 下单结束时间 yyyy-MM-dd */
    private String endTime;

    /** 订单号/商品标题模糊搜索 */
    private String keyword;
}
