package com.example.rxjavatest.ch6;

import android.util.Log;

import io.reactivex.functions.Consumer;


public class ErrorHandler implements Consumer<Throwable> {
    private static final ErrorHandler INSTANCE = new ErrorHandler();

    public static ErrorHandler get() {
        return INSTANCE;
    }

    private ErrorHandler() {
    }

    @Override
    public void accept(Throwable throwable) {
        Log.e("APP", "Error on " + Thread.currentThread().getName() + ":", throwable);
    }
}
