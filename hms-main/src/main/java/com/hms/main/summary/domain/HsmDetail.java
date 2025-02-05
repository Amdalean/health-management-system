package com.hms.main.summary.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 财务明细子表对象 hsm_detail
 * 
 * @author CYQ
 * @date 2025-02-05
 */
public class HsmDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联主表ID */
    @Excel(name = "关联主表ID")
    private Long summaryId;

    /** 所属类别 */
    @Excel(name = "所属类别")
    private String category;

    /** 类型（收入/支出） */
    @Excel(name = "类型", readConverterExp = "收=入/支出")
    private String type;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 变化量 */
    @Excel(name = "变化量")
    private BigDecimal changeAmount;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setSummaryId(Long summaryId) 
    {
        this.summaryId = summaryId;
    }

    public Long getSummaryId() 
    {
        return summaryId;
    }
    public void setCategory(String category) 
    {
        this.category = category;
    }

    public String getCategory() 
    {
        return category;
    }
    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }
    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
    }
    public void setChangeAmount(BigDecimal changeAmount) 
    {
        this.changeAmount = changeAmount;
    }

    public BigDecimal getChangeAmount() 
    {
        return changeAmount;
    }
    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("summaryId", getSummaryId())
            .append("category", getCategory())
            .append("type", getType())
            .append("amount", getAmount())
            .append("changeAmount", getChangeAmount())
            .append("remark", getRemark())
            .append("name", getName())
            .toString();
    }
}
