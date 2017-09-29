package com.oxycast.chromecastapp.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.oxycast.chromecastapp.adapters.QuickAccessAdapter;
import com.oxycast.chromecastapp.web.db.BookmarkContract;
import com.oxycast.chromecastapp.web.db.WebBrowserDbHelper;

/**
 * Created by sergey on 03.03.17.
 */

public class QuickAccessDataSource implements QuickAccessAdapter.DataSource {
    WebBrowserDbHelper helper;

    public QuickAccessDataSource(Context context)
    {
        helper = new WebBrowserDbHelper(context);
    }

    @Override
    public Cursor getRowIds() {
        SQLiteDatabase db =helper.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT _id FROM " + BookmarkContract.BookmarkEntry.TABLE_NAME + " WHERE "+ BookmarkContract.BookmarkEntry.COLUNM_NAME_QUICKACCESS + " = ?" + " LIMIT 8;", new String[]{ "1"});;
        // db.close();
        return cur;
    }

    @Override
    public Cursor getRowById(long rowId) {
        SQLiteDatabase db =helper.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + BookmarkContract.BookmarkEntry.TABLE_NAME + " WHERE _id = ?", new String[]{Long.toString(rowId)});
        return cur;
    }

    @Override
    public void deleteRow(long rowId) {
        helper.deleteHistory(rowId);
    }
}
