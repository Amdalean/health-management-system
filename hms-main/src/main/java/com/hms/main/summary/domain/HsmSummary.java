package com.hms.main.summary.domain;

import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 财务汇总主对象 hsm_summary
 * 
 * @author CYQ
 * @date 2025-02-05
 */
public class HsmSummary extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 年度 */
    @Excel(name = "年度")
    private Long year;

    /** 月份 */
    @Excel(name = "月份")
    private Long month;

    /** 收入 */
    @Excel(name = "收入")
    private BigDecimal income;

    /** 支出 */
    @Excel(name = "支出")
    private BigDecimal expense;

    /** 结余 */
    @Excel(name = "结余")
    private BigDecimal balance;

    /** 月初存款 */
    @Excel(name = "月初存款")
    private BigDecimal startDeposit;

    /** 月底存款 */
    @Excel(name = "月底存款")
    private BigDecimal endDeposit;

    /** 财务明细子表信息 */
    private List<HsmDetail> hsmDetailList;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setYear(Long year) 
    {
        this.year = year;
    }

    public Long getYear() 
    {
        return year;
    }
    public void setMonth(Long month) 
    {
        this.month = month;
    }

    public Long getMonth() 
    {
        return month;
    }
    public void setIncome(BigDecimal income) 
    {
        this.income = income;
    }

    public BigDecimal getIncome() 
    {
        return income;
    }
    public void setExpense(BigDecimal expense) 
    {
        this.expense = expense;
    }

    public BigDecimal getExpense() 
    {
        return expense;
    }
    public void setBalance(BigDecimal balance) 
    {
        this.balance = balance;
    }

    public BigDecimal getBalance() 
    {
        return balance;
    }
    public void setStartDeposit(BigDecimal startDeposit) 
    {
        this.startDeposit = startDeposit;
    }

    public BigDecimal getStartDeposit() 
    {
        return startDeposit;
    }
    public void setEndDeposit(BigDecimal endDeposit) 
    {
        this.endDeposit = endDeposit;
    }

    public BigDecimal getEndDeposit() 
    {
        return endDeposit;
    }

    public List<HsmDetail> getHsmDetailList()
    {
        return hsmDetailList;
    }

    public void setHsmDetailList(List<HsmDetail> hsmDetailList)
    {
        this.hsmDetailList = hsmDetailList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("year", getYear())
            .append("month", getMonth())
            .append("remark", getRemark())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("income", getIncome())
            .append("expense", getExpense())
            .append("balance", getBalance())
            .append("startDeposit", getStartDeposit())
            .append("endDeposit", getEndDeposit())
            .append("hsmDetailList", getHsmDetailList())
            .toString();
    }
}
