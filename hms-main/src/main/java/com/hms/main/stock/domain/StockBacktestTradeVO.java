package com.hms.main.stock.domain;

import java.util.Date;

/**
 * 回测逐笔交易 VO（stock_backtest_trade 表映射，前台展开用）
 * add by CYQ 2026-09-06
 */
public class StockBacktestTradeVO {

    private Long id;
    private String symbol;
    private String name;
    /** 口径：LIVE=单标的私有资金，PORTFOLIO=组合共享资金账户 */
    private String strategyType;
    /** 组合回测批次号，0=单标的LIVE口径 */
    private Long runId;
    private String action;
    private Double price;
    private Integer volume;
    private String reason;
    private Date tradeDate;
    /** 本笔交易费用（佣金+印花税） */
    private Double fee;
    /** 成交后现金余额（组合口径） */
    private Double cashAfter;
    /** 成交后该标的持仓股数 */
    private Integer positionAfter;
    /** 本笔已实现盈亏（卖出时，已扣卖出费用） */
    private Double realizedPnl;
    /** 成交当日组合总资产 */
    private Double nav;
    private String confidence;
    private String regime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getVolume() { return volume; }
    public void setVolume(Integer volume) { this.volume = volume; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Date getTradeDate() { return tradeDate; }
    public void setTradeDate(Date tradeDate) { this.tradeDate = tradeDate; }
    public Double getFee() { return fee; }
    public void setFee(Double fee) { this.fee = fee; }
    public Double getCashAfter() { return cashAfter; }
    public void setCashAfter(Double cashAfter) { this.cashAfter = cashAfter; }
    public Integer getPositionAfter() { return positionAfter; }
    public void setPositionAfter(Integer positionAfter) { this.positionAfter = positionAfter; }
    public Double getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(Double realizedPnl) { this.realizedPnl = realizedPnl; }
    public Double getNav() { return nav; }
    public void setNav(Double nav) { this.nav = nav; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getRegime() { return regime; }
    public void setRegime(String regime) { this.regime = regime; }
}
