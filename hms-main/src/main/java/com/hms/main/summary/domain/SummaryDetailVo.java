package com.hms.main.summary.domain;

import com.hms.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 财务汇总明细视图对象
 *
 * @author CYQ
 * @date 2025-02-05
 */
@Data
public class SummaryDetailVo extends BaseEntity
{
    /**
     * 类型标签
     */
    private String dictLabel;

    /**
     * 金额
     */
    private Double mny;

}