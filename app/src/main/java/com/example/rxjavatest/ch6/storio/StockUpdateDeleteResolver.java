package com.example.rxjavatest.ch6.storio;


import com.example.rxjavatest.ch6.StockUpdate;
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver;
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery;

import io.reactivex.annotations.NonNull;

public class StockUpdateDeleteResolver extends DefaultDeleteResolver<StockUpdate> {
    @NonNull
    @Override
    protected DeleteQuery mapToDeleteQuery(@NonNull StockUpdate object) {
        return DeleteQuery.builder()
                .table(StockUpdateTable.TABLE)
                .where(StockUpdateTable.Columns.ID + " = ?")
                .whereArgs(object.getId())
                .build();
    }
}
