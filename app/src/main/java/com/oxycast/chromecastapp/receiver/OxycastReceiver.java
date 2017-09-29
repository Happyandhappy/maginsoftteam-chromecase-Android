package com.oxycast.chromecastapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.oxycast.chromecastapp.MainActivity;
import com.oxycast.chromecastapp.WebViewActivity;

public class OxycastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startintent = new Intent(context, MainActivity.class);
        startintent.putExtra("link", intent.getData());
        startintent.putExtra("mime",intent.getType());
        startintent.putExtra("isexternal",true);
        context.startActivity(startintent);
    }
}
