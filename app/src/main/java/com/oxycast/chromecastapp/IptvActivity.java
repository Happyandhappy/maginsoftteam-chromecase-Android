package com.oxycast.chromecastapp;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.oxycast.chromecastapp.adapters.IptvAdapter;
import com.oxycast.chromecastapp.app.ChromecastApp;
import com.oxycast.chromecastapp.cast.CastVideo;
import com.oxycast.chromecastapp.cast.ICastStatus;
import com.oxycast.chromecastapp.data.IptvDataSource;
import com.oxycast.chromecastapp.iptv.M3UItem;
import com.oxycast.chromecastapp.iptv.M3UParser;
import com.oxycast.chromecastapp.iptv.M3UPlaylist;
import com.oxycast.chromecastapp.logger.LoggerWrapper;
import com.oxycast.chromecastapp.media.Video;
import com.oxycast.chromecastapp.utils.AdProvider;
import com.oxycast.chromecastapp.utils.FileManager;
import com.oxycast.chromecastapp.utils.Utils;
import com.oxycast.chromecastapp.web.db.WebBrowserDbHelper;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IptvActivity extends AppCompatActivity implements ICastStatus {
    WebBrowserDbHelper dbHelper;
    IptvAdapter adapter;
    Video playAfterConnect;
    private CastStateListener mCastStateListener;
    private final int GET_FILE_DIALOG_CODE = 1005;
    private EditText addressET = null;
    private EditText titleET = null;
    M3UParser parser = new M3UParser();
    LoggerWrapper logger = null;
    private AdView mAdView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iptv);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(MainActivity.getAppTitle(this,getResources().getString(R.string.app_name)));

        mAdView = (AdView) findViewById(R.id.adView);
        showAd();

        ChromecastApp.Instance().mCastContext = CastContext.getSharedInstance(this);


        if(Build.VERSION.SDK_INT>=23)
        {
            requestStoragePermission();
        }
        else {
            logger = new LoggerWrapper(IptvActivity.class);
            logger.debug("Start IPTV activity");
        }
        dbHelper = new WebBrowserDbHelper(this);
        adapter = new IptvAdapter(this, new IptvDataSource(this));

        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    //showIntroductoryOverlay();
                }

                if (newState == CastState.CONNECTED) {
                    if (playAfterConnect != null) {
                        Cast(playAfterConnect.getTitle(), playAfterConnect.getUrl(),playAfterConnect.getMimeType(),(ArrayList<Video>)playAfterConnect.getOtherVideos());
                        playAfterConnect = null;
                    }
                }

            }
        };

        ListView lv = (ListView) findViewById(R.id.iptvList);

        LayoutInflater inflater = LayoutInflater.from(IptvActivity.this); // 1
        final View headerView = inflater.inflate(R.layout.iptv_listview_header, null);

        addressET = (EditText) headerView.findViewById(R.id.addressText);
        titleET = (EditText) headerView.findViewById(R.id.iptvName);
        final Button chooseButton = (Button) headerView.findViewById(R.id.choosefile);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, getString(R.string.iptv_select_file)), GET_FILE_DIALOG_CODE);
            }
        });

        final Button addButton = (Button) headerView.findViewById(R.id.iptvAdd);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText addressText = (EditText) headerView.findViewById(R.id.addressText);
                EditText titleText = (EditText) headerView.findViewById(R.id.iptvName);
                final String title = titleText.getText().toString();
                final String url = addressText.getText().toString();
                logger.info("Add playlist click: Title " + title + " URL: " + url  );
                if (TextUtils.isEmpty(url)) {
                    logger.debug("URL is empty");
                    Toast.makeText(IptvActivity.this, getString(R.string.iptv_error_message), Toast.LENGTH_LONG).show();
                } else {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (isValidate(url)) {
                                logger.debug("Validation is true");
                                String titleStr = title;
                                if (TextUtils.isEmpty(titleStr)) {
                                    titleStr = FilenameUtils.getName(url);
                                }

                                dbHelper.addIptv(titleStr, url);
                                logger.info("IPTV playlist is added");

                                sendMessage(0);
                            } else {
//                                Toast.makeText(IptvActivity.this, getString(R.string.iptv_not_supported), Toast.LENGTH_LONG).show();
                              //  ;
                                logger.debug("Validation is false");
                                sendMessage(1);
                            }
                        }
                    });
                    thread.start();

                }
            }
        });


        lv.addHeaderView(headerView);
        lv.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                Log.d("IPTV", "Context menu");
                IptvActivity.super.onCreateContextMenu(menu, v, menuInfo);

                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                View vvv = info.targetView;
                if (info.id >= 0) {
                    getMenuInflater().inflate(R.menu.iptv_list_context_menu, menu);
                }

            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                cursor.moveToFirst();
                String title = cursor.getString(1);
                String url = cursor.getString(2);
                cursor.close();
                logger.info("Item click: Title " + title  + " URL " + url);
                processFile(title, url);
            }
        });
        lv.setAdapter(adapter);

        //  registerForContextMenu(lv);
    }
    private void requestStoragePermission()
    {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        int grant = ContextCompat.checkSelfPermission(this,permission);

        String permission1 = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int grant1 = ContextCompat.checkSelfPermission(this,permission1);


        if(grant != PackageManager.PERMISSION_GRANTED && grant1 != PackageManager.PERMISSION_GRANTED)
        {
            String[] pl = new String[2];
            pl[0] = permission;
            pl[1] = permission1;
            ActivityCompat.requestPermissions(this, pl,1);

        }
        else{
            logger = new LoggerWrapper(IptvActivity.class);
            logger.debug("Start IPTV activity");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            logger = new LoggerWrapper( IptvActivity.class );
            logger.debug("IptvActivity onRequestPermissionsResult");
        }
        else{

            //Toast.makeText(this,"Permission don't granted",Toast.LENGTH_LONG).show();
        }
    }

    private void sendMessage(int what) {
        Message msg = handler.obtainMessage();
        msg.what = what;
        handler.sendMessage(msg);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {

            if (msg.what == 0) {
                adapter.Update();
                addressET.getText().clear();
                titleET.getText().clear();
            }

            if (msg.what == 1) {
                Toast.makeText(IptvActivity.this, getString(R.string.iptv_not_supported), Toast.LENGTH_LONG).show();
                ;
            }
        }
    };
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
    private void processFile(String title, String url)
    {
        File file = new File(url);
        String ext = FilenameUtils.getExtension(file.getName());

        if(ext.equalsIgnoreCase("mpd") || ext.equalsIgnoreCase("xml"))
        {
            logger.debug("Item is mpeg dash");
            Cast(title,url,"application/dash+xml",null);
        }
        else if(ext.equalsIgnoreCase("m3u8"))
        {
            logger.debug("Item is m3u8");
            Cast(title,url,"application/x-mpegURL",null);
        }
        else if (ext.equalsIgnoreCase("m3u")){
            try {
                logger.debug("Item is m3u");
                InputStream is = null;
                if (url.startsWith("http")) {
                    is = new URL(url).openStream();
                } else {
                    is = new FileInputStream(url.replace("file://",""));
                }

                M3UPlaylist playlist = parser.parseFile(is);

                M3UItem item = playlist.getPlaylistItems().get(0);
                /*String itemurl = item.getItemUrl();
                if(itemurl.startsWith("http")==false)
                {
                    itemurl = new File(url).getParent() + "/" + itemurl;
                }*/

                logger.debug("Playlist contains " + playlist.getPlaylistItems().size() + " items");

              //  Map<String, List<String>> itemheaders = getHeaders(itemurl);
               // if(isPlaylist(itemheaders))
                if(playlist.getPlaylistItems().size()>0)
                {
                    ArrayList<Video> videos = new ArrayList<>();
                    for(int i = 0;i<playlist.getPlaylistItems().size();i++)
                    {
                        File fl = new File(playlist.getPlaylistItems().get(i).getItemUrl());
                        String ext1 = FilenameUtils.getExtension(fl.getName());

                        Video vid  = new Video(playlist.getPlaylistItems().get(i).getItemName(),playlist.getPlaylistItems().get(i).getItemUrl());
                        /*if(ext1.equalsIgnoreCase("ts"))
                        {
                            vid.setMimeType("video/mp2t");
                        }
                        else {
                            vid.setMimeType("application/x-mpegURL");
                        }
                        */
                        String mime = Utils.getMimeType(vid.getUrl());
                        vid.setMimeType(mime);
                        videos.add(vid);
                    }

                    logger.debug("Open choose item dialog");
                    FindVideos(videos);
                }
                else{
                    logger.debug("Playlist contain one item. Play it");
                    Cast(title,url,"application/x-mpegURL",null);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        else{
            logger.debug("Item is other format");

            String mime = Utils.getMimeType(ext);
            if(TextUtils.isEmpty(mime))
            {
                mime= "video/mp4";
            }

            Cast(title,url,mime,null);
        }
    }
    int selectedVideo = 0;

    public void FindVideos(final ArrayList<Video> videos) {

                final List<CharSequence> titles = new ArrayList<CharSequence>();
                for (Video video : videos) {
                    Log.d("ChromecastApp", video.getTitle() + " || " + video.getUrl());
                    titles.add(video.getTitle());
                }
                selectedVideo = 0;
                AlertDialog.Builder builder = new AlertDialog.Builder(IptvActivity.this);
                builder.setTitle(R.string.select_video_title).setSingleChoiceItems(titles.toArray(new CharSequence[titles.size()]), 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedVideo = which;
                    }
                }).setPositiveButton(R.string.cast, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mediaRouteMenuItem.isVisible() == true) {
                                    dialog.cancel();
                                    if (selectedVideo > -1) {
                                        logger.debug("Choosed item with index: " + selectedVideo);
                                        if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                                            logger.debug("Cast video");
                                            Cast(titles.get(selectedVideo).toString(),videos.get(selectedVideo).getUrl(),videos.get(selectedVideo).getMimeType(),videos);
                                            selectedVideo = -1;
                                        } else {

                                            dialog.cancel();
                                            Video video = videos.get(selectedVideo);
                                            video.setOtherVideo(videos);
                                             playAfterConnect = video;
                                            ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                                            provider.onPerformDefaultAction();


                                            // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                                        }
                                    }
                                } else {
                                    dialog.cancel();
                                    if (!((Activity) IptvActivity.this).isFinishing()) {
                                        Toast.makeText(IptvActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }

                ).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

               /* if (findFilesDialog != null) {
                    findFilesDialog.dismiss();
                }
                findFilesDialog = builder.create();*/
                if (!((Activity) IptvActivity.this).isFinishing()) {
                    //    findFilesDialog.show();
                }


        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void Cast(String title, String url,String mime,ArrayList<Video> videos) {
        Video video = new Video(title, url);
        logger.debug("Cast video " + url);
        video.setMimeType(mime);
        if(videos!=null)
        {
            video.setOtherVideo(videos);
        }
        if (mediaRouteMenuItem.isVisible() == true) {

            if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {

                CastVideo castVideo = new CastVideo(IptvActivity.this);
                castVideo.setStatusListener(this);
                castVideo.Cast(video);

            } else {
                if (mediaRouteMenuItem != null) {
                    ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);

                    playAfterConnect = video;
                    provider.onPerformDefaultAction();
                }
                //Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
            }
        } else {
            //String video = parent.getItemAtPosition(position).toString();
            //TestCast(video);
            if (!((Activity) IptvActivity.this).isFinishing()) {
                Toast.makeText(IptvActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
            }
        }
    }

    private MenuItem mediaRouteMenuItem;

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browse, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info.position >= 0) {
            getMenuInflater().inflate(R.menu.iptv_list_context_menu, menu);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_FILE_DIALOG_CODE) {
            if (resultCode != RESULT_OK) return;

            Uri selectedfile = data.getData(); //The uri with the location of the file

            String message = String.format("%s", selectedfile);
            Log.d("IPTV", message);

            try {
                String path = FileManager.getPath(IptvActivity.this, selectedfile);


                addressET.setText("file://" + path);

                File file = new File(path);
                titleET.setText(file.getName());
                //   Toast.makeText(IptvActivity.this, path, Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final ListView grid = (ListView) findViewById(R.id.iptvList);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Cursor cur = (Cursor) grid.getItemAtPosition(info.position);
        cur.moveToFirst();
        Long id = cur.getLong(0);
        String title = cur.getString(1);
        String url = cur.getString(2);
        cur.close();
        switch (item.getItemId()) {

            case R.id.menuOpen:
                processFile(title, url);
                return true;
            case R.id.menuRemove:
                dbHelper.deleteIptv(id);
                adapter.Update();
                return true;
        }

        return false;
    }

    private boolean isValidate(String url) {
        File file = new File(url);
        boolean result = false;
        try {
            String ext = FilenameUtils.getExtension(file.getName());

            if (ext.equalsIgnoreCase("m3u") || ext.equalsIgnoreCase("m3u8") || ext.equalsIgnoreCase("mpd") || ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("mp4") || ext.equalsIgnoreCase("webm")
                    || ext.equalsIgnoreCase("mkv")) {
                if (ext.equalsIgnoreCase("m3u") || ext.equalsIgnoreCase("m3u8")
                        ) {

                    // parse and check url
                  /*  boolean isfile = false;
                    InputStream is = null;
                    if (url.startsWith("http")) {
                        is = new URL(url).openStream();
                    }
                    else{
                        is = new FileInputStream(url.replace("file://",""));
                        isfile = true;
                    }

                   M3UPlaylist playlist = parser.parseFile(is);
                   for(M3UItem item:playlist.getPlaylistItems())
                   {
                       if(item.getItemUrl().startsWith("http") == false && isfile == true)
                       {
                           Log.e("IPTV Playlist", "Playlist from file with relative url");
                           return false;
                       }
                       else
                       {

                       }
                   }*/
                    return true;
                          } else {
                    //if(isCorsEnabled(getHeaders(url)))
                    //{
                        return true;
                    //}
                    //return false;
                }
            } else {
                result = false;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return result;
    }

    private Map<String, List<String>> getHeaders(String url)
    {
        try {
            URL obj = new URL(url);
            URLConnection conn = obj.openConnection();
            Map<String, List<String>> map = conn.getHeaderFields();

            return map;
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private boolean isCorsEnabled(Map<String, List<String>> headers)
    {
        boolean result = false;

        if(headers.containsKey("Access-Control-Allow-Origin") == true)
        {
            List<String> rs = headers.get("Access-Control-Allow-Origin");
            for(String s:rs)
            {
                if(s == "*") {
                    result = true;
                }
            }
        }

        return result;
    }

    private boolean isPlaylist(Map<String, List<String>> headers)
    {
        boolean result = false;

        if(headers.containsKey("content-type") == true)
        {
            List<String> rs = headers.get("content-type");
            for(String s:rs)
            {
                if(s == "audio/x-mpegurl" || s == "application/vnd.apple.mpegurl" || s == "") {
                    result = true;
                }
            }
        }

        return result;
    }

    @Override
    public void onStopped() {

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
    public void onError(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(IptvActivity.this);
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

