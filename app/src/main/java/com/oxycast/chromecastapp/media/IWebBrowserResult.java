package com.oxycast.chromecastapp.media;

import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebChromeClient;

import java.util.ArrayList;

/**
 * Created by sergey on 25.02.17.
 */

public interface IWebBrowserResult {
        void FindVideo(Video video);
        void FindVideos(ArrayList<Video> video);
        void SetTitle(String title);
        void setHistory(String str);
        void setUrl(String str);
        void setBitmap(Bitmap bitmap);
        void showToast(String message);
        void m1954a(View view, int i, WebChromeClient.CustomViewCallback customViewCallback);

        void m1955a(View view, WebChromeClient.CustomViewCallback customViewCallback);
}
