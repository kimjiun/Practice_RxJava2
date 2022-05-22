package com.example.rxjavatest.ch9;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rxjavatest.Constant;
import com.example.rxjavatest.R;
import com.example.rxjavatest.ch9.ErrorHandler;
import com.example.rxjavatest.ch9.StockDataAdapter;
import com.example.rxjavatest.ch9.StockUpdate;
import com.example.rxjavatest.ch9.storio.StockUpdateTable;
import com.example.rxjavatest.ch9.storio.StorIOFactory;
import com.example.rxjavatest.yahoo.RetrofitYahooServiceFactory;
import com.example.rxjavatest.yahoo.YahooService;
import com.pushtorefresh.storio.sqlite.queries.Query;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.javatuples.Triplet;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.MainThreadDisposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import kotlin.Triple;
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

public class Ch9_MainActivity extends RxAppCompatActivity {
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
        ex5();
        //setStocks();
    }

    private void setStocks(){
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        stockDataAdapter = new StockDataAdapter();
        recyclerView.setAdapter(stockDataAdapter);

        YahooService yahooService = new RetrofitYahooServiceFactory().create();

        String symbols = "AAPL,GOOG,MSFT";

        Configuration configuration = new ConfigurationBuilder()
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
            Observable.interval(0, 5, TimeUnit.SECONDS)
                    .flatMap(
                            i -> yahooService.yqlQuery("US", "en", symbols)
                                    .toObservable()
                    )
                .map(r -> r.getQuoteResponse().getResult())
                .flatMap(Observable::fromIterable)
                .map(StockUpdate::create),
                observeTwitterStream(configuration, filterQuery)
                    .sample(2700, TimeUnit.MILLISECONDS)
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
                .filter(update -> !stockDataAdapter.contains(update))
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

    // map
    private void ex1(){
        Observable.just(1, 2, 3)
            .map(i -> i + 1)
            .subscribe(e -> log("NUM : " + e));

        // .map() 메소드에서는 값을 수정하지 말고 항상 새로운 값을 반환하는 것이 좋음 따라서 아래는 좋지 않음
        Observable.just(new Date(1), new Date(2), new Date())
                .map(i -> {
                    i.setTime(i.getTime() + 1);
                    return  i;
                })
                .subscribe(e -> log("subscribe", "DaTE : " + e.toString()));

        // 아래 접근법은 동시 수정 버그를 피하는데 도움이 되며, 디버깅과 추론을 하기 더 쉽다.
        Observable.just(new Date(1), new Date(2), new Date())
                .map(i -> new Date(i.getTime() + 1));

        // 값의 유형을 변경할수도 있음
        Observable.just(new Date(1), new Date(2), new Date())
                .map(i -> new Date(i.getTime() + 1))
                .subscribe(e -> log("subscribe", "DaTE : " + e.toString()));
    }

    // flatmap
    private void ex2(){
        final Observable<Observable<Date>> map =
                Observable.just("ID1", "ID2", "ID3")
                        .map(id -> Observable.fromCallable(mockHttpRequest(id)));

        Observable.just("ID1", "ID2", "ID3")
                .map(id -> Observable.fromCallable(mockHttpRequest(id)))
                .subscribe(e -> {
                    e.subscribe(v -> log("subscribe-subscribe", v.toString()));
                });

        Observable.just("ID1", "ID2", "ID3")
                .flatMap(id -> Observable.fromCallable(mockHttpRequest(id)))
                .subscribe(e -> log(e.toString()));

        Observable.interval(0, 5, TimeUnit.SECONDS)
                .flatMap(
                        i -> singleTest().toObservable()
                );

        Observable.interval(0, 5, TimeUnit.SECONDS)
                .flatMapSingle(
                        i -> singleTest()
                );
    }

    // switchmap
    private void ex3(){
        Observable.interval(3, TimeUnit.SECONDS)
                .take(2)
                .switchMap(x -> Observable.interval(1, TimeUnit.SECONDS)
                    .map(i -> x + "A" + i)
                    .take(5)
                )
                .subscribe(item -> log("flatmap", item));
    }

    // 튜플
    private void ex4(){
        // Pair
        Observable.just("UserId1", "UserId2", "UserId3")
                .map(id -> Pair.create(id, id + "-access-token"))
                .subscribe(pair -> {
                    log("subscribe-subscribe", pair.second);
                });

        // Triplet
        Observable.just("UserId1", "UserId2", "UserId3")
                .map(id -> Triplet.with(id, id + "-access-token", "third-value"))
                .subscribe(triplet -> {
                    log(triplet.getValue0(), triplet.getValue1() + triplet.getValue2());
                });

        // Custom
        Observable.just(new User("1"), new User("2"), new User("3"))
                .map(user -> new UserCredentials(user, "accessToken"))
                .subscribe(credentials -> {
                    log(credentials.user.userId, credentials.accessTocken);
                });
    }

    private void ex5(){
        // zip
        /*
        Observable.zip(
                Observable.just("One", "Two", "Three"),
                Observable.interval(1, TimeUnit.SECONDS),
                (number, interval) -> number + "-" + interval
        )
                .subscribe(e -> log(e));

        // combineLatest
        Observable.combineLatest(
                Observable.interval(500, TimeUnit.MILLISECONDS),
                Observable.interval(1, TimeUnit.SECONDS),
                (number, interval) -> number + "-" + interval
        )
                .subscribe(e -> log("subscribe", e));

        // concat
        Observable.concat(
                Observable.interval(3, TimeUnit.SECONDS),
                Observable.just(-1L, -2L)
        )
                .subscribe(v -> log("subscribe", v.toString()));
         */

        // merge
        Observable.merge(
                Observable.interval(3, TimeUnit.SECONDS),
                Observable.just(-3L, -4L)
        )
                .subscribe(v -> log("subscribe", v.toString()));
    }

    Observable<Status> observeTwitterStream(Configuration configuration, FilterQuery filterQuery) {
        return Observable.create(emitter -> {
            final TwitterStream twitterStream = new TwitterStreamFactory(configuration).getInstance();

            emitter.setCancellable(() -> {
                Schedulers.io().scheduleDirect(() -> twitterStream.cleanUp());
            });

            StatusListener listener = new StatusListener() {
                @Override
                public void onStatus(Status status) {
                    emitter.onNext(status);
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

    private Callable<Date> mockHttpRequest(String id){
        return Date::new;
    }

    private Single<Date> singleTest(){
        Single<Date> v = new Single<Date>() {
            @Override
            protected void subscribeActual(SingleObserver<? super Date> observer) {

            }
        };
        return v;
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
