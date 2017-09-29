package com.oxycast.chromecastapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.framework.media.widget.MiniControllerFragment;
import com.oxycast.chromecastapp.adapters.SubtitleAdapter;
import com.oxycast.chromecastapp.app.ChromecastApp;
import com.oxycast.chromecastapp.cast.CastVideo;
import com.oxycast.chromecastapp.cast.ICastStatus;
import com.oxycast.chromecastapp.cloudserver.CloudFileViewer;
import com.oxycast.chromecastapp.media.Video;
import com.oxycast.chromecastapp.services.ProxyService;
import com.oxycast.chromecastapp.subtitle.AsyncSubtitles;
import com.oxycast.chromecastapp.subtitle.ORequest;
import com.oxycast.chromecastapp.subtitle.OSubtitle;
import com.oxycast.chromecastapp.util.IabBroadcastReceiver;
import com.oxycast.chromecastapp.utils.AdProvider;
import com.oxycast.chromecastapp.utils.FileManager;
import com.oxycast.chromecastapp.utils.InAppPurchase;
import com.oxycast.chromecastapp.utils.Utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.chainsaw.Main;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cn.pedant.SweetAlert.SweetAlertDialog;
import fi.iki.elonen.SimpleWebServer;
import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.services.network.NetworkUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import subtitleFile.FormatSRT;
import subtitleFile.TimedTextFileFormat;
import subtitleFile.TimedTextObject;

public class MainActivity extends AppCompatActivity  implements ICastStatus, IabBroadcastReceiver.IabBroadcastListener{

    private static final boolean TESTMODE = true;
    private final int REQUEST_CODE_OPEN_DIRECTORY = 1001;
    private final int REQUEST_CODE_CLOUD_VIDEO = 1002;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    //    LoggerWrapper logger = null;
    Video playAfterConnect;
    private CastStateListener mCastStateListener;
    private static final int PORT = 8065;
    private static int PROXYPORT = 9500;
    private static int [] PORT2 = {8080,8066,8090,9000,9010,9500,9900,10000, 11000,12000};
    SimpleWebServer server = null;
    CastSession mCastSession = null;
    //private View fragmentController;


    static final String SKU_ADFREE= "ad_free";
    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10003;
    // The helper object
    private String TAG="suraj";
    LinearLayout buttonRemoveAd;
    InAppPurchase inAppPurchase;
    //TelephonyManager mTelephonyMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(MainActivity.getAppTitle(this,getResources().getString(R.string.app_name)));


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = prefs.edit();

        boolean isUserPro  = prefs.getBoolean("remove_ads",false);
        Log.e("mainactivity","isUserPro: "+isUserPro);
        AdProvider.isAdFree=isUserPro;
        inAppPurchase=new InAppPurchase(MainActivity.this);
        inAppPurchase.onCreate();
        //fragmentController=findViewById(R.id.castMiniController);

