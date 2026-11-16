package com.hms.main.stock.domain;

import java.util.Date;

/**
 * 操作建议 VO
 * add by CYQ 2026-06-05
 */
public class StockRecommendationVO {
    private Long id;
    private String symbol;
    private String name;
    private String strategyName;
    private String action;
    private Double priceHint;
    private Integer suggestedShares;
    private Double suggestedAmount;
    /** 实际成交单价（已执行且维护过实际值时非空） add by CYQ 2026-08-04 */
    private Double actualPrice;
    /** 实际成交股数（已执行且维护过实际值时非空） add by CYQ 2026-08-04 */
    private Integer actualShares;
    private String reason;
    private String confidence;
    private String status;
    /** 信号性质：speculation投机/value投资/spread套利（graham-investment-speculation-gate） add by CYQ 2026-09-06 */
    private String evidenceClass;
    /** 归属人（建议所属用户昵称） add by CYQ 2026-09-21 */
    private String ownerName;
    private Date createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Double getPriceHint() { return priceHint; }
    public void setPriceHint(Double priceHint) { this.priceHint = priceHint; }
    public Integer getSuggestedShares() { return suggestedShares; }
    public void setSuggestedShares(Integer suggestedShares) { this.suggestedShares = suggestedShares; }
    public Double getSuggestedAmount() { return suggestedAmount; }
    public void setSuggestedAmount(Double suggestedAmount) { this.suggestedAmount = suggestedAmount; }
    public Double getActualPrice() { return actualPrice; }
    public void setActualPrice(Double actualPrice) { this.actualPrice = actualPrice; }
    public Integer getActualShares() { return actualShares; }
    public void setActualShares(Integer actualShares) { this.actualShares = actualShares; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEvidenceClass() { return evidenceClass; }
    public void setEvidenceClass(String evidenceClass) { this.evidenceClass = evidenceClass; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }
}
