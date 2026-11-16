package com.hms.main.stock.domain;

/**
 * 持仓 VO
 * add by CYQ 2026-06-05
 */
public class StockHoldingVO {
    private String symbol;
    private String name;
    private Integer shares;
    private Double costPrice;
    private Double currentPrice;
    private Double marketValue;
    private Double profitLoss;
    private Double profitLossPct;
    private String assignedStrategy;
    private String regime;
    /** 归属人（持仓所属用户昵称） add by CYQ 2026-09-21 */
    private String ownerName;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getShares() { return shares; }
    public void setShares(Integer shares) { this.shares = shares; }
    public Double getCostPrice() { return costPrice; }
    public void setCostPrice(Double costPrice) { this.costPrice = costPrice; }
    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }
    public Double getMarketValue() { return marketValue; }
    public void setMarketValue(Double marketValue) { this.marketValue = marketValue; }
    public Double getProfitLoss() { return profitLoss; }
    public void setProfitLoss(Double profitLoss) { this.profitLoss = profitLoss; }
    public Double getProfitLossPct() { return profitLossPct; }
    public void setProfitLossPct(Double profitLossPct) { this.profitLossPct = profitLossPct; }
    public String getAssignedStrategy() { return assignedStrategy; }
    public void setAssignedStrategy(String assignedStrategy) { this.assignedStrategy = assignedStrategy; }
    public String getRegime() { return regime; }
    public void setRegime(String regime) { this.regime = regime; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
