package com.oxycast.chromecastapp.web.db;

import android.provider.BaseColumns;

/**
 * Created by sergey on 15.04.17.
 */

public class IptvContract {
    public IptvContract() {}

    /* Inner class that defines the table contents */
    public static abstract class IptvEntry implements BaseColumns {
        public static final String TABLE_NAME = "Iptv";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUNM_NAME_DATE = "date";
    }
}
