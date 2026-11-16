package com.hms.main.tbk.domain;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * AI 选品结果
 *
 * add by CYQ 2026-11-04 优惠查询-AI 选品
 */
@Data
public class TbkPickResult {

    /** 用户原始需求 */
    private String input;

    /** AI 解析出的平台搜索词 */
    private String keyword;

    /** 预算上限（元） */
    private BigDecimal budget;

    /** AI 解析出的偏好点 */
    private List<String> preferences;

    /** AI 解析出的商品大类 */
    private String category;

    /** AI 一句话结论 */
    private String summary;

    /** AI 购买建议 */
    private String advice;

    /** 最值得买的是第几个（1 开始，对应 items 顺序） */
    private Integer bestIndex;

    /** 候选商品（按性价比分降序） */
    private List<TbkPickItem> items;

    /** 是否用上了 AI 打分（false 表示规则兜底） */
    private boolean aiUsed;

    /** 搜索过程中的降级提示（某平台失败 / 预算内商品不足等） */
    private List<String> warnings;

    /** 固定提示 */
    private String notice;

    /** 总耗时（毫秒） */
    private long costMs;
}
