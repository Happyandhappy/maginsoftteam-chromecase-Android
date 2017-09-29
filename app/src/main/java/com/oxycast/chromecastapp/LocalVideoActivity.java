package com.oxycast.chromecastapp;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.oxycast.chromecastapp.adapters.LocalVideoAdapter;
import com.oxycast.chromecastapp.adapters.RLocalVideoAdapter;
import com.oxycast.chromecastapp.app.ChromecastApp;
import com.oxycast.chromecastapp.cast.CastVideo;
import com.oxycast.chromecastapp.cast.ICastStatus;
import com.oxycast.chromecastapp.logger.LoggerWrapper;
import com.oxycast.chromecastapp.media.Video;
import com.oxycast.chromecastapp.utils.AdProvider;
import com.oxycast.chromecastapp.utils.Utils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

import fi.iki.elonen.SimpleWebServer;

public class LocalVideoActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, SearchView.OnQueryTextListener, ICastStatus {
    private CastStateListener mCastStateListener;


    ListView videoList;
    LocalVideoAdapter adapter = null;
    private static final int PORT = 8065;
    private static int [] PORT2 = {8080,8066,8090,9000,9010,9500,9900,10000, 11000,12000};
    SimpleWebServer server = null;
    CastVideo castVideo = null;
    String playAfterConnect = null;
    ArrayList<String> videoArray = null;
    LoggerWrapper logger = null;
    private AdView mAdView;

