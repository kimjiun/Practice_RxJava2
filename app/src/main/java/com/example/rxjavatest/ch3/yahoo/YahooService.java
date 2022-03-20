package com.example.rxjavatest.ch3.yahoo;

import com.example.rxjavatest.ch3.yahoo.json.YahooStockResults;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface YahooService {
    String APIKEY = "Your Key";

    @Headers("X-API-KEY: " + APIKEY)
    @GET("quote?format=json")
    Single<YahooStockResults> yqlQuery(
            @Query("region") String region,
            @Query("lang") String lang,
            @Query("symbols") String symbols
    );
}
