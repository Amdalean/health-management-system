package com.hms.main.summary.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 财务汇总收入构成对象 hsm_summary_income
 *
 * add by CYQ 2026-09-21 收入构成维护
 */
public class HsmSummaryIncome extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联财务汇总主表ID */
    private Long summaryId;

    /** 收入项目 */
    @Excel(name = "收入项目")
    private String itemName;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 排序 */
    private Integer sort;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getSummaryId()
    {
        return summaryId;
    }

    public void setSummaryId(Long summaryId)
    {
        this.summaryId = summaryId;
    }

    public String getItemName()
    {
        return itemName;
    }

    public void setItemName(String itemName)
    {
        this.itemName = itemName;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("summaryId", getSummaryId())
            .append("itemName", getItemName())
            .append("amount", getAmount())
            .append("sort", getSort())
            .append("remark", getRemark())
            .toString();
    }
}
