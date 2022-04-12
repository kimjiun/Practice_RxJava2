package com.example.rxjavatest.ch6;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rxjavatest.R;
import com.example.rxjavatest.ch6.storio.StockUpdateTable;
import com.example.rxjavatest.ch6.storio.StorIOFactory;
import com.example.rxjavatest.yahoo.RetrofitYahooServiceFactory;
import com.example.rxjavatest.yahoo.YahooService;
import com.example.rxjavatest.yahoo.json.YahooStockResults;
import com.pushtorefresh.storio.sqlite.queries.Query;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class Ch6_MainActivity extends AppCompatActivity {
    @BindView(R.id.hello_world_salute)
    TextView helloText;

    @BindView(R.id.no_data_available)
    TextView noDataAvailableView;

    @BindView(R.id.stock_updates_recycler_view)
    RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private StockDataAdapter stockDataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        RxJavaPlugins.setErrorHandler(ErrorHandler.get());

        //ex1();
        //ex2();
        //ex3();
        //ex4();
        //ex5();
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

        Disposable disposable = Observable.interval(0, 10, TimeUnit.SECONDS)
                .flatMap(
                        i -> Observable.<YahooStockResults>error(new RuntimeException("Oops"))
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> {
                    log("doOnError", "error");
                    Toast.makeText(this, "We couldn't reach internet - falling back to local data",
                            Toast.LENGTH_SHORT)
                            .show();
                })
                .observeOn(Schedulers.io())
                .map(r -> r.getQuoteResponse().getResult())
                .flatMap(Observable::fromIterable)
                .map(StockUpdate::create)
                .doOnNext(this::saveStockUpdate)
                .onErrorResumeNext(
                    v2(StorIOFactory.get(this)
                        .get()
                        .listOfObjects(StockUpdate.class) // 반환될 객체 유형
                        .withQuery(Query.builder()
                                .table(StockUpdateTable.TABLE)
                                .orderBy("date DESC")
                                .limit(50)
                                .build())
                        .prepare()
                        .asRxObservable()
                    )
                    .take(1) // 옵저버블은 첫번째 요소를 받을때까지 변경 사항을 수신하고, select 쿼리가 실행된 후 종료
                    .flatMap(Observable::fromIterable) // 데이터는 List<StockUpdate> 형식으로 반환되므로 이를 연결하기 위함
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stockUpdate -> {
                    Log.d("APP", "New update " + stockUpdate.getStockSymbol());
                    noDataAvailableView.setVisibility(View.GONE);
                    stockDataAdapter.add(stockUpdate);
                }, error ->{
                    if(stockDataAdapter.getItemCount() == 0){
                        noDataAvailableView.setVisibility(View.VISIBLE);
                    }
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

    private void deleteStockUpdate(StockUpdate stockUpdate){
        log("deleteStockUpdate", stockUpdate.getStockSymbol());
        StorIOFactory.get(this)
                .delete()
                .object(stockUpdate)
                .prepare()
                .asRxSingle()
                .subscribe();
    }

    public static <T> Observable<T> v2(rx.Observable<T> source) {
        return toV2Observable(source);
    }

    private void ex1(){
        Observable.just("One")
                .doOnNext(i -> {throw new RuntimeException();})
                .subscribe(i -> {
                    log("subscribe", i);
                    }, throwable -> {log(throwable);}
                );

        Observable.just("One")
                .doOnNext(i -> {throw new RuntimeException("Very wrong");})
                .subscribe(i -> log("subscribe", i), this::log);
    }

    private void ex2(){
        Observable.<String>error(new RuntimeException("Crash!"))
                .onExceptionResumeNext(Observable.just("Second"))
                .subscribe(i -> {
                            log("subscribe", i);
                        }, e -> log("subscribe"));
    }

    private void ex3(){
        Observable.<String>error(new RuntimeException("Crash!"))
                .doOnError(e -> log("doOnError", e))
                .onExceptionResumeNext(Observable.just("Second"))
                .subscribe(i -> {
                    log("subscribe", i);
                }, e -> log("subscribe"));
    }

    private void ex4(){
        Observable.<String>error(new RuntimeException("Crash!"))
                .onErrorReturn(throwable -> "Return")
                .subscribe(i -> {
                    log("subscribe", i);
                }, e -> log("subscribe"));

        Observable.<String>error(new RuntimeException("Crash!"))
                .onErrorReturnItem("ReturnItem")
                .subscribe(i -> {
                    log("subscribe", i);
                }, e -> log("subscribe"));
    }

    private void ex5(){
        Observable.<String>error(new Error("Crash!"))
                .doOnError(ErrorHandler.get())
                .subscribe(i -> {
                    log("subscribe", i);
                }, ErrorHandler.get());
    }

    private void log(String stage, Throwable throwable) {
        Log.e("APP", stage, throwable);
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
