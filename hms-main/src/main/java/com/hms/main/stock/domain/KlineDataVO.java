package com.hms.main.stock.domain;

import java.util.Map;
import java.util.List;

/**
 * K线数据 VO（含信号标注）
 * add by Hermes 2026-06-29
 */
public class KlineDataVO {
    private String symbol;
    private String name;
    private List<KlineBar> bars;
    private List<SignalMarker> signals;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<KlineBar> getBars() { return bars; }
    public void setBars(List<KlineBar> bars) { this.bars = bars; }
    public List<SignalMarker> getSignals() { return signals; }
    public void setSignals(List<SignalMarker> signals) { this.signals = signals; }

    public static class KlineBar {
        private String date;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long volume;
        private Double ma10;
        private Double ma30;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Double getOpen() { return open; }
        public void setOpen(Double open) { this.open = open; }
        public Double getHigh() { return high; }
        public void setHigh(Double high) { this.high = high; }
        public Double getLow() { return low; }
        public void setLow(Double low) { this.low = low; }
        public Double getClose() { return close; }
        public void setClose(Double close) { this.close = close; }
        public Long getVolume() { return volume; }
        public void setVolume(Long volume) { this.volume = volume; }
        public Double getMa10() { return ma10; }
        public void setMa10(Double ma10) { this.ma10 = ma10; }
        public Double getMa30() { return ma30; }
        public void setMa30(Double ma30) { this.ma30 = ma30; }
    }

    public static class SignalMarker {
        private String date;
        private String type;    // "golden_cross", "death_cross"
        private Double price;
        private String label;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}
