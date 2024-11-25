package com.hms.main.jotdown.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 随手记对象 hsm_jotdown
 * 
 * @author ruoyi
 * @date 2024-11-16
 */
public class HsmJotdown extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 人员 */
    @Excel(name = "人员")
    private String personnel;

    /** 辅助核算 */
    @Excel(name = "辅助核算")
    private String auxiliaryAccounting;

    /** 说明 */
    @Excel(name = "说明")
    private String description;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date date;

    /** 会计期间 */
    @Excel(name = "会计期间")
    private String accountingPeriod;

    /** 费用类型 */
    @Excel(name = "费用类型")
    private String expenseType;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setPersonnel(String personnel) 
    {
        this.personnel = personnel;
    }

    public String getPersonnel() 
    {
        return personnel;
    }
    public void setAuxiliaryAccounting(String auxiliaryAccounting) 
    {
        this.auxiliaryAccounting = auxiliaryAccounting;
    }

    public String getAuxiliaryAccounting() 
    {
        return auxiliaryAccounting;
    }
    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }
    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
    }
    public void setDate(Date date) 
    {
        this.date = date;
    }

    public Date getDate() 
    {
        return date;
    }
    public void setAccountingPeriod(String accountingPeriod) 
    {
        this.accountingPeriod = accountingPeriod;
    }

    public String getAccountingPeriod() 
    {
        return accountingPeriod;
    }
    public void setExpenseType(String expenseType) 
    {
        this.expenseType = expenseType;
    }

    public String getExpenseType() 
    {
        return expenseType;
    }
    public void setRemarks(String remarks) 
    {
        this.remarks = remarks;
    }

    public String getRemarks() 
    {
        return remarks;
    }
    public void setCreatedAt(Date createdAt) 
    {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() 
    {
        return createdAt;
    }
    public void setUpdatedAt(Date updatedAt) 
    {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() 
    {
        return updatedAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("personnel", getPersonnel())
            .append("auxiliaryAccounting", getAuxiliaryAccounting())
            .append("description", getDescription())
            .append("amount", getAmount())
            .append("date", getDate())
            .append("accountingPeriod", getAccountingPeriod())
            .append("expenseType", getExpenseType())
            .append("remarks", getRemarks())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
}
