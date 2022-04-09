package com.example.rxjavatest.yahoo.json;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class YahooStockResult {
    private String symbol;

    @SerializedName("displayName")
    private String name;

    @SerializedName("regularMarketPrice")
    private BigDecimal lastTradePriceOnly;

    @SerializedName("regularMarketDayLow")
    private BigDecimal daysLow;

    @SerializedName("regularMarketDayHigh")
    private BigDecimal daysHigh;

    @SerializedName("regularMarketVolume")
    private String volume;

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getDaysLow() {
        return daysLow;
    }

    public BigDecimal getDaysHigh() {
        return daysHigh;
    }

    public String getVolume() {
        return volume;
    }

    public BigDecimal getLastTradePriceOnly() {
        return lastTradePriceOnly;
    }
}