        setupIAB();
        mAdView = (AdView) findViewById(R.id.adView);
        mInterstitialAd = new InterstitialAd(this);
        if (!AdProvider.isAdFree) {
            mInterstitialAd.setAdUnitId(getString(R.string.main_interstitial_ad_unit_id));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestNewInterstitial();

                }
            },20000);
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mInterstitialAd.show();
                }
            });
        }
        if (Build.VERSION.SDK_INT >= 19) {
            ChromecastApp.mountedDir = FileManager.getAllAvailableSDCards(MainActivity.this);
        }

        showAd();

        /*SpannableString title = new SpannableString(getString(R.string.home_title));

        // Add a span for the sans-serif-light font
        title.setSpan(
                new TypefaceSpan("sans-serif-light"),
                0,
                title.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(title);
        actionBar.setElevation(0);*/

        ChromecastApp.Instance().mCastContext = CastContext.getSharedInstance(this);

        mCastSession = ChromecastApp.Instance().mCastContext.getSessionManager().getCurrentCastSession();
        View btnWebView =  findViewById(R.id.wcbutton);
        btnWebView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);

                startActivity(intent);
            }
        });

        View btnLocalVideo =  findViewById(R.id.phoneVideoButton);
        btnLocalVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LocalVideoActivity.class);

                startActivity(intent);
            }
        });

        View iptvButton =  findViewById(R.id.iptvButton);
        iptvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, IptvActivity.class);
                startActivity(intent);
            }
        });
        View rate =  findViewById(R.id.rate);
        rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });


        View help =  findViewById(R.id.help);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),HelpActivity.class));

            }
        });

        View facebook =  findViewById(R.id.facebook);
        facebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(getOpenFacebookIntent(getApplicationContext()));
            }
        });

        View share =  findViewById(R.id.share);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "OxyCast TV -Cast Local And Online Videos: https://play.google.com/store/apps/details?id="+getPackageName() ;
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent,"Share using"));
            }
        });


        View serverButton = findViewById(R.id.serverButton);
        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CloudFileViewer.class);
                intent.putExtra("isAdFree",AdProvider.isAdFree);
                startActivityForResult(intent,REQUEST_CODE_CLOUD_VIDEO);
            }
        });


       buttonRemoveAd = (LinearLayout) findViewById(R.id.lay_removeAd);
        if (AdProvider.isAdFree)
        {
            buttonRemoveAd.setVisibility(View.INVISIBLE);
        }
        buttonRemoveAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AdProvider.isAdFree)
                {
                    showRemoveAdDialog();
                }else {
                    Toast.makeText(MainActivity.this,"This is ad free version!",Toast.LENGTH_LONG).show();
                }

            }
        });

        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    //showIntroductoryOverlay();
                }

                if (newState == CastState.CONNECTED) {
                    if (playAfterConnect != null) {

                        if (playAfterConnect.getUrl().startsWith("http")) {
                            if (playAfterConnect.getMimeType().equalsIgnoreCase("application/x-mpegURL") ||
                                    playAfterConnect.getMimeType().equalsIgnoreCase("application/vnd.apple.mpegURL")) {
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(playAfterConnect.isproxy() ==false)
                                            playAfterConnect.setUrl(Utils.getRedirect(playAfterConnect.getUrl()));
                                        sendCastMessage(playAfterConnect);
                                        playAfterConnect = null;
                                    }
                                });
                                thread.start();
                            } else {
                                Cast(playAfterConnect.getTitle(), playAfterConnect.getUrl(), playAfterConnect.getMimeType());
                                playAfterConnect = null;
                            }
                        } else if (playAfterConnect.getUrl().startsWith("file")) {
                            LocalCast(playAfterConnect);
                            playAfterConnect = null;
                        }

                    }
                }

            }
        };


       /* String action = getIntent().getAction();

        if (action.equals(Intent.ACTION_VIEW)) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                Log.d("ACTIONVIEW", "uri: " + uri.toString());

            }
            else{
                Log.d("ACTIONVIEW", "no uri");
            }
            playAfterConnect = new Video(getIntent().getData().toString(), getIntent().getData().toString());

            String mime = Utils.getMimeType(playAfterConnect.getUrl());

            playAfterConnect.setMimeType(mime);

            if (mediaRouteMenuItem.isVisible() == true) {
                //  logger.debug("Choosed item with index: " + selectedVideo);
                if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                    //  logger.debug("Cast video");
                    Cast(playAfterConnect.getUrl().toString(), playAfterConnect.getUrl(), mime);

                } else {

                    ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                    provider.onPerformDefaultAction();
                    // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                }
            } else {
                if (!((Activity) MainActivity.this).isFinishing()) {
                    Toast.makeText(MainActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
                }
            }
        }
        */
        String action = getIntent().getAction();

        if (action.equals(Intent.ACTION_VIEW)) {
            Uri uri = getIntent().getData();
            Log.v("suraj","url sent by other app:"+uri.toString());
            if (uri != null) {
                Bundle bundle = getIntent().getExtras();

                Log.d("ACTIONVIEW", "uri: " + uri.toString());
                if (uri.getScheme().equalsIgnoreCase("file")) {
                    playAfterConnect = new Video(getIntent().getData().toString(), getIntent().getData().toString());
                    playAfterConnect.setLocalPath(getIntent().getData().toString().replace("file://", ""));
                } else {
                    playAfterConnect = new Video(getIntent().getData().toString(), getIntent().getData().toString());
                    String mime = Utils.getMimeType(playAfterConnect.getUrl());

                    playAfterConnect.setMimeType(mime);

                    if (!getIntent().getData().toString().contains("?wmsAuthSign=")) {
                        if (bundle != null) {
                            Log.d("ACTIONVIEW", "Additional parameters");
                            String[] payload = bundle.getStringArray("headers");
                            if (payload != null && payload.length == 2) {
                                Uri videouri = getIntent().getData();
                                String useragent = payload[1];
                                String proxy = videouri.getScheme() + "://" + videouri.getHost() + ":" + videouri.getPort();
                                String host = videouri.getHost();
                                String ip = getIp();

                                PROXYPORT++;
                                String newUrl = "http://" + ip + ":" + PROXYPORT + videouri.getPath();
                                Log.v("suraj", "video is playing using this url:" + newUrl);
                                playAfterConnect.setUrl(newUrl);
                                playAfterConnect.setIsproxy(true);
                                try {
                                    // stopService(
                                    //     new Intent(MainActivity.this, ProxyService.class));
                                } catch (Exception ex) {

                                }

                                if (!TextUtils.isEmpty(proxy)) {
                                    Intent proxyintent = new Intent(MainActivity.this, ProxyService.class);
                                    proxyintent.putExtra("proxy", proxy);
                                    proxyintent.putExtra("host", host);
                                    proxyintent.putExtra("useragent", useragent);
                                    proxyintent.putExtra("IP", ip);
                                    proxyintent.putExtra("video", playAfterConnect);
                                    proxyintent.putExtra("port", PROXYPORT);
                                    startService(proxyintent);
                                    Log.v("suraj", "proxy server started!");

                                } else {
                                    Log.d("Proxy", "Proxy is null:  " + proxy);
                                }
                            }
                        }
                    }

                }
            } else {
                Log.d("ACTIONVIEW", "no uri");

            }


            final Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //  String action = getIntent().getAction();
                    if (playAfterConnect!=null){
                    if (playAfterConnect.getMimeType().equalsIgnoreCase("application/x-mpegURL") ||
                            playAfterConnect.getMimeType().equalsIgnoreCase("application/vnd.apple.mpegURL")) {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(playAfterConnect.isproxy() ==false)
                                    playAfterConnect.setUrl(Utils.getRedirect(playAfterConnect.getUrl()));
                                sendCastMessage(playAfterConnect);
                            }
                        });
                        thread.start();
                    } else {
                        if (mediaRouteMenuItem != null && mediaRouteMenuItem.isVisible() == true) {
                            //  logger.debug("Choosed item with index: " + selectedVideo);
                            if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                                //  logger.debug("Cast video");
                                if (playAfterConnect.getUrl().startsWith("http")) {
                                    Cast(playAfterConnect.getUrl().toString(), playAfterConnect.getUrl(), playAfterConnect.getMimeType());
                                } else if (playAfterConnect.getUrl().startsWith("file")) {
                                    LocalCast(playAfterConnect);
                                }
                            } else {

                                ChromecastApp.currentVideo = playAfterConnect;
                                ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                                provider.onPerformDefaultAction();
                                // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                            }
                        } else {
                            if (!((Activity) MainActivity.this).isFinishing()) {
                                Toast.makeText(MainActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                }
                }
            }, 5000);
        }
        boolean isIntroDone=prefs.getBoolean("introDone",false);
        if (!isIntroDone){
            startActivity(new Intent(MainActivity.this,AppIntroActivity.class));
            finish();
            return;
        }
    }



