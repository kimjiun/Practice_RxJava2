package com.example.rxjavatest.ch11;

import static com.example.rxjavatest.ch10.LocalItemPersistenceHandlingTransformer.addLocalPersistence;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rxjavatest.Constant;
import com.example.rxjavatest.R;
import com.example.rxjavatest.ch10.ErrorHandler;
import com.example.rxjavatest.ch10.StockDataAdapter;
import com.example.rxjavatest.ch10.StockUpdate;
import com.example.rxjavatest.ch10.storio.StorIOFactory;
import com.example.rxjavatest.yahoo.RetrofitYahooServiceFactory;
import com.example.rxjavatest.yahoo.YahooService;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
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

public class Ch11_MainActivity extends RxAppCompatActivity {
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

        ex1();
        //setStocks();
    }

    private void ex1(){
        Subject<Long> subject = PublishSubject.create();
        Observable.interval(2, TimeUnit.SECONDS)
                .take(5)
                .doOnSubscribe((d) -> log("ORIG Subscribe"))
                .doOnComplete(() -> log("ORIG doOnCOmplete"))
                .subscribe(subject);

        subject
                .doOnSubscribe((d) -> log("First Subscribe"))
                .doOnComplete(() -> log("First doOnCOmplete"))
                .subscribe(v -> log("First:" + v));

        try {
            Thread.sleep(4100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        subject
                .doOnSubscribe((d) -> log("Second Subscribe"))
                .doOnComplete(() -> log("Second doOnCOmplete"))
                .subscribe(v -> log("Second:" + v));
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

        final String[] trackingKeywords = {"Apple", "Google", "Microsoft"};

        final FilterQuery filterQuery = new FilterQuery()
                .track(trackingKeywords)
                .language("en");

        Observable.merge(
                createFinancialStockUpdateObservable(yahooService, symbols),
                createTweetStockUpdateObservable(configuration, trackingKeywords, filterQuery)
        )
        .compose(bindToLifecycle())
        .subscribeOn(Schedulers.io())
        .doOnError(ErrorHandler.get())
        .compose(addUiErrorHandling())
        .compose(addLocalPersistence(this))
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

    private ObservableTransformer<StockUpdate, StockUpdate> addUiErrorHandling(){
        return upstream -> upstream.observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showToastErrorNotiMethod)
                .observeOn(Schedulers.io());
    }

    private void showToastErrorNotiMethod(Throwable throwable) {
        Toast.makeText(this, "We couldn't reach internet - falling back to local data", Toast.LENGTH_SHORT).show();
    }

    private ObservableTransformer<StockUpdate, StockUpdate> addLocalItemPersistenceHandling(){
        return upstream -> upstream.doOnNext(this::saveStockUpdate)
                    .onExceptionResumeNext(StorIOFactory.loadLocalDataObservable(this));
    }

    private Observable<StockUpdate> createTweetStockUpdateObservable(Configuration configuration, String[] trackingKeywords, FilterQuery filterQuery) {
        return observeTwitterStream(configuration, filterQuery)
                .sample(2700, TimeUnit.MILLISECONDS)
                .map(StockUpdate::create)
                .filter(
                        getStockUpdatePredicate(trackingKeywords)
                )
                .flatMapMaybe(
                        getStockUpdateMaybeSourceFunction(trackingKeywords)
                );
    }

    private Observable<StockUpdate> createFinancialStockUpdateObservable(YahooService yahooService, String symbols) {
        return Observable.interval(0, 5, TimeUnit.SECONDS)
                .flatMapSingle(
                        i -> yahooService.yqlQuery("US", "en", symbols)
                )
                .map(r -> r.getQuoteResponse().getResult())
                .flatMap(Observable::fromIterable)
                .map(StockUpdate::create)
                .groupBy(StockUpdate::getStockSymbol)
                .flatMap(Observable::distinctUntilChanged);
    }

    @NonNull
    private Function<StockUpdate, MaybeSource<? extends StockUpdate>> getStockUpdateMaybeSourceFunction(String[] trackingKeywords) {
        return update -> Observable.fromArray(trackingKeywords)
                .filter(keyword -> update.getTwitterStatus()
                        .toLowerCase().contains(keyword.toLowerCase()))
                .map(keyword -> update)
                .firstElement();
    }

    @NonNull
    private Consumer<Throwable> showToastErrorNoti() {
        return error -> Toast.makeText(this, "We couldn't reach internet - falling back to local data", Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private io.reactivex.functions.Predicate<StockUpdate> getStockUpdatePredicate(String[] trackingKeywords) {
        return stockUpdate -> {
            for (String keyword : trackingKeywords) {
                if (stockUpdate.getTwitterStatus().contains(keyword)) {
                    return true;
                }
            }
            return false;
        };
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
