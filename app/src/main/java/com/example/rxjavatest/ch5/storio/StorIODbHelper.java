package com.example.rxjavatest.ch5.storio;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import io.reactivex.annotations.NonNull;

public class StorIODbHelper extends SQLiteOpenHelper {
    StorIODbHelper(@NonNull Context context) {
        super(context, "reactivestocks.db", null, 1);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(StockUpdateTable.createTableQuery());
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
