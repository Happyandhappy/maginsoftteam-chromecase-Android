package com.oxycast.chromecastapp.web.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

/**
 * Created by sergey on 01.03.17.
 */

public class WebBrowserDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "ChromecastWeb.db";

    private static final String TEXT_TYPE = " TEXT";
    static final String DATE_TYPE = " INTEGER";
    static final String BLOB_TYPE = " BLOB";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_HISTORY =
            "CREATE TABLE " + HistoryContract.HistoryEntry.TABLE_NAME + " (" +
                    HistoryContract.HistoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    HistoryContract.HistoryEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    HistoryContract.HistoryEntry.COLUMN_NAME_URL + TEXT_TYPE + COMMA_SEP +
                    HistoryContract.HistoryEntry.COLUNM_NAME_DATE + DATE_TYPE + COMMA_SEP +
                    HistoryContract.HistoryEntry.COLUNM_NAME_THUMBNAIL + BLOB_TYPE + " NULL" +
                    ")";

    private static final String SQL_CREATE_BOOKMARKS =
            "CREATE TABLE " + BookmarkContract.BookmarkEntry.TABLE_NAME + " (" +
                    BookmarkContract.BookmarkEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    BookmarkContract.BookmarkEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    BookmarkContract.BookmarkEntry.COLUMN_NAME_URL + TEXT_TYPE + COMMA_SEP +
                    BookmarkContract.BookmarkEntry.COLUNM_NAME_DATE + DATE_TYPE + COMMA_SEP+
                    BookmarkContract.BookmarkEntry.COLUNM_NAME_QUICKACCESS + DATE_TYPE + COMMA_SEP+
                    BookmarkContract.BookmarkEntry.COLUNM_NAME_THUMBNAIL + BLOB_TYPE + " NULL" +
                    ")";

    private static final String SQL_CREATE_IPTV =
            "CREATE TABLE IF NOT EXISTS " + IptvContract.IptvEntry.TABLE_NAME + "(" +
                    IptvContract.IptvEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    IptvContract.IptvEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    IptvContract.IptvEntry.COLUMN_NAME_URL + TEXT_TYPE + COMMA_SEP +
                    IptvContract.IptvEntry.COLUNM_NAME_DATE + DATE_TYPE +

                    ")";

    public WebBrowserDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_HISTORY);
        db.execSQL(SQL_CREATE_BOOKMARKS);
        db.execSQL(SQL_CREATE_IPTV);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
       // db.execSQL(SQL_DELETE_ENTRIES);
       // onCreate(db);
        db.execSQL(SQL_CREATE_IPTV);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //onUpgrade(db, oldVersion, newVersion);
    }

    public long addIptv(String title, String url)
    {
        long unixTime = System.currentTimeMillis() / 1000L;
        SQLiteDatabase db = this.getWritableDatabase();
// Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(IptvContract.IptvEntry.COLUMN_NAME_TITLE, title);
        values.put( IptvContract.IptvEntry.COLUMN_NAME_URL, url);
        values.put( IptvContract.IptvEntry.COLUNM_NAME_DATE, unixTime);

        long newRowId;
        newRowId = db.insert(
                IptvContract.IptvEntry.TABLE_NAME,
                null,
                values);

        return newRowId;
    }

    public void deleteIptv(String url)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define 'where' part of query.
        String selection =  IptvContract.IptvEntry.COLUMN_NAME_URL + " = ?";
// Specify arguments in placeholder order.
        String[] selectionArgs = { url };
// Issue SQL statement.
        db.delete( IptvContract.IptvEntry.TABLE_NAME, selection, selectionArgs);

        db.close();
    }

    public void deleteIptv(long id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define 'where' part of query.
        String selection =  IptvContract.IptvEntry._ID + " = ?";
// Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(id) };
// Issue SQL statement.
        db.delete( IptvContract.IptvEntry.TABLE_NAME, selection, selectionArgs);

        db.close();
    }

    public long addHistory(String title, String url, Bitmap bitmap)
    {
        long unixTime = System.currentTimeMillis() / 1000L;
        SQLiteDatabase db = this.getWritableDatabase();

// Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(HistoryContract.HistoryEntry.COLUMN_NAME_TITLE, title);
        values.put( HistoryContract.HistoryEntry.COLUMN_NAME_URL, url);
        values.put( HistoryContract.HistoryEntry.COLUNM_NAME_DATE, unixTime);

        if(bitmap!=null)
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            values.put( HistoryContract.HistoryEntry.COLUNM_NAME_THUMBNAIL,byteArray);
        }
