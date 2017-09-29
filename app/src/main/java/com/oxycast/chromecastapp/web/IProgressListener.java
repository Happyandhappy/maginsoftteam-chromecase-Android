package com.oxycast.chromecastapp.web;

/**
 * Created by sergey on 26.02.17.
 */

public interface IProgressListener {
    void onUpdateProgress(int progress);
    void onPageFinished();
    void onPageStarted();
}
