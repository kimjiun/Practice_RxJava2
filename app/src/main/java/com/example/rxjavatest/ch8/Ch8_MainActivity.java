package com.example.rxjavatest.ch8;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rxjavatest.Constant;
import com.example.rxjavatest.R;
import com.example.rxjavatest.ch8.storio.StockUpdateTable;
import com.example.rxjavatest.ch8.storio.StorIOFactory;
import com.example.rxjavatest.yahoo.RetrofitYahooServiceFactory;
import com.example.rxjavatest.yahoo.YahooService;
import com.pushtorefresh.storio.sqlite.queries.Query;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.MainThreadDisposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class Ch8_MainActivity extends RxAppCompatActivity {
    @BindView(R.id.hello_world_salute)
    TextView helloText;

    @BindView(R.id.no_data_available)
    TextView noDataAvailableView;

    @BindView(R.id.stock_updates_recycler_view)
    RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private StockDataAdapter stockDataAdapter;

    private CompositeDisposable disposable;
    BehaviorSubject<ActivityEvent> lifecycleSubject = BehaviorSubject.create();

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
        setStocks();
    }

    private void setStocks(){
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        stockDataAdapter = new StockDataAdapter();
        recyclerView.setAdapter(stockDataAdapter);

        YahooService yahooService = new RetrofitYahooServiceFactory().create();

        String symbols = "AAPL,GOOG,MSFT";

        final Configuration configuration = new ConfigurationBuilder()
                .setDebugEnabled(true)
                .setOAuthConsumerKey(Constant.twitter_api_consumer_key)
                .setOAuthConsumerSecret(Constant.twitter_api_consumer_key_secret)
                .setOAuth2AccessToken(Constant.twitter_api_access_token)
                .setOAuthAccessTokenSecret(Constant.twitter_api_access_token_secret)
                .build();

        final FilterQuery filterQuery = new FilterQuery()
                .track("Apple", "Google", "Microsoft")
                .language("en");

        Observable.merge(
            Observable.interval(30, 5, TimeUnit.SECONDS)
                    .flatMap(
                            i -> yahooService.yqlQuery("US", "en", symbols)
                                    .toObservable()
                    )
                .map(r -> r.getQuoteResponse().getResult())
                .flatMap(Observable::fromIterable)
                .map(StockUpdate::create),
                observeTwitterStream(configuration, filterQuery)
                    .sample(700, TimeUnit.MILLISECONDS)
                    .map(StockUpdate::create)
        )
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .doOnError(ErrorHandler.get())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> Toast.makeText(this, "We couldn't reach internet - falling back to local data", Toast.LENGTH_SHORT).show())
                .doOnNext(this::saveStockUpdate)
                .onExceptionResumeNext(
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
                    recyclerView.smoothScrollToPosition(0);
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
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onComplete();
            }
        });

        Observable.<Integer>create(e ->{
            e.onNext(1);
            e.onNext(2);
            e.onComplete();
        });
    }

    private void ex2(){
        Observable.<Integer>create(e ->{
            try {
                e.onNext(returnValue());
            }
            catch(Exception ex){
                e.onError(ex);
            }
            finally {
                e.onComplete();
            }
        });
    }

    private void ex3(){
        Observable.create(emitter -> {
            emitter.setCancellable(() -> helloText.setOnClickListener(null));
            helloText.setOnClickListener(v -> emitter.onNext(v));
        });
    }

    private void ex4(){
        Observable.create(emitter -> {
            emitter.setDisposable(new MainThreadDisposable() { // TextView와의 상호작용이 메인 안드로이드 UI 스레드에서 발생
                @Override
                protected void onDispose() {
                    helloText.setOnClickListener(null);
                }
            });
            helloText.setOnClickListener(v -> emitter.onNext(v));
        });
    }

    Observable<Status> observeTwitterStream(Configuration configuration, FilterQuery filterQuery){
        return Observable.create(emitter -> {
            final TwitterStream twitterStream = new TwitterStreamFactory(configuration).getInstance();

            emitter.setCancellable(() -> {
                Schedulers.io().scheduleDirect(() -> twitterStream.cleanUp());
            });

            StatusListener listener = new StatusListener() {
                @Override
                public void onStatus(Status status) {
                    emitter.onNext(status); // 상태 없데이트 추가
                }

                @Override
                public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

                }

                @Override
                public void onTrackLimitationNotice(int numberOfLimitedStatuses) {

                }

                @Override
                public void onScrubGeo(long userId, long upToStatusId) {

                }

                @Override
                public void onStallWarning(StallWarning warning) {

                }

                @Override
                public void onException(Exception ex) {
                    emitter.onError(ex);
                }
            };

            twitterStream.addListener(listener);
            twitterStream.filter(filterQuery);
        });
    }



    private int returnValue(){
        int[] ii = new int[5];
        ii[7] = 1;

        return 1;
    }

    private void log(String stage, Throwable throwable) {
        Log.e("APP", stage, throwable);
    }

    private void log(Throwable throwable) {
        Log.e("APP", "Error", throwable);
    }

    private void log(String stage, String item) { Log.d("APP", stage + ":" + Thread.currentThread().getName() + ":" + item); }

    private static void log(String stage) { Log.d("APP", stage + ":" + Thread.currentThread().getName());}

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

    @Override
    protected void onDestroy() {
        if(disposable != null){
            disposable.dispose();
        }

        lifecycleSubject.onNext(ActivityEvent.DESTROY);

        super.onDestroy();
    }
}
