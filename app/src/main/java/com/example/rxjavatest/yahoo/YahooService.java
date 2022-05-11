package com.example.rxjavatest.yahoo;

import com.example.rxjavatest.Constant;
import com.example.rxjavatest.yahoo.json.YahooStockResults;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface YahooService {
    String APIKEY = Constant.yahoo_stock_api_key;

    @Headers("X-API-KEY: " + APIKEY)
    @GET("quote?format=json")
    Single<YahooStockResults> yqlQuery(
            @Query("region") String region,
            @Query("lang") String lang,
            @Query("symbols") String symbols
    );
}