public void setupIAB(){
   // mHelper = new IabHelper(this, ChromecastApp.base64encodedStr);

    // enable debug logging (for a production application, you should set this to false).
  //  mHelper.enableDebugLogging(MainActivity.TESTMODE);

    // Start setup. This is asynchronous and the specified listener
    // will be called once setup completes.
    Log.d(TAG, "Starting setup.");
 /*   mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
        public void onIabSetupFinished(IabResult result) {
            Log.d(TAG, "Setup finished.");

            if (!result.isSuccess()) {
                // Oh noes, there was a problem.
                complain("Problem setting up in-app billing: " + result);
                return;
            }

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // IAB is fully set up. Now, let's get an inventory of stuff we own.
            Log.d(TAG, "Setup successful. Querying inventory.");
            mHelper.queryInventoryAsync(mGotInventoryListener);
        }
    });*/
}
   /* // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }
            Log.d(TAG, "Query inventory was successful.");
            // Do we have the premium upgrade?
            Purchase adFreePurchase = inventory.getPurchase(SKU_ADFREE);
            mIsAdFree = (adFreePurchase != null && verifyDeveloperPayload(adFreePurchase));
            AdProvider.setAsAdFree(mIsAdFree);
            showAd();
        }
        *//** Verifies the developer payload of a purchase. *//*
        boolean verifyDeveloperPayload(Purchase p) {
            String payload = p.getDeveloperPayload();
            return true;
        }
    };*/

    private boolean mIsAdFree;
    // Callback for when a purchase is finished
    /*IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_ADFREE)) {
                // bought the premium upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                mIsAdFree = true;
                AdProvider.setAsAdFree(mIsAdFree);
            }
        }
    };*/
    public void complain(String msg){
        Log.v(TAG,msg);
    }

    private void buyAdFree() {
        Log.v("suraj","buyAdFree called");
        String payload = "";
        inAppPurchase.purchaseRemoveAds();
       /* mHelper.launchPurchaseFlow(MainActivity.this, SKU_ADFREE, RC_REQUEST,
                mPurchaseFinishedListener, payload);*/
    }
