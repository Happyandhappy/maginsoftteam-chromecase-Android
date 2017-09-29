package com.oxycast.chromecastapp.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.oxycast.chromecastapp.adapters.HistoryAdapter;
import com.oxycast.chromecastapp.web.db.HistoryContract;
import com.oxycast.chromecastapp.web.db.WebBrowserDbHelper;

/**
 * Created by Sergey on 01.03.2017.
 */

public class HistoryDataSource implements HistoryAdapter.DataSource {

    WebBrowserDbHelper helper;

    public HistoryDataSource(Context context)
    {
         helper = new WebBrowserDbHelper(context);
    }

    @Override
    public Cursor getRowIds() {
        SQLiteDatabase db =helper.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT _id FROM " + HistoryContract.HistoryEntry.TABLE_NAME + " order by _id desc", new String[]{});;
       // db.close();
        return cur;
    }

    @Override
    public Cursor getRowById(long rowId) {
        SQLiteDatabase db =helper.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + HistoryContract.HistoryEntry.TABLE_NAME + " WHERE _id = ?", new String[]{Long.toString(rowId)});
        return cur;
    }

    @Override
    public void deleteRow(long rowId)
    {
        helper.deleteHistory(rowId);
    }

}
