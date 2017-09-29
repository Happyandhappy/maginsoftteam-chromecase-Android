package com.oxycast.chromecastapp.utils;

/**
 * Created by oxytouch on 01-08-2017.
 */

public class AdProvider {
    public static boolean isAdFree;

    public static void setAsAdFree(boolean isAdFree) {
        AdProvider.isAdFree = isAdFree;
    }

    public static boolean isAdFree() {
        return isAdFree;
    }
}