public void showRemoveAdDialog(){
    new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Purchase")
            .setContentText(getResources().getString(R.string.purchase_message))
            .setCancelText("Cancel")
            .setConfirmText("BUY")
            .showCancelButton(true)
            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                @Override
                public void onClick(final SweetAlertDialog sweetAlertDialog) {
                    buyAdFree();
                    sweetAlertDialog.cancel();
                }
            })
            .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                @Override
                public void onClick(SweetAlertDialog sDialog) {
                    sDialog.cancel();
                }
            })
            .show();
}
public void resetLayouts(){
    Log.v("suraj","resetLayouts called");
   /* RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) fragmentController.getLayoutParams();
    if (mAdView.getVisibility()==View.VISIBLE){
        params.addRule(RelativeLayout.ABOVE,R.id.adView);
    }else {

        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    }

    fragmentController.setLayoutParams(params);*/
}
    @Override
    protected void onPause() {

        ChromecastApp.Instance().mCastContext.removeCastStateListener(mCastStateListener);
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        inAppPurchase.onDestroy();

        if (mAdView != null) {
            mAdView.destroy();
        }

        super.onDestroy();
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
                    resetLayouts();
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    mAdView.setVisibility(View.GONE);
                    resetLayouts();
                }
            });

        }else{
            mAdView.setVisibility(View.GONE);
        }
    }



    @Override
    protected void onResume() {
        ChromecastApp.Instance().mCastContext.addCastStateListener(mCastStateListener);
        if (mAdView != null) {
            mAdView.resume();
        }
        resetLayouts();
        super.onResume();

    }


    private void Cast(String title, String url,String mime) {
        final Video video = new Video(title, url);

        video.setMimeType(mime);
        //ChromecastApp.Instance().mCastContext.getSessionManager().(false);

        if (mediaRouteMenuItem!=null && mediaRouteMenuItem.isVisible() == true) {

            if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                CastVideo castVideo = new CastVideo(MainActivity.this);
                castVideo.setStatusListener(this);
                castVideo.Cast(video);

            } else {
                if (mediaRouteMenuItem != null) {
                    ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);

                    playAfterConnect = video;
                    provider.onPerformDefaultAction();
                }
            }
        } else {

            if (!((Activity) MainActivity.this).isFinishing()) {
                Toast.makeText(MainActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestNewInterstitial() {

            if (MainActivity.TESTMODE){
                AdRequest adRequest = new AdRequest.Builder()
                        .addTestDevice("A8CC46975AED15129FE541950D993AAF")
                        .build();
                mInterstitialAd.loadAd(adRequest);
            }else {
                AdRequest adRequest = new AdRequest.Builder()
                        .build();
                mInterstitialAd.loadAd(adRequest);
            }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        inAppPurchase.onActivityResul(requestCode,resultCode,data);

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {
            Log.d("API", String.format("Open Directory result Uri : %s", data.getData()));
            if(Build.VERSION.SDK_INT > 21) {

                String id = DocumentsContract.getTreeDocumentId(data.getData());
                id = id + "/video/H.D .Videos";
                //Uri docUri = DocumentsContract.buildDocumentUriUsingTree(data.getData(),
                //      DocumentsContract.getTreeDocumentId(data.getData()));
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(data.getData(),id);

                Log.d("API", String.format("Access permission to Directory result Uri : %s", docUri));
                //String storage = "/storage/9016-4EF8/video/H.D .Videos/HouseMd.ttml";

                //Uri uri = getDestinationFileUri(docUri,"/storage/9016-4EF8",storage,true);

                String storage ="content://com.android.externalstorage.documents/document/9016-4EF8%3Avideo%2FHouseMd.txt";
                Uri uri1 = Uri.parse(storage);
              /*  this.grantUriPermission(this.getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


                this.getContentResolver().takePersistableUriPermission(uri, takeFlags);
*/
                // Uri uri = DocumentsContract.buildDocumentUriUsingTree(data.getData(),DocumentsContract.getTreeDocumentId(uri1));
                Uri uri = null;
                try {
                    uri = DocumentsContract.createDocument(this.getContentResolver(),docUri,"application/xml","Housemd.txt");
                    writeFileContent(uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Log.d("API", String.format("Dest File Uri: %s", uri));

            }
        }else  if (requestCode == REQUEST_CODE_CLOUD_VIDEO && resultCode == Activity.RESULT_OK) {
            String videoUrl=data.getStringExtra("link");
            Log.v("suraj","link:"+data.getStringExtra("link"));

            if (videoUrl.length()>0) {
                playAfterConnect = new Video(videoUrl,videoUrl);
                String mime = Utils.getMimeType(videoUrl);
                if (mime!=null)
                    playAfterConnect.setMimeType(mime);
                else
                    playAfterConnect.setMimeType("");

                sendCastMessage(playAfterConnect);

            }else{
                Toast.makeText(this,"Invalid video url!",Toast.LENGTH_LONG).show();
            }
        }else if (requestCode==RC_REQUEST){
            Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
           // if (mHelper == null) return;
            // Pass on the activity result to the helper for handling
           /* if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
                // not handled, so handle it ourselves (here's where you'd
                // perform any handling of activity results not related to in-app
                // billing...
                super.onActivityResult(requestCode, resultCode, data);
            }*/
        }
    }

    private void writeFileContent(Uri uri) {

        try {

            Log.d("API", String.format("writeFileContent start %s",uri));
            String asString = "111222333444";
            byte[] newBytes = asString.getBytes("UTF-8");

            ParcelFileDescriptor pfd =
                    this.getContentResolver().
                            openFileDescriptor(uri, "w");

            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());

            fileOutputStream.write(newBytes);
            fileOutputStream.close();
            pfd.close();
            Log.d("API", String.format("writeFileContent end"));

        } catch (Exception ex) {
            Log.e("WRITE", ex.getMessage() + "\r\n" + ex.getStackTrace(),ex);
        }
    }

    private MenuItem mediaRouteMenuItem;

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browse, menu);

        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);

        return true;
    }

    String subtitlePath;
    AsyncSubtitles mASub;
    OSubtitle subtitle;
    public void showDialog() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this,R.style.SubtitleDialog);
        builder.setTitle("Subtitles");
        // Get the layout inflater

        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        View view = inflater.inflate(R.layout.search_dialog, null);
        builder.setView(view);
        final ProgressBar progressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        final EditText sv = (EditText) view.findViewById(R.id.search_bar);
        final ListView listResult = (ListView)view.findViewById(R.id.listResult);
        listResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                subtitle = (OSubtitle)parent.getItemAtPosition(position);
                if(mASub!=null)
                {
//                    File file = new File(ChromecastApp.currentVideo.getLocalPath());
                    final File file =  getSubtitleStorageDir();
                    //File subtitleFile = new File(file.getParent(),subtitle.getSubFileName());
                    File zipFileName = new File(subtitle.getZipDownloadLink());
                    File subtitleFile = new File(file.getAbsolutePath(),zipFileName.getName());
                    subtitlePath = subtitleFile.getAbsolutePath();
                    // mASub.downloadSubByIdToPath(subtitle.getIDSubtitle(),subtitlePath);


                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            try {

                                File subfile = downloadSubtitle(subtitle.getZipDownloadLink(), subtitlePath);
                                if (subfile.exists() == true) {
                                    sendMessage("Download complete");
                                }
                                else {
                                    sendMessage("Download error");
                                }

                                List<File> files = unzipSubtitle(subfile,file.getAbsoluteFile());

                                sendMessage("Unzip complete. Subtitles count: " +files.size());

                                List<File> converted = convertSubtitles(files);

                                sendMessage("Convert complete. Subtitles count: " + converted.size());

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                    Log.d("Subtitle", "Download URL: " + subtitle.getSubDownloadLink());
                    Log.d("Subtitle", "URL: " + subtitle.getSubtitlesLink());
                    Log.d("Subtitle", "ZIP URL: " + subtitle.getZipDownloadLink());

                }
            }
        });

        if(sv!=null && ChromecastApp.currentVideo!=null)
        {
            String fileNameWithOutExt = FilenameUtils.removeExtension(new File(ChromecastApp.currentVideo.getUrl()).getName());
            sv.setText(fileNameWithOutExt);
        }

        sv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    sv.clearFocus();
                    progressBar.setVisibility(View.VISIBLE);
                    try {
                        mASub = new AsyncSubtitles(MainActivity.this, new AsyncSubtitles.SubtitlesInterface() {
                            @Override
                            public void onSubtitlesListFound(List<OSubtitle> list) {
                                Log.d("Subtitle","Finded " + list.size());
                                progressBar.setVisibility(View.INVISIBLE);
                                SubtitleAdapter adapter = new SubtitleAdapter(MainActivity.this,list);
                                if(listResult!=null)
                                {
                                    listResult.setAdapter(adapter);
                                }
                            }

                            @Override
                            public void onSubtitleDownload(boolean b) {
                                if(b == true)
                                {
                                    Log.d("Subtitle", "Subtitle format: " + subtitle.getSubFormat());
                                    Toast.makeText(MainActivity.this, "Download success",Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    Toast.makeText(MainActivity.this, "Download error",Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onError(int error) {
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        });
                        mASub.setLanguagesArray(new String[]{"en"});
                        ORequest req = new ORequest("", sv.getText().toString() , null, new String[]{"spa","eng"});

                        mASub.setNeededParamsToSearch(req);

                        mASub.getPossibleSubtitle();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    return true;
                }
                return false;
            }
        });

        ImageView searchImage = (ImageView) view.findViewById(R.id.search_icon);
        searchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                try {
                    mASub = new AsyncSubtitles(MainActivity.this, new AsyncSubtitles.SubtitlesInterface() {
                        @Override
                        public void onSubtitlesListFound(List<OSubtitle> list) {
                            Log.d("Subtitle","Finded " + list.size());
                            progressBar.setVisibility(View.INVISIBLE);
                            SubtitleAdapter adapter = new SubtitleAdapter(MainActivity.this,list);
                            if(listResult!=null)
                            {
                                listResult.setAdapter(adapter);
                            }
                        }

                        @Override
                        public void onSubtitleDownload(boolean b) {

                        }

                        @Override
                        public void onError(int error) {
                            Log.d("Subtitle", "Error: " + error);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    mASub.setLanguagesArray(new String[]{"en"});
                    ORequest req = new ORequest("", sv.getText().toString() , null, new String[]{"spa","eng"});
                    mASub.setNeededParamsToSearch(req);
                    mASub.getPossibleSubtitle();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        AppCompatDialog dialog1 = builder.create();
        dialog1.setCanceledOnTouchOutside(true);
        dialog1.setCancelable(true);
        dialog1.show();
    }

    private File downloadSubtitle(String url,String path) throws Exception
    {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url)
                .build();
        Response response = client.newCall(request).execute();

        InputStream in = response.body().byteStream();

        File file = new File(path);
        FileOutputStream outputStream =  new FileOutputStream(file);
        int read;
        byte[] bytes = new byte[1024];

        while ((read = in.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }

        response.body().close();

        return file;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this,"Permission is granted",Toast.LENGTH_LONG).show();

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


        if(grant != PackageManager.PERMISSION_GRANTED && grant1 != PackageManager.PERMISSION_GRANTED)
        {
            String[] pl = new String[2];
            pl[0] = permission;
            pl[1] = permission1;
            ActivityCompat.requestPermissions(this, pl,1);

        }
        else{

        }
    }

    public File getSubtitleStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Subtitle");
        if (!file.mkdirs()) {
            Log.e("Subtitle", "Directory not created");
        }
        return file;
    }

   private  Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 1) {
                Bundle res = msg.getData();
                String text = res.getString("text");
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            }
            else if(msg.what == 2)
            {
                Video video = (Video)msg.getData().getSerializable("video");
                Log.d("REDIRECT", "Video url: " + video.getUrl());
                if (mediaRouteMenuItem!=null && mediaRouteMenuItem.isVisible() == true) {
                    //  logger.debug("Choosed item with index: " + selectedVideo);
                    if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                        //  logger.debug("Cast video");
                        if(video.getUrl().startsWith("http")) {
                            Cast(video.getUrl().toString(), video.getUrl(), video.getMimeType());
                        }
                        else if (video.getUrl().startsWith("file")){
                            LocalCast(video);
                        }
                    } else {

                        ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                        provider.onPerformDefaultAction();
                        // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (!((Activity) MainActivity.this).isFinishing()) {
                        Toast.makeText(MainActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };
    private final MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                if(msg.what == 1) {
                    Bundle res = msg.getData();
                    String text = res.getString("text");
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
                }
                else if(msg.what == 2)
                {
                    Video video = (Video)msg.getData().getSerializable("video");
                    Log.d("REDIRECT", "Video url: " + video.getUrl());
                    if (activity.mediaRouteMenuItem!=null && activity.mediaRouteMenuItem.isVisible() == true) {
                        //  logger.debug("Choosed item with index: " + selectedVideo);
                        if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                            //  logger.debug("Cast video");
                            if(video.getUrl().startsWith("http")) {
                                activity.Cast(video.getUrl().toString(), video.getUrl(), video.getMimeType());
                            }
                            else if (video.getUrl().startsWith("file")){
                                activity.LocalCast(video);
                            }
                        } else {

                            ActionProvider provider = MenuItemCompat.getActionProvider(activity.mediaRouteMenuItem);
                            provider.onPerformDefaultAction();
                            // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if (!(activity.isFinishing())) {
                            Toast.makeText(activity, activity.getResources().getString(R.string.not_device), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    }


    private void sendMessage(String text)
    {
        //Message msg = handler.obtainMessage();
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("text",text);
        msg.setData(bundle);
        msg.what = 1;
        mHandler.sendMessage(msg);
    }

    private void sendCastMessage(Video video)
    {
        //Message msg = handler.obtainMessage();
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putSerializable("video",video);
        msg.setData(bundle);
        msg.what = 2;
        mHandler.sendMessage(msg);
    }

    public List<File> unzipSubtitle(File zipFile, File targetDirectory) throws IOException {

        //    ZipFile zip =new ZipFile(zipFile);
        List<File> subtitles = new ArrayList<File>();
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            byte[] buffer = new byte[2048];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                String ext = FilenameUtils.getExtension(ze.getName());
                if(ext.equalsIgnoreCase("srt") || ext.equalsIgnoreCase("vtt") ) {
                    FileOutputStream fout = new FileOutputStream(file);
                    BufferedOutputStream bufout = new BufferedOutputStream(fout);
                    int count;
                    try {
                        while ((count = zis.read(buffer)) != -1)
                            bufout.write(buffer, 0, count);
                    } finally {
                        subtitles.add(file);
                        bufout.close();
                        fout.close();
                    }
                }

            }
        } finally {
            zis.close();
        }


        return subtitles;
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

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (!TextUtils.isEmpty(extension)) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        else{

            int index = url.lastIndexOf(".");
            extension = url.substring(index+1);
            if (!TextUtils.isEmpty(extension)) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }
            else{

                type = "video/mp4";
            }
        }

        return type;
    }

    private void LocalCast(Video video) {

        String formatedIpAddress = "";
        if(isHotspot() == false) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        }
        else{
            //  CastSession mCastSession = ChromecastApp.Instance().mCastContext.getSessionManager().getCurrentCastSession();
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

        File videoFile = new File(video.getLocalPath());
        String workingDir = videoFile.getParent();

        Log.d("NanoHttpd", "Working directory: " + workingDir);
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

            server = new SimpleWebServer(formatedIpAddress, currentPort, new File("/"), false, "*");
            try {
                server.start();

                iswebserveropen = server.isAlive();

                break;
            } catch (Exception ex) {

                Crashlytics.log("Address " + formatedIpAddress + " PORT " + currentPort + " " + ex.getMessage() + " | " + ex.getStackTrace() );

            }
        }
        ChromecastApp.localWebServer = "http://" + formatedIpAddress + ":" + currentPort;
        String url = "http://" + formatedIpAddress + ":" + currentPort +  videoFile.getAbsolutePath();  //videoFile.getAbsolutePath().replace(" ", "%20");

        //String url = "http://" + formatedIpAddress + ":" + currentPort + "/" + videoFile.getAbsolutePath();
        Log.d("NanoHttpd","Please access! " + url);
        String mime = getMimeType("file://" + video.getLocalPath());
        Log.d("NanoHttpd", "Mime: " + mime);

        Video cVideo = new Video(videoFile.getName(),url);
        cVideo.setLocalPath(video.getLocalPath());
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

        ChromecastApp.currentVideo = cVideo;

        CastVideo castVideo = new CastVideo(MainActivity.this);
        castVideo.setStatusListener(MainActivity.this);
        castVideo.Cast(cVideo);
    }


    public List<File> convertSubtitles(List<File> files)
    {
        List<File> converted = new ArrayList<>();
        if(files.size()>0)
        {
            for(File st: files) {
                try {
                    String enc = Utils.getEncoding(st);
                    Log.d("Encoding", "Subtitle encoding: " + enc);

                    File utffile = new File(st.getParent(),FilenameUtils.removeExtension(st.getName()) + "_utf8." + FilenameUtils.getExtension(st.getName()) );
                    if(utffile.exists() == true)
                    {
                        utffile.delete();
                    }
                    utffile.createNewFile();
                    convertToUTF8(st,enc,utffile,"UTF-8");
                    TimedTextFileFormat ttff = new FormatSRT();

                    InputStream is = new FileInputStream(utffile);
                    TimedTextObject tto = ttff.parseFile(utffile.getName(), is);
                    String newFileName = FilenameUtils.removeExtension(utffile.getName()) + ".ttml";

                    File outputPath = new File(st.getParent(),newFileName);
                    writeFileTxt(outputPath.getAbsolutePath(), tto.toTTML(),enc);
                    converted.add(outputPath);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return converted;
    }

    public void convertToUTF8(File source, String srcEncoding, File target, String tgtEncoding)
    {

        try {
            FileInputStream fileInput = new FileInputStream(source);

            BufferedReader br = new BufferedReader(new InputStreamReader(fileInput, srcEncoding));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), tgtEncoding));

            char[] buffer = new char[16384];
            int read;
            while ((read = br.read(buffer)) != -1)
                bw.write(buffer, 0, read);
        }
        catch (Exception ex){}
    }

    public static void writeFileTxt(String fileName, String[] totalFile, String enc){

        CharsetEncoder encoder = Charset.forName(enc).newEncoder();
        File file = null;
        PrintWriter pw = null;
        try
        {
            file = new File(fileName);
            if(file.exists() == false)
            {
                file.createNewFile();
            }
            pw = new PrintWriter(file,enc);

            for (int i = 0; i < totalFile.length; i++) {
                Log.d("Subtitle", totalFile[i]);
                pw.println(totalFile[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Execute the "finally" to make sure the file is closed

            } catch (Exception e2) {
                e2.printStackTrace();
            }

            try {
                if (pw != null)
                    pw.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    @Override
    public void onStopped() {

    }

    @Override
    public void onError(String title) {
        if (! MainActivity.this.isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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

    private String getIp()
    {
        String formatedIpAddress = "";
        if(isHotspot() == false) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        }
        else{
            //    CastSession mCastSession = ChromecastApp.Instance().mCastContext.getSessionManager().getCurrentCastSession();
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
    public static SpannableString getAppTitle(Context context,String text){
        SpannableString title = new SpannableString(text);
        // Add a span for the sans-serif-light font
        title.setSpan(
                new com.oxycast.chromecastapp.extras.TypefaceSpan(context, "SEGOEUIZ(1).TTF"),
                0,
                title.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return title;
    }
    @Override
    public void receivedBroadcast() {
        inAppPurchase.receivedBroadcast();
    }

    public  Intent getOpenFacebookIntent(Context context) {

        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            return new Intent(Intent.ACTION_VIEW, Uri.parse(getFacebookPageURL(context)));
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse(getFacebookPageURL(context)));
        }
    }

    public String getFacebookPageURL(Context context) {
        String FACEBOOK_URL = "https://www.facebook.com/oxycast";
        String FACEBOOK_PAGE_ID = "391013591269366";
        PackageManager packageManager = context.getPackageManager();
        try {
            int versionCode = packageManager.getPackageInfo("com.facebook.katana", 0).versionCode;
            if (versionCode >= 3002850) { //newer versions of fb app
                return "fb://facewebmodal/f?href=" + FACEBOOK_URL;
            } else { //older versions of fb app
                return "fb://page/" + FACEBOOK_PAGE_ID;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return FACEBOOK_URL; //normal web url
        }
    }

}
