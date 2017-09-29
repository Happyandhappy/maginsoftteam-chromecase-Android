package com.oxycast.chromecastapp.services;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class ProxyService extends Service {
    ProxyHandler proxy = null;

    private static int PORT = 9500;

    public ProxyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent!=null) {
            String proxyaddress = intent.getStringExtra("proxy");
            String host = intent.getStringExtra("host");
            String useragent = intent.getStringExtra("useragent");
            PORT = intent.getIntExtra("port", 9500);

            //  for (int i = 0; i < 10; i++) {
            int currentPort = PORT;
            //    proxy = new ProxyHandler(currentPort,proxyaddress,host,useragent);
            try {
                String ip = intent.getStringExtra("IP");
                proxy = new ProxyHandler(currentPort, proxyaddress, host, useragent, ip);
                proxy.start();
            } catch (Exception ex) {
                Log.e("ProxyService", ex.getMessage() + "\r\n" + ex.getStackTrace());
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(proxy!=null) {
            proxy.stop();
            proxy = null;
        }
    }
}
