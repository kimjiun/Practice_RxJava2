package com.example.rxjavatest.ch2;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rxjavatest.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class Ch2_MainActivity extends AppCompatActivity {
    private static final String TAG = "JIUN/Ch2_MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ch1_activity_main);

        //ex1();
        //ex2();

        //ex3();
        //ex4();
        //ex5();

        ex6();
    }

    private void ex1(){
        Disposable disposable = Observable.just("First Item", "Second Item")
                .subscribeOn(Schedulers.io())
                .doOnNext(e -> Log.d(TAG, "on-next:" +
                        Thread.currentThread().getName() + ":" + e))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> Log.d(TAG, "subscribe:" +
                        Thread.currentThread().getName() + ":" + e));
    }

    private void ex2(){
        Disposable disposable = Observable.just("ONE", "TWO")
                .subscribeOn(Schedulers.io())
                .doOnDispose(() -> log("doOnDispose"))
                .doOnComplete(() -> log("doOnComplete"))
                .doOnNext(e -> log("doOnNext", e))
                .doOnEach(e -> log("doOnEach"))
                .doOnSubscribe((e) -> log("doOnSubscribe"))
                .doOnTerminate(() -> log("doOnTerminate"))
                .doFinally(() -> log("doFinally"))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> log("subscribe", e));

        if(disposable.isDisposed())
            disposable.dispose();
    }

    // Flowable - 아이템 버리기
    private void ex3(){
        PublishSubject<Integer> observable = PublishSubject.create();

        Disposable disposable = observable.toFlowable(BackpressureStrategy.MISSING) // DROP
                //.onBackpressureDrop()
                .sample(10, TimeUnit.MILLISECONDS)
                .subscribe(v -> log("s", v.toString()), this::log);

        for(int i =0; i< 1000000 ; i++){
            observable.onNext(i);
        }
    }

    // Flowable - 마지막 아이템 유지하기
    private void ex4(){
        PublishSubject<Integer> observable = PublishSubject.create();

        Disposable disposable = observable.toFlowable(BackpressureStrategy.MISSING) // LATEST
                //.onBackpressureLatest()
                .debounce(10, TimeUnit.MILLISECONDS)
                .subscribe(v -> log("s", v.toString()), this::log);

        for(int i =0; i< 1000000 ; i++){
            observable.onNext(i);
        }
    }

    // Flowable - 버퍼링
    private void ex5(){
        PublishSubject<Integer> observable = PublishSubject.create();

        Disposable disposable = observable.toFlowable(BackpressureStrategy.MISSING) // BUFFER
                .onBackpressureBuffer()
                //.buffer(10)
                .subscribe(v -> log("s", v.toString()), this::log);

        for(int i =0; i< 1000 ; i++){
            observable.onNext(i);
        }
    }

    private void log(Throwable throwable){
        Log.e(TAG, "Error", throwable);
    }

    private void log(String stage, String item){
        Log.d(TAG, stage + ":" +Thread.currentThread().getName() + ":" + item);
    }

    private void log(String stage){
        Log.d(TAG, stage + ":" +Thread.currentThread().getName());
    }
}
