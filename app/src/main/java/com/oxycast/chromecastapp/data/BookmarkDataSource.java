package com.oxycast.chromecastapp.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.oxycast.chromecastapp.adapters.BookmarkAdapter;
import com.oxycast.chromecastapp.web.db.BookmarkContract;
import com.oxycast.chromecastapp.web.db.WebBrowserDbHelper;

/**
 * Created by Sergey on 01.03.2017.
 */

public class BookmarkDataSource implements BookmarkAdapter.DataSource {
    WebBrowserDbHelper helper;

    public BookmarkDataSource(Context context)
    {
        helper = new WebBrowserDbHelper(context);
    }

    @Override
    public Cursor getRowIds() {
        SQLiteDatabase db =helper.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT _id FROM " + BookmarkContract.BookmarkEntry.TABLE_NAME + " WHERE "+ BookmarkContract.BookmarkEntry.COLUNM_NAME_QUICKACCESS + " = ?", new String[]{ "0"});;
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
    public void deleteRow(long rowId)
    {
        helper.deleteBookmark(rowId);
    }
}
