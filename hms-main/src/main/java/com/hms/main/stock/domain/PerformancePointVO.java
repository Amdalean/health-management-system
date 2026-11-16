package com.hms.main.stock.domain;

/**
 * 组合表现数据点 VO
 * add by Hermes 2026-06-29
 */
public class PerformancePointVO {
    private String date;
    private Double portfolioValue;
    private Double stockValue;
    private Double cash;
    private Double totalReturnPct;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Double getPortfolioValue() { return portfolioValue; }
    public void setPortfolioValue(Double portfolioValue) { this.portfolioValue = portfolioValue; }
    public Double getStockValue() { return stockValue; }
    public void setStockValue(Double stockValue) { this.stockValue = stockValue; }
    public Double getCash() { return cash; }
    public void setCash(Double cash) { this.cash = cash; }
    public Double getTotalReturnPct() { return totalReturnPct; }
    public void setTotalReturnPct(Double totalReturnPct) { this.totalReturnPct = totalReturnPct; }
}