    private RecyclerView recyclerView;
    private RLocalVideoAdapter videoAdapter;
    private GridLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_video);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(MainActivity.getAppTitle(this,getResources().getString(R.string.app_name)));

        ChromecastApp.Instance().mCastContext = CastContext.getSharedInstance(this);
        mAdView = (AdView) findViewById(R.id.adView);
        showAd();
        recyclerView = (RecyclerView)findViewById(R.id.recyclerview);

        videoList = (ListView) findViewById(R.id.video_list);
        videoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                if(mediaRouteMenuItem.isVisible() == true) {

                    if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {

                        final String video = parent.getItemAtPosition(position).toString();
                        //final String ip = getIp();
                        Cast(video,"");
                     /*   Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Cast(video,ip);
                            }
                        });*/
                        //   thread.start();

                    } else {
                        if (mediaRouteMenuItem != null) {
                            ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                            String video = parent.getItemAtPosition(position).toString();
                            playAfterConnect = video;
                            provider.onPerformDefaultAction();
                        }
                        //Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    //String video = parent.getItemAtPosition(position).toString();
                    //TestCast(video);
                    if(!((Activity) LocalVideoActivity.this).isFinishing()) {
                        Toast.makeText(LocalVideoActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    //showIntroductoryOverlay();
                }

                if(newState == CastState.CONNECTED)
                {
                    if(!TextUtils.isEmpty(playAfterConnect))
                    {
                        //    final String ip = getIp();
                        //   Thread thread = new Thread(new Runnable() {
                        //       @Override
                        ///  public void run() {
                        Cast(playAfterConnect,"");
                        playAfterConnect = null;
                        //      }});
                        // thread.start();
                    }
                }

            }
        };

        if(Build.VERSION.SDK_INT>=23)
        {
            requestStoragePermission();
        }
        else {
            logger = new LoggerWrapper( LocalVideoActivity.class );
            videoArray = getAllMedia();

            if(videoList!=null)
            {
                adapter = new LocalVideoAdapter(this,videoArray);
                adapter.setLogger(logger);
                videoList.setAdapter(adapter);

                setupVideoAdapter();

            }
            // Toast.makeText(this, "Finded video: " + videoArray.size(), Toast.LENGTH_LONG).show();
            for (String video: videoArray) {
                Log.d("LocalVideo", "Video: " + video);
                logger.debug("Video: " + video);
            }
        }
    }
    public void setupVideoAdapter(){
        videoAdapter = new RLocalVideoAdapter(this, videoArray);
        recyclerView.setAdapter(videoAdapter);
        recyclerView.setHasFixedSize(true);
        layoutManager = new GridLayoutManager(this, 2, 1, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }
    public void listClick(String video){


        if(mediaRouteMenuItem.isVisible() == true) {

            if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                //final String ip = getIp();
                Cast(video,"");
                     /*   Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Cast(video,ip);
                            }
                        });*/
                //   thread.start();

            } else {
                if (mediaRouteMenuItem != null) {
                    ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                    playAfterConnect = video;
                    provider.onPerformDefaultAction();
                }
                //Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
            }
        }
        else{
            //String video = parent.getItemAtPosition(position).toString();
            //TestCast(video);
            if(!((Activity) LocalVideoActivity.this).isFinishing()) {
                Toast.makeText(LocalVideoActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
            }
        }
    }
    private void showAd() {
        Log.v("suraj","showAd called");
        if (!AdProvider.isAdFree){
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.GONE);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mAdView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    mAdView.setVisibility(View.GONE);
                }
            });
        }else{
            mAdView.setVisibility(View.GONE);
        }
    }
    @Override
    protected void onPause() {
        ChromecastApp.Instance().mCastContext.removeCastStateListener(mCastStateListener);
        super.onPause();
    }

    @Override
    protected void onResume() {
        ChromecastApp.Instance().mCastContext.addCastStateListener(mCastStateListener);

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d("Chromecast", "Destroy");

        super.onDestroy();
        if(server!=null)
        {
            server.stop();
        }
    }

    private boolean isHotspot() {
        boolean isWifiAPenabled = false;

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Method[] wmMethods = wifi.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {

                try {
                    isWifiAPenabled = (boolean)method.invoke(wifi);
                    break;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return isWifiAPenabled;
    }

    private void TestCast(String video) {

        String formatedIpAddress = "";
        if(isHotspot() == false) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

            logger.info("Is external wifi");
        }
        else{
            logger.info("Is Hotspot");
            //  CastSession mCastSession = ChromecastApp.Instance().mCastContext.getSessionManager().getCurrentCastSession();
            String deviceAddress = ""; //mCastSession.getCastDevice().getIpAddress().getHostAddress();
            if(TextUtils.isEmpty(deviceAddress)==true)
            {
                formatedIpAddress = "192.168.43.1";
            }
            else{
                String[] arrayDeviceAddress = deviceAddress.split(".");
                if(arrayDeviceAddress.length == 4)
                {
                    formatedIpAddress = arrayDeviceAddress[0] + "." + arrayDeviceAddress[1] + "." + arrayDeviceAddress[2] + ".1";
                }
                else{
                    formatedIpAddress = "192.168.43.1";
                }
            }

        }

        Log.d("NanoHttpd", "Please access! http://" + formatedIpAddress + ":" + PORT);
        logger.info("Please access! http://" + formatedIpAddress + ":" + PORT);
        File videoFile = new File(video);
        String workingDir = videoFile.getParent();

        Log.d("NanoHttpd", "Working directory: " + workingDir);
        logger.info("Working directory: " + workingDir);
        // ServerRunner.executeInstance(new SimpleWebServer(formatedIpAddress, PORT, videoFile.getParentFile(), false));
        if (server != null) {
            server.stop();
        }
        int currentPort = PORT;
        boolean iswebserveropen = false;
        for (int i = 0; i < 10; i++)
        {
            currentPort = PORT2[i];
            Crashlytics.log("Address " + formatedIpAddress + " PORT " + currentPort);

            logger.info("Address " + formatedIpAddress + " PORT " + currentPort);
            // server = new SimpleWebServer(formatedIpAddress, currentPort, videoFile.getParentFile(), false, "*");

            server = new SimpleWebServer(formatedIpAddress, currentPort, new File("/"), true, "*");
            try {
                server.start();

                iswebserveropen = server.isAlive();

                break;
            } catch (Exception ex) {

                Crashlytics.log("Address " + formatedIpAddress + " PORT " + currentPort + " " + ex.getMessage() + " | " + ex.getStackTrace() );
                logger.error("Address " + formatedIpAddress + " PORT " + currentPort,ex);
            }
        }
        ChromecastApp.localWebServer = "http://" + formatedIpAddress + ":" + currentPort;
        String url = "";
        try {
            url = "http://" + formatedIpAddress + ":" + currentPort + videoFile.getAbsolutePath().replace(" ","%20");
        }
        catch (Exception ex)
        {
            logger.error("Error make video link: ", ex);
        }
        if(iswebserveropen == false)
        {
            Toast.makeText(this,getString(R.string.webservernotopen),Toast.LENGTH_LONG).show();
            return;
        }
        else{
            Toast.makeText(this,"Web server is started. URL: " +  url ,Toast.LENGTH_LONG).show();
        }
        //String url = "http://" + formatedIpAddress + ":" + currentPort + "/" + videoFile.getAbsolutePath();
        Log.d("NanoHttpd","Please access! " + url);
        logger.info("Please access! " + url);
        String mime = getMimeType("file://" + video);
        Log.d("NanoHttpd", "Mime: " + mime);
        logger.info("Mime: " + mime);

        Video cVideo = new Video(videoFile.getName(),url);
        cVideo.setLocalPath(video);
        String fileNameWithOutExt = FilenameUtils.removeExtension(videoFile.getName());

        /*File subtitlevtt = new File(workingDir, fileNameWithOutExt + ".vtt");
        if(subtitlevtt.exists() == true) {
            cVideo.setSubtitle( "http://" + formatedIpAddress + ":" + currentPort + videoFile.getParent() +"/" + fileNameWithOutExt + ".vtt");
        }

        File subtitlettml = new File(workingDir, fileNameWithOutExt + ".ttml");
        if(subtitlettml.exists() == true) {
            cVideo.setSubtitle( "http://" + formatedIpAddress + ":" + currentPort + videoFile.getParent() + "/" + fileNameWithOutExt + ".ttml");
        }

        subtitlettml = new File(workingDir, fileNameWithOutExt + ".dfxp");
        if(subtitlettml.exists() == true) {
            cVideo.setSubtitle( "http://" + formatedIpAddress + ":" + currentPort + videoFile.getParent() + "/" + fileNameWithOutExt + ".dfxp");
        }

        subtitlettml = new File(workingDir, fileNameWithOutExt + ".xml");
        if(subtitlettml.exists() == true) {
            cVideo.setSubtitle( "http://" + formatedIpAddress + ":" + currentPort + videoFile.getParent() + "/" + fileNameWithOutExt + ".xml");
        }*/

        cVideo.setMimeType(mime);

        if(videoArray.size()>1)
        {
            ArrayList<Video> othervideo = new ArrayList<>();
            for(String vdo:videoArray)
            {
                if(!vdo.equalsIgnoreCase(video))
                {
                    othervideo.add(Utils.prepareVideo(vdo,"http://" + formatedIpAddress + ":" + currentPort));
                }
            }

            if(othervideo.size()>0)
            {
                cVideo.setOtherVideo(othervideo);
            }
        }
        ChromecastApp.currentVideo = cVideo;
        // castVideo.Cast(cVideo);
    }

    private String getIp()
    {
        String formatedIpAddress = "";
        if(isHotspot() == false) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

            logger.info("Is external wifi");
        }
        else{
            logger.info("Is Hotspot");
            CastSession mCastSession = ChromecastApp.Instance().mCastContext.getSessionManager().getCurrentCastSession();
            String deviceAddress = mCastSession.getCastDevice().getIpAddress().getHostAddress();
            if(TextUtils.isEmpty(deviceAddress)==true)
            {
                formatedIpAddress = "192.168.43.1";
            }
            else{
                String[] arrayDeviceAddress = deviceAddress.split(".");
                if(arrayDeviceAddress.length == 4)
                {
                    formatedIpAddress = arrayDeviceAddress[0] + "." + arrayDeviceAddress[1] + "." + arrayDeviceAddress[2] + ".1";
                }
                else{
                    formatedIpAddress = "192.168.43.1";
                }
            }

        }

        return formatedIpAddress;
    }

    private void Cast(String video, String formatedIpAddress) {
        sendMessage(0,null);
        if(isHotspot() == false) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

            logger.info("Is external wifi");
        }
        else{
            logger.info("Is Hotspot");
            CastSession mCastSession = ChromecastApp.Instance().mCastContext.getSessionManager().getCurrentCastSession();
            String deviceAddress = mCastSession.getCastDevice().getIpAddress().getHostAddress();
            if(TextUtils.isEmpty(deviceAddress)==true)
            {
                formatedIpAddress = "192.168.43.1";
            }
            else{
                String[] arrayDeviceAddress = deviceAddress.split(".");
                if(arrayDeviceAddress.length == 4)
                {
                    formatedIpAddress = arrayDeviceAddress[0] + "." + arrayDeviceAddress[1] + "." + arrayDeviceAddress[2] + ".1";
                }
                else{
                    formatedIpAddress = "192.168.43.1";
                }
            }

        }

        Log.d("NanoHttpd", "Please access! http://" + formatedIpAddress + ":" + PORT);
        logger.info("Please access! http://" + formatedIpAddress + ":" + PORT);
        File videoFile = new File(video);
        String workingDir = videoFile.getParent();

        Log.d("NanoHttpd", "Working directory: " + workingDir);
        logger.info("Working directory: " + workingDir);
        // ServerRunner.executeInstance(new SimpleWebServer(formatedIpAddress, PORT, videoFile.getParentFile(), false));
        if (server != null) {
            server.stop();
        }
        int currentPort = PORT;
        boolean iswebserveropen = false;
        for (int i = 0; i < 10; i++)
        {
            currentPort = PORT2[i];
            Crashlytics.log("Address " + formatedIpAddress + " PORT " + currentPort);

            logger.info("Address " + formatedIpAddress + " PORT " + currentPort);
            // server = new SimpleWebServer(formatedIpAddress, currentPort, videoFile.getParentFile(), false, "*");

            server = new SimpleWebServer(formatedIpAddress, currentPort, new File("/"), false, "*");
            try {
                server.start();

                iswebserveropen = server.isAlive();

                break;
            } catch (Exception ex) {

                Crashlytics.log("Address " + formatedIpAddress + " PORT " + currentPort + " " + ex.getMessage() + " | " + ex.getStackTrace() );
                logger.error("Address " + formatedIpAddress + " PORT " + currentPort,ex);
            }
        }
        ChromecastApp.localWebServer = "http://" + formatedIpAddress + ":" + currentPort;
        String url = "http://" + formatedIpAddress + ":" + currentPort +  Utils.preparedVideoPath(videoFile);  //videoFile.getAbsolutePath().replace(" ", "%20");

        if(iswebserveropen == false)
        {
            //Toast.makeText(this,getString(R.string.webservernotopen),Toast.LENGTH_LONG).show();
            sendMessage(2,null);
            return;
        }
        else{
            //Toast.makeText(this,"Web server is started. URL: " +  url ,Toast.LENGTH_LONG).show();
        }
        //String url = "http://" + formatedIpAddress + ":" + currentPort + "/" + videoFile.getAbsolutePath();
        Log.d("NanoHttpd","Please access! " + url);
        logger.info("Please access! " + url);
        String mime = getMimeType("file://" + video);
        Log.d("NanoHttpd", "Mime: " + mime);
        logger.info("Mime: " + mime);

        Video cVideo = new Video(videoFile.getName(),url);
        cVideo.setLocalPath(video);
        String fileNameWithOutExt = FilenameUtils.removeExtension(videoFile.getName());

        File subtitlevtt = new File(workingDir, fileNameWithOutExt + ".vtt");
        if(subtitlevtt.exists() == true) {
            cVideo.setSubtitle( "http://" + formatedIpAddress + ":" + currentPort + Utils.preparedVideoPath(subtitlevtt));
        }

        File subtitlettml = new File(workingDir, fileNameWithOutExt + ".ttml");
        if(subtitlettml.exists() == true) {

            cVideo.setSubtitle( "http://" + formatedIpAddress + ":" + currentPort + Utils.preparedVideoPath(subtitlettml));
            Log.d("Subtitle", "URL: " + "http://" + formatedIpAddress + ":" + currentPort + Utils.preparedVideoPath(subtitlettml));
        }

        subtitlettml = new File(workingDir, fileNameWithOutExt + ".dfxp");
        if(subtitlettml.exists() == true) {
            cVideo.setSubtitle( "http://" + formatedIpAddress + ":" + currentPort + Utils.preparedVideoPath(subtitlettml));
        }

        subtitlettml = new File(workingDir, fileNameWithOutExt + ".xml");
        if(subtitlettml.exists() == true) {
            cVideo.setSubtitle( "http://" + formatedIpAddress + ":" + currentPort + Utils.preparedVideoPath(subtitlettml));

        }

        cVideo.setMimeType(mime);

        if(videoArray.size()>1)
        {
            ArrayList<Video> othervideo = new ArrayList<>();
            for(String vdo:videoArray)
            {
                if(!vdo.equalsIgnoreCase(video))
                {
                    othervideo.add(Utils.prepareVideo(vdo,"http://" + formatedIpAddress + ":" + currentPort));
                }
            }

            if(othervideo.size()>0)
            {
                cVideo.setOtherVideo(othervideo);
            }
        }
        ChromecastApp.currentVideo = cVideo;

        sendMessage(1,cVideo);
    }

    private void sendMessage(int status,Video video)
    {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putInt("status",status);
        if(video!=null){
            bundle.putSerializable("video",video);
        }
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            Bundle res = msg.getData();
            final int status = res.getInt("status");
            ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
            if (bar != null) {
                if (status == 0) {
                    bar.setVisibility(View.VISIBLE);
                }
                if (status == 1) {

                    Video video = (Video) res.getSerializable("video");
                    castVideo = new CastVideo(LocalVideoActivity.this);
                    Log.d("PeformanceCast","start");
                    castVideo.setStatusListener(LocalVideoActivity.this);
                    castVideo.Cast(video);
                    Log.d("PeformanceCast","end");
                    bar.setVisibility(View.INVISIBLE);
                }

                if(status == 2)
                {
                    bar.setVisibility(View.INVISIBLE);
                }
            }
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            logger = new LoggerWrapper( LocalVideoActivity.class );
            //Toast.makeText(this,"Permission is granted",Toast.LENGTH_LONG).show();
            videoArray = getAllMedia();
            if(videoList!=null)
            {
                adapter = new LocalVideoAdapter(this,videoArray);
                adapter.setLogger(logger);
                videoList.setAdapter(adapter);
                setupVideoAdapter();
            }
            //   Toast.makeText(this, "Finded video: " + videoArray.size(), Toast.LENGTH_LONG).show();

            for (String video: videoArray) {
                Log.d("LocalVideo", "Video: " + video);
            }
        }
        else{

            //Toast.makeText(this,"Permission don't granted",Toast.LENGTH_LONG).show();
        }
    }

    private void requestStoragePermission()
    {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        int grant = ContextCompat.checkSelfPermission(this,permission);

        String permission1 = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int grant1 = ContextCompat.checkSelfPermission(this,permission1);


        if(grant != PackageManager.PERMISSION_GRANTED || grant1 != PackageManager.PERMISSION_GRANTED)
        {
            String[] pl = new String[2];
            pl[0] = permission;
            pl[1] = permission1;
            ActivityCompat.requestPermissions(this, pl,1);

        }
        else{
            logger = new LoggerWrapper( LocalVideoActivity.class );
            videoArray = getAllMedia();
            if(videoList!=null)
            {
                adapter = new LocalVideoAdapter(this,videoArray);
                adapter.setLogger(logger);
                videoList.setAdapter(adapter);
                setupVideoAdapter();
            }
            //   Toast.makeText(this, "Finded video: " + videoArray.size(), Toast.LENGTH_LONG).show();

            for (String video: videoArray) {
                Log.d("LocalVideo", "Video: " + video);
                logger.debug("Video: " + video);
            }
        }
    }

    public ArrayList<String> getAllMedia() {
        HashSet<String> videoItemHashSet = new HashSet<>();
        String[] projection = { MediaStore.Video.VideoColumns.DATA ,MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.MIME_TYPE};
        Cursor cursor = this.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
        try {
            cursor.moveToFirst();
            do{
                String mime = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));

                if(mime.contains("video/mp4") || mime.contains("video/webm") || mime.contains("video/x-matroska") || mime.contains("video/avi")) {
                    videoItemHashSet.add((cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))));
                }
            }while(cursor.moveToNext());

            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<String> downloadedList = new ArrayList<>(videoItemHashSet);
        return downloadedList;
    }

    private MenuItem mediaRouteMenuItem;
    SearchView searchView;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.local_video_menu, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(this);

        searchView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    String text = searchView.getQuery().toString();

                    ArrayList<String> temp = new ArrayList<String>();
                    if(TextUtils.isEmpty(text)) {
                        temp.addAll(videoArray);
                    }
                    else {
                        for (String video : videoArray) {
                            if (video.toLowerCase(Locale.ENGLISH).contains(text.toLowerCase(Locale.ENGLISH))) {
                                temp.add(video);
                            }
                        }
                    }
                    adapter.updateVideoList(temp);
                    videoAdapter.updateVideoList(temp);
                }
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.refresh:
                videoArray = getAllMedia();
                searchView.setQuery("",false);
                if(adapter!=null){
                    adapter.updateVideoList(videoArray);
                    videoAdapter.updateVideoList(videoArray);
                }
                else{
                    adapter = new LocalVideoAdapter(this,videoArray);
                    adapter.setLogger(logger);
                    if(videoList!=null) {
                        videoList.setAdapter(adapter);
                        setupVideoAdapter();
                    }
                }
                return true;
            default:
                return false;

        }
    }

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (!TextUtils.isEmpty(extension)) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        else{
            logger.info("Extension is null. " + url);
            int index = url.lastIndexOf(".");
            extension = url.substring(index+1);
            if (!TextUtils.isEmpty(extension)) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }
            else{
                logger.info("Unexpected error to receive mime. Use default video/mp4. " + url);
                type = "video/mp4";
            }
        }

        return type;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d("SearchView", "Query = " + query + " : submitted");
        ArrayList<String> temp = new ArrayList<String>();

        if (TextUtils.isEmpty(query)) {
            temp.addAll(videoArray);
        } else {
            for (String video : videoArray) {
                if (video.toLowerCase(Locale.ENGLISH).contains(query.toLowerCase(Locale.ENGLISH))) {
                    temp.add(video);
                }
            }
        }

        adapter.updateVideoList(temp);
        videoAdapter.updateVideoList(temp);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d("SearchView","Query = " + newText);

        ArrayList<String> temp = new ArrayList<String>();
        if (TextUtils.isEmpty(newText)) {
            if(videoArray!=null) {
                temp.addAll(videoArray);
            }
        } else {
            if(videoArray!=null) {
                for (String video : videoArray) {
                    if (video.toLowerCase(Locale.ENGLISH).contains(newText.toLowerCase(Locale.ENGLISH))) {
                        temp.add(video);
                    }
                }
            }
        }

        adapter.updateVideoList(temp);
        videoAdapter.updateVideoList(temp);
        return false;
    }

    @Override
    public void onStopped() {

    }

    @Override
    public void onError(String title) {
        if (! this.isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(LocalVideoActivity.this);
            builder.setTitle(R.string.iptv_unable_cast_title)
                    .setMessage(R.string.iptv_unable_cast)
                    .setCancelable(true)
                    .setNegativeButton("ОК",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}
