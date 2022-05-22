package com.example.rxjavatest.ch10;

import androidx.annotation.Nullable;

import com.example.rxjavatest.yahoo.json.YahooStockResult;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import twitter4j.Status;


public class StockUpdate implements Serializable {
    private final String stockSymbol;
    private final BigDecimal price;
    private final Date date;
    private Integer id;
    private final String twitterStatus;

    public StockUpdate(String stockSymbol, BigDecimal price, Date date, String twitterStatus) {
        if(stockSymbol == null) stockSymbol = "";
        if(twitterStatus == null) twitterStatus = "";

        this.stockSymbol = stockSymbol;
        this.price = price;
        this.date = date;
        this.twitterStatus = twitterStatus;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Date getDate() {
        return date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTwitterStatus() {
        return twitterStatus;
    }

    public static StockUpdate create(YahooStockResult r) {
        return new StockUpdate(r.getSymbol(), r.getLastTradePriceOnly(), new Date(), "");
    }

    public static StockUpdate create(Status status) {
        return new StockUpdate("", BigDecimal.ZERO, status.getCreatedAt(), status.getText());
    }

    public boolean isTwitterStatusUpdate(){
        return !twitterStatus.isEmpty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        StockUpdate that = (StockUpdate) o;
        if(!stockSymbol.equals(that.stockSymbol)) return false;
        if(!price.equals(that.price)) return false;
        if(!twitterStatus.equals(that.twitterStatus)) return false;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        int result = stockSymbol.hashCode();
        result = 31 * result + price.hashCode();
        result = 31 * result + twitterStatus.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
