package com.oxycast.chromecastapp.Ads;

import android.content.Context;

import com.oxycast.chromecastapp.utils.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sergey on 25.02.17.
 */

public class AdsBlockHelper {
    private static Set<String> ads_host = new HashSet<String>();
    private static Set<String> whites_host = new HashSet<String>();
    private static Set<String> blacks_host = new HashSet<String>();

    static class ReadFileAsync implements Runnable {
        Context context;
        String file;
        Set set;

        ReadFileAsync(Context context, String str, Set set) {
            this.context = context;
            this.file = str;
            this.set = set;
        }

        public void run() {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.context.getAssets().open(this.file)));
                while (true) {
                    String readLine = bufferedReader.readLine();
                    if (readLine != null) {
                        this.set.add(readLine.toLowerCase());
                    } else {
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    Context context;
    public AdsBlockHelper(Context context) {
      //  VideoCastApp.m2121c().m2126a(this);
        this.context = context;
        if (ads_host.isEmpty()) {
            AdsBlockHelper.ReadFile(context, ads_host, "ads_host.txt");
        }
        if (whites_host.isEmpty()) {
            AdsBlockHelper.ReadFile(context, whites_host, "whites_host.txt");
        }
        if (blacks_host.isEmpty()) {
            AdsBlockHelper.ReadFile(context, blacks_host, "blacks_host.txt");
        }

    }

    private static void ReadFile(Context context, Set<String> set, String str) {
        new Thread(new ReadFileAsync(context, str, set)).start();
    }

   public  boolean isAdsHost(String str) {
        String b = Utils.getHost(str);
        return Utils.isAdsBlocker(context) && b != null && ads_host.contains(b.toLowerCase());
        //return false;
    }

    public boolean isInWhiteList(String str) {
         String b = Utils.getHost(str);
       // return b == null || whites_host.contains(b.toLowerCase());
        return true;
    }

    public boolean isInBlackList(String str)
    {
        String b = Utils.getHost(str);
        return b == null || blacks_host.contains(b.toLowerCase());
    }
}

