package com.example.rxjavatest.ch6;

import com.example.rxjavatest.yahoo.json.YahooStockResult;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


public class StockUpdate implements Serializable {
    private final String stockSymbol;
    private final BigDecimal price;
    private final Date date;
    private Integer id;

    public StockUpdate(String stockSymbol, BigDecimal price, Date date) {
        this.stockSymbol = stockSymbol;
        this.price = price;
        this.date = date;
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

    public static StockUpdate create(YahooStockResult r) {
        return new StockUpdate(r.getSymbol(), r.getLastTradePriceOnly(), new Date());
    }
}
