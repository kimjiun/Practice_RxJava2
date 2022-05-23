package com.example.rxjavatest.ch10;

import android.content.Context;

import com.example.rxjavatest.ch10.storio.StorIOFactory;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

public class LocalItemPersistenceHandlingTransformer implements ObservableTransformer<StockUpdate, StockUpdate> {
    private final Context context;

    public LocalItemPersistenceHandlingTransformer(Context context) {
        this.context = context;
    }

    public static LocalItemPersistenceHandlingTransformer addLocalPersistence(Context context){
        return new LocalItemPersistenceHandlingTransformer(context);
    }

    @Override
    public ObservableSource<StockUpdate> apply(Observable<StockUpdate> upstream) {
        return upstream.doOnNext(this::saveStockUpdate)
                .onExceptionResumeNext(StorIOFactory.loadLocalDataObservable(context));
    }

    private void saveStockUpdate(StockUpdate stockUpdate){
        StorIOFactory.get(context)
                .put()
                .object(stockUpdate)
                .prepare()
                .asRxSingle()
                .subscribe();
    }
}
