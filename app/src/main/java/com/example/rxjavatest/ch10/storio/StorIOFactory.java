package com.example.rxjavatest.ch10.storio;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;

import android.content.Context;

import com.example.rxjavatest.ch10.StockUpdate;
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite;
import com.pushtorefresh.storio.sqlite.queries.Query;

import io.reactivex.Observable;

public class StorIOFactory {
    private static StorIOSQLite INSTANCE;

    public synchronized static StorIOSQLite get(Context context) {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        INSTANCE = DefaultStorIOSQLite.builder()
                .sqliteOpenHelper(new StorIODbHelper(context))
                .addTypeMapping(StockUpdate.class, SQLiteTypeMapping.<StockUpdate>builder()
                        .putResolver(new StockUpdatePutResolver())
                        .getResolver(new StockUpdateGetResolver())
                        .deleteResolver(new StockUpdateDeleteResolver())
                        .build())
                .build();

        return INSTANCE;
    }


    public static Observable<StockUpdate> loadLocalDataObservable(Context context) {
        return v2(StorIOFactory.get(context)
                .get()
                .listOfObjects(StockUpdate.class) // 반환될 객체 유형
                .withQuery(Query.builder()
                        .table(StockUpdateTable.TABLE)
                        .orderBy("date DESC")
                        .limit(50)
                        .build())
                .prepare()
                .asRxObservable())
                .take(1) // 옵저버블은 첫번째 요소를 받을때까지 변경 사항을 수신하고, select 쿼리가 실행된 후 종료
                .flatMap(Observable::fromIterable); // 데이터는 List<StockUpdate> 형식으로 반환되므로 이를 연결하기 위함
    }

    public static <T> Observable<T> v2(rx.Observable<T> source) {
        return toV2Observable(source);
    }
}
