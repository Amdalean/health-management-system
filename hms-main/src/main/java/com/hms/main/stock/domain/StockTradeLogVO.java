package com.hms.main.stock.domain;

import java.util.Date;

/**
 * 交易记录 VO
 * add by Hermes 2026-06-29
 */
public class StockTradeLogVO {
    private Long id;
    private String symbol;
    private String name;
    private String action;
    private Double price;
    private Integer shares;
    private Double amount;
    private Double fee;
    private Long recommendationId;
    private String note;
    private Date tradeTime;
    private Double profitLoss;
    private Double profitLossPct;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getShares() { return shares; }
    public void setShares(Integer shares) { this.shares = shares; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Double getFee() { return fee; }
    public void setFee(Double fee) { this.fee = fee; }
    public Long getRecommendationId() { return recommendationId; }
    public void setRecommendationId(Long recommendationId) { this.recommendationId = recommendationId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Date getTradeTime() { return tradeTime; }
    public void setTradeTime(Date tradeTime) { this.tradeTime = tradeTime; }
    public Double getProfitLoss() { return profitLoss; }
    public void setProfitLoss(Double profitLoss) { this.profitLoss = profitLoss; }
    public Double getProfitLossPct() { return profitLossPct; }
    public void setProfitLossPct(Double profitLossPct) { this.profitLossPct = profitLossPct; }
}
