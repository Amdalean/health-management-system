package com.hms.main.stock.domain;

import java.util.Date;

/**
 * 季频财务 VO（Baostock → stock_financial_quarter，只读展示）
 * add by CYQ 2026-09-06 财务层闭环
 */
public class StockFinancialVO {
    private String symbol;
    private Date statDate;
    private Date pubDate;
    private Integer year;
    private Integer quarter;
    private Double roeAvg;
    private Double netProfitWan;
    private Double epsTtm;
    private Double revenueWan;
    private Double liabilityToAsset;
    private Double ebitToInterest;
    private Double cfoToNp;
    private Double yoyNi;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Date getStatDate() { return statDate; }
    public void setStatDate(Date statDate) { this.statDate = statDate; }
    public Date getPubDate() { return pubDate; }
    public void setPubDate(Date pubDate) { this.pubDate = pubDate; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getQuarter() { return quarter; }
    public void setQuarter(Integer quarter) { this.quarter = quarter; }
    public Double getRoeAvg() { return roeAvg; }
    public void setRoeAvg(Double roeAvg) { this.roeAvg = roeAvg; }
    public Double getNetProfitWan() { return netProfitWan; }
    public void setNetProfitWan(Double netProfitWan) { this.netProfitWan = netProfitWan; }
    public Double getEpsTtm() { return epsTtm; }
    public void setEpsTtm(Double epsTtm) { this.epsTtm = epsTtm; }
    public Double getRevenueWan() { return revenueWan; }
    public void setRevenueWan(Double revenueWan) { this.revenueWan = revenueWan; }
    public Double getLiabilityToAsset() { return liabilityToAsset; }
    public void setLiabilityToAsset(Double liabilityToAsset) { this.liabilityToAsset = liabilityToAsset; }
    public Double getEbitToInterest() { return ebitToInterest; }
    public void setEbitToInterest(Double ebitToInterest) { this.ebitToInterest = ebitToInterest; }
    public Double getCfoToNp() { return cfoToNp; }
    public void setCfoToNp(Double cfoToNp) { this.cfoToNp = cfoToNp; }
    public Double getYoyNi() { return yoyNi; }
    public void setYoyNi(Double yoyNi) { this.yoyNi = yoyNi; }
}
