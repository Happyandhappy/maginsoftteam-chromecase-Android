package com.oxycast.chromecastapp.cast;

/**
 * Created by Sergey on 27.03.2017.
 */

public interface ICastStatus {
    void onStopped();
    void onError(String title);
}
