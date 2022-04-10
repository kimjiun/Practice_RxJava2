package com.example.rxjavatest.ch5;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rxjavatest.R;
import com.example.rxjavatest.ch5.storio.StorIOFactory;
import com.example.rxjavatest.yahoo.RetrofitYahooServiceFactory;
import com.example.rxjavatest.yahoo.YahooService;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;

public class Ch5_MainActivity extends AppCompatActivity {
    @BindView(R.id.hello_world_salute)
    TextView helloText;

    @BindView(R.id.stock_updates_recycler_view)
    RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private StockDataAdapter stockDataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        //ex1();
        //ex2();
        //ex3();
        //ex4();
        setStocks();
    }

    private void setStocks(){
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        stockDataAdapter = new StockDataAdapter();
        recyclerView.setAdapter(stockDataAdapter);

        YahooService yahooService = new RetrofitYahooServiceFactory().create();

        String symbols = "TSLA,AAPL,GOOG,MSFT";

        Disposable disposable = Observable.interval(0, 5, TimeUnit.SECONDS)
                .flatMap(
                        i -> yahooService.yqlQuery("US", "en", symbols)
                                .toObservable()
                )
                .subscribeOn(Schedulers.io())
                .map(r -> r.getQuoteResponse().getResult())
                .flatMap(Observable::fromIterable)
                .map(StockUpdate::create)
                .doOnNext(this::saveStockUpdate)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stockUpdate -> {
                    Log.d("APP", "New update " + stockUpdate.getStockSymbol());
                    stockDataAdapter.add(stockUpdate);
                });
    }

    private void saveStockUpdate(StockUpdate stockUpdate){
        log("saveStockUpdate", stockUpdate.getStockSymbol());
        StorIOFactory.get(this)
                .put()
                .object(stockUpdate)
                .prepare()
                .asRxSingle()
                .subscribe();
    }

    public static <T> Observable<T> v2(rx.Observable<T> source) {
        return toV2Observable(source);
    }

    private void ex1(){
        Observable.just("One", "Two", "Three")
                .subscribeOn(Schedulers.single())
                .doOnNext(i -> log("doOnNext", i))
                .subscribeOn(Schedulers.newThread())
                .doOnNext(i -> log("doOnNext", i))
                .subscribeOn(Schedulers.io())
                .subscribe(i -> log("subscribe", i));
    }

    // Scheduler Test
    private void ex2(){
        Observable.just("One", "Two", "Three")
                .doOnNext(i -> log("doOnNext", i))
                .observeOn(Schedulers.newThread())
                .doOnNext(i -> log("doOnNext", i))
                .observeOn(Schedulers.computation())
                .subscribe(i -> log("subscribe", i));
    }

    private void ex3(){
        Observable.just("One", "Two")
                .doOnNext(i -> log("doOnNext", i))
                .observeOn(Schedulers.computation())
                .subscribe(i -> log("subscribe", i));

        Observable.range(1, 1000)
                .map(Object::toString)
                .doOnNext(i -> log("doOnNext", i))
                .observeOn(Schedulers.computation())
                .subscribe(i -> log("subscribe", i));
    }

    private void ex4(){
        Observable.range(1, 100)
                .flatMap(i -> Observable.just(i) // 100개의 옵저버블을 새로만듬
                    .subscribeOn(Schedulers.io())
                    .map(Ch5_MainActivity::importantLongTask)
                )
                .map(Object::toString)
                .subscribe(e -> log("subscribe", e));
    }

    private void log(Throwable throwable) {
        Log.e("APP", "Error", throwable);
    }

    private void log(String stage, String item) {
        Log.d("APP", stage + ":" + Thread.currentThread().getName() + ":" + item);
    }

    private static void log(String stage) {
        Log.d("APP", stage + ":" + Thread.currentThread().getName());
    }

    public static int importantLongTask(int i) {
        try {
            long minMillis = 10L;
            long maxMillis = 1000L;
            log("Working on " + i);
            final long waitingTime = (long) (minMillis + (Math.random() * maxMillis - minMillis));
            Thread.sleep(waitingTime);
            return i;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