// Insert the new row, returning the primary key value of the new row

        long newRowId;
        newRowId = db.insert(
                     HistoryContract.HistoryEntry.TABLE_NAME,
                HistoryContract.HistoryEntry.COLUNM_NAME_THUMBNAIL,
                     values);

        return newRowId;
    }

    public long addBookmark(String title,String url,boolean quickaccess,Bitmap thumbnail)
    {
        long unixTime = System.currentTimeMillis() / 1000L;
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(BookmarkContract.BookmarkEntry.COLUMN_NAME_TITLE, title);
        values.put( BookmarkContract.BookmarkEntry.COLUMN_NAME_URL, url);
        values.put(BookmarkContract.BookmarkEntry.COLUNM_NAME_DATE, unixTime);
        values.put(BookmarkContract.BookmarkEntry.COLUNM_NAME_QUICKACCESS,quickaccess);

        if(thumbnail!=null)
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            values.put( HistoryContract.HistoryEntry.COLUNM_NAME_THUMBNAIL, byteArray);
        }
        // Insert the new row, returning the primary key value of the new row

        long newRowId;
        newRowId = db.insert(
                BookmarkContract.BookmarkEntry.TABLE_NAME,
                BookmarkContract.BookmarkEntry.COLUNM_NAME_THUMBNAIL,
                values);

        return newRowId;
    }

    public void deleteBookmark(String url)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define 'where' part of query.
        String selection =  BookmarkContract.BookmarkEntry.COLUMN_NAME_URL + " = ? AND " + BookmarkContract.BookmarkEntry.COLUNM_NAME_QUICKACCESS + " = 0" ;
// Specify arguments in placeholder order.
        String[] selectionArgs = { url };
// Issue SQL statement.
        db.delete( BookmarkContract.BookmarkEntry.TABLE_NAME, selection, selectionArgs);

        db.close();
    }

    public void deleteBookmark(long id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define 'where' part of query.
        String selection =  BookmarkContract.BookmarkEntry._ID + " = ?";
// Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(id) };
// Issue SQL statement.
        db.delete( BookmarkContract.BookmarkEntry.TABLE_NAME, selection, selectionArgs);

        db.close();
    }

    public void deleteQuickAccess(String url)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define 'where' part of query.
        String selection =  BookmarkContract.BookmarkEntry.COLUMN_NAME_URL + " = ? AND " + BookmarkContract.BookmarkEntry.COLUNM_NAME_QUICKACCESS + " = 1" ;
// Specify arguments in placeholder order.
        String[] selectionArgs = { url };
// Issue SQL statement.
        db.delete( BookmarkContract.BookmarkEntry.TABLE_NAME, selection, selectionArgs);

        db.close();
    }

    public void clearHistory()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define 'where' part of query.
        String selection =  HistoryContract.HistoryEntry._ID + " >= 0";
// Specify arguments in placeholder order.

// Issue SQL statement.
        db.delete( HistoryContract.HistoryEntry.TABLE_NAME, selection, null);

        db.close();
    }

    public boolean isBookmarkExist(String url)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor= db.rawQuery("SELECT COUNT (*) FROM " + BookmarkContract.BookmarkEntry.TABLE_NAME + " WHERE " + BookmarkContract.BookmarkEntry.COLUMN_NAME_URL + "=? AND " + BookmarkContract.BookmarkEntry.COLUNM_NAME_QUICKACCESS + " = 0",
                new String[] { url });
        int count = 0;
        if(null != cursor)
            if(cursor.getCount() > 0){
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }
        cursor.close();
        db.close();

        if(count>0)
        {
            return true;
        }

        return false;
    }

    public boolean isQuickAccessExist(String url)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor= db.rawQuery("SELECT COUNT (*) FROM " + BookmarkContract.BookmarkEntry.TABLE_NAME + " WHERE " + BookmarkContract.BookmarkEntry.COLUMN_NAME_URL + "=? AND " + BookmarkContract.BookmarkEntry.COLUNM_NAME_QUICKACCESS + " = 1",
                new String[] { url });
        int count = 0;
        if(null != cursor)
            if(cursor.getCount() > 0){
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }
        cursor.close();
        db.close();

        if(count>0)
        {
            return true;
        }

        return false;
    }

    public boolean isHistroryExist(String url)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor= db.rawQuery("SELECT COUNT (*) FROM " + HistoryContract.HistoryEntry.TABLE_NAME + " WHERE " + HistoryContract.HistoryEntry.COLUMN_NAME_URL + "=?",
                new String[] { url });
        int count = 0;
        if(null != cursor)
            if(cursor.getCount() > 0){
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }
        cursor.close();
        db.close();

        if(count>0)
        {
            return true;
        }

        return false;
    }

    public void deleteHistory(String url)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define 'where' part of query.
        String selection =  HistoryContract.HistoryEntry.COLUMN_NAME_URL + " = ?";
// Specify arguments in placeholder order.
        String[] selectionArgs = { url };
// Issue SQL statement.
        db.delete( HistoryContract.HistoryEntry.TABLE_NAME, selection, selectionArgs);

        db.close();
    }

    public void deleteHistory(long id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // Define 'where' part of query.
        String selection =  HistoryContract.HistoryEntry._ID + " = ?";
// Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(id) };
// Issue SQL statement.
        db.delete( HistoryContract.HistoryEntry.TABLE_NAME, selection, selectionArgs);

        db.close();
    }
}
