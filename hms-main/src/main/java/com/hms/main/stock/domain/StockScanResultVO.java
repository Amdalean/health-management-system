package com.hms.main.stock.domain;

/**
 * 全市场扫描结果 VO
 *
 * add by CYQ 2026-06-08
 */
public class StockScanResultVO {

    private String symbol;
    private String name;
    private String action;
    private Double price;
    private Integer suggestedShares;
    private Double suggestedAmount;
    private String strategy;
    private String regime;
    private String confidence;
    private Integer score;
    private String reason;
    private Double pctChg;
    private Double amount;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getSuggestedShares() { return suggestedShares; }
    public void setSuggestedShares(Integer suggestedShares) { this.suggestedShares = suggestedShares; }

    public Double getSuggestedAmount() { return suggestedAmount; }
    public void setSuggestedAmount(Double suggestedAmount) { this.suggestedAmount = suggestedAmount; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getRegime() { return regime; }
    public void setRegime(String regime) { this.regime = regime; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Double getPctChg() { return pctChg; }
    public void setPctChg(Double pctChg) { this.pctChg = pctChg; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
