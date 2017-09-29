package com.oxycast.chromecastapp.web.db;

import android.provider.BaseColumns;

/**
 * Created by sergey on 01.03.17.
 */

public class BookmarkContract {
    public BookmarkContract() {}

    /* Inner class that defines the table contents */
    public static abstract class BookmarkEntry implements BaseColumns {
        public static final String TABLE_NAME = "Bookmarks";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUNM_NAME_DATE = "date";
        public static final String COLUNM_NAME_QUICKACCESS = "quickaccess";
        public static final String COLUNM_NAME_THUMBNAIL = "thumbnail";
    }
}
