package com.oxycast.chromecastapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.oxycast.chromecastapp.adapters.BookmarkAdapter;
import com.oxycast.chromecastapp.adapters.HistoryAdapter;
import com.oxycast.chromecastapp.adapters.QuickAccessAdapter;
import com.oxycast.chromecastapp.adapters.VideoListAdapter;
import com.oxycast.chromecastapp.app.ChromecastApp;
import com.oxycast.chromecastapp.cast.CastVideo;
import com.oxycast.chromecastapp.cast.ICastStatus;
import com.oxycast.chromecastapp.data.BookmarkDataSource;
import com.oxycast.chromecastapp.data.HistoryDataSource;
import com.oxycast.chromecastapp.data.QuickAccessDataSource;
import com.oxycast.chromecastapp.media.IWebBrowserResult;
import com.oxycast.chromecastapp.media.Video;
import com.oxycast.chromecastapp.searchbar.FocusEditText;
import com.oxycast.chromecastapp.util.IabBroadcastReceiver;
import com.oxycast.chromecastapp.utils.AdProvider;
import com.oxycast.chromecastapp.utils.InAppPurchase;
import com.oxycast.chromecastapp.utils.Utils;
import com.oxycast.chromecastapp.web.AdblockWebView;
import com.oxycast.chromecastapp.web.IProgressListener;
import com.oxycast.chromecastapp.web.db.WebBrowserDbHelper;

import org.adblockplus.libadblockplus.android.settings.AdblockHelper;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class WebViewActivity extends AppCompatActivity implements IWebBrowserResult, IProgressListener, ICastStatus, IabBroadcastReceiver.IabBroadcastListener {

    private final SessionManagerListener<CastSession> mSessionManagerListener =
            new MySessionManagerListener();
    private CastSession mCastSession;
    private AdView mAdView;
    private MenuItem mQueueMenuItem;
    private Toolbar mToolbar;
    private IntroductoryOverlay mIntroductoryOverlay;
    private CastStateListener mCastStateListener;

    private MenuItem mSearchAction;
    private boolean isSearchOpened = false;
    private EditText edtSeach;

    @Override
    public void onStopped() {

    }

    @Override
    public void onError(String title) {
        if (! this.isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
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

    @Override
    public void receivedBroadcast() {
        inAppPurchase.receivedBroadcast();


    }

    private class MySessionManagerListener implements SessionManagerListener<CastSession> {
//
        @Override
        public void onSessionEnded(CastSession session, int error) {
            if (session == mCastSession) {
                mCastSession = null;
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            mCastSession = session;
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            mCastSession = session;
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionStarting(CastSession session) {
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionEnding(CastSession session) {
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
        }
    }

    FocusEditText mFocusEditText;
    AdblockWebView webView;
    FrameLayout webViewPanel;
    FrameLayout historyPanel;
    FrameLayout quickaccesspanel;
    ImageView menuButton;
    ProgressBar progressBar;
    String PageTitle;
    Bitmap PageBitmap;
    CastVideo castVideo;
    ImageView searchImage;
    WebBrowserDbHelper dbHelper;

    ListView listView = null;
    QuickAccessAdapter quickAccessAdapter;
    BookmarkAdapter bookmarkAdapter = null;
    HistoryAdapter historyAdapter = null;
    View logoLayout = null;
    ArrayList<Video> mFoundVideos;
    InAppPurchase inAppPurchase;

    Video PlayAfterConnect = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT<21)
        {
            CookieSyncManager.createInstance(this);
            CookieSyncManager.getInstance().startSync();
        }
        setContentView(R.layout.activity_web_view);

        ChromecastApp.Instance().mCastContext = CastContext.getSharedInstance(this);
        inAppPurchase=new InAppPurchase(WebViewActivity.this);
        inAppPurchase.onCreate();
        mAdView = (AdView) findViewById(R.id.adView);
        showAd();

        initToolbars();
        mFoundVideos = new ArrayList<Video>();
        logoLayout = findViewById(R.id.logo_layout);
        mFocusEditText = (FocusEditText) findViewById(R.id.search_bar);
      //  mFocusEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        webViewPanel = (FrameLayout) findViewById(R.id.webViewPanel);
        historyPanel = (FrameLayout) findViewById(R.id.historyPanel);
        quickaccesspanel = (FrameLayout)findViewById(R.id.quickaccesspanel);
        webView = (AdblockWebView) findViewById(R.id.webView);
        webView.setWebBrowserListener(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WebViewActivity.this);

        boolean isadblock  = prefs.getBoolean("isadblock",false);

        webView.setAdblockEnabled(isadblock);
        if (Build.VERSION.SDK_INT >= 21) {
            // AppRTC requires third party cookies to work
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
      else{
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
        }

      //  menuButton = (ImageView) findViewById(R.id.menuButton);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
      //  webView.setProgressBar(progressBar);
        webView.setProgressListener(this);
        mFocusEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, final boolean hasFocus) {
                // this is the main interesting point, you can extend this with animations.
                if (hasFocus) {
                    Log.v("wasim","has facus");
                    mFocusEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                    logoLayout.setVisibility(View.GONE);
                    quickaccesspanel.setVisibility(View.GONE);

                   /* if(mFocusEditText.getText().toString().length()>0)
                    {
                        mFocusEditText.selectAll();
                    }*/


                    RelativeLayout rlTB = (RelativeLayout) findViewById(R.id.textboxRL);
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTB.getLayoutParams();
                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    //lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    rlTB.setLayoutParams(lp);

                   // menuButton.setVisibility(View.VISIBLE);
                    //   toolbar.setVisibility(View.GONE);
                } else {
                    Log.v("wasim","has no facus");
                    if (mFocusEditText.getText().toString().trim().length() == 0) {
                        mFocusEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                        logoLayout.setVisibility(View.VISIBLE);
                        quickaccesspanel.setVisibility(View.VISIBLE);
                        webViewPanel.setVisibility(View.GONE);
                        historyPanel.setVisibility(View.GONE);

                        RelativeLayout rlTB = (RelativeLayout) findViewById(R.id.textboxRL);

                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTB.getLayoutParams();
                        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT,0);
                      //  lp.(RelativeLayout.ALIGN_PARENT_LEFT);
                        rlTB.setLayoutParams(lp);
                    //    menuButton.setVisibility(View.GONE);

                    }
                    //toolbar.setVisibility(View.VISIBLE);
                }
            }
        });

        mFocusEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    mFocusEditText.clearFocus();
                    webViewPanel.setVisibility(View.VISIBLE);
                    performSearch(v.getText().toString().trim());
                    return true;
                }
                return false;
            }
        });


     /*   menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu(view);
            }
        });*/
        searchImage = (ImageView) findViewById(R.id.search_icon);
        searchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFocusEditText.clearFocus();
                webViewPanel.setVisibility(View.VISIBLE);
                performSearch(mFocusEditText.getText().toString().trim());
            }
        });

        dbHelper = new WebBrowserDbHelper(this);

        listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cur = (Cursor)listView.getItemAtPosition(position);
                cur.moveToFirst();
                String url = cur.getString(2);
                cur.close();

                mFocusEditText.setText(url);
                performSearch(url);
            }
        });

        ImageView ivGoogle = (ImageView) findViewById(R.id.googleIV);
        ivGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFocusEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                logoLayout.setVisibility(View.GONE);
                quickaccesspanel.setVisibility(View.GONE);
             //   menuButton.setVisibility(View.VISIBLE);
                mFocusEditText.setText("https://google.com");
                performSearch("https://google.com");

                RelativeLayout rlTB = (RelativeLayout) findViewById(R.id.textboxRL);
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTB.getLayoutParams();
                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                //lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                rlTB.setLayoutParams(lp);
            }
        });

       final GridView grid = (GridView)findViewById(R.id.quickaccess);
        quickAccessAdapter = new QuickAccessAdapter(this,new QuickAccessDataSource(this));
        grid.setAdapter(quickAccessAdapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cur = (Cursor)grid.getItemAtPosition(position);
                cur.moveToFirst();
                String url = cur.getString(2);
                cur.close();
                mFocusEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                logoLayout.setVisibility(View.GONE);
                quickaccesspanel.setVisibility(View.GONE);
              //  menuButton.setVisibility(View.VISIBLE);
                mFocusEditText.setText(url);
                performSearch(url);

                RelativeLayout rlTB = (RelativeLayout) findViewById(R.id.textboxRL);
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTB.getLayoutParams();
                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                //lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                rlTB.setLayoutParams(lp);
            }
        });

        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Cursor cur = (Cursor)grid.getItemAtPosition(i);
                cur.moveToFirst();
                Long id = cur.getLong(0);
                String url = cur.getString(2);
                cur.close();

                showQuickAccessMenu(view,url,id);
                return false;
            }
        });


        registerForContextMenu(grid);
        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    //showIntroductoryOverlay();
                }

                if(newState == CastState.CONNECTED)
                {
                    if(PlayAfterConnect!=null)
                    {
                        castVideo = new CastVideo(WebViewActivity.this);
                        castVideo.setStatusListener(WebViewActivity.this);
                        castVideo.Cast(PlayAfterConnect);
                        PlayAfterConnect = null;
                    }
                }
            }
        };

        initAdblock();
        ChromecastApp.Instance().mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        inAppPurchase.onActivityResul(requestCode, resultCode, data);
    }
    private void initAdblock() {
        webView.setDebugMode(true);
        // render as fast as we can
        webView.setAllowDrawDelay(0);

        AdblockHelper.get().retain(true);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        getMenuInflater().inflate(R.menu.quick_access_context_menu, menu);
        GridView gv = (GridView) v;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int position = info.position;

    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final GridView grid = (GridView)findViewById(R.id.quickaccess);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Cursor cur = (Cursor)grid.getItemAtPosition(info.position);
        cur.moveToFirst();
        Long id = cur.getLong(0);
        String url = cur.getString(2);
        cur.close();
        switch (item.getItemId()) {

            case R.id.menuOpen:
                mFocusEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                logoLayout.setVisibility(View.GONE);
                quickaccesspanel.setVisibility(View.GONE);
                // menuButton.setVisibility(View.VISIBLE);
                mFocusEditText.setText(url);
                performSearch(url);

                RelativeLayout rlTB = (RelativeLayout) findViewById(R.id.textboxRL);
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTB.getLayoutParams();
                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                //lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                rlTB.setLayoutParams(lp);
                return true;
            case R.id.menuRemove:
                dbHelper.deleteBookmark(id);
                if(quickAccessAdapter!=null)
                {
                    quickAccessAdapter.Update();
                    quickAccessAdapter.notifyDataSetChanged();
                }
                return true;
        }

        return false;
    }


    private void initToolbars() {
        mToolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setTitle(MainActivity.getAppTitle(this,getResources().getString(R.string.app_name)));

        //  getSupportActionBar().inflateMenu(R.menu.webview_menu);
       // bottomToolbar.getBackground().setAlpha(125);


    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return ChromecastApp.Instance().mCastContext.onDispatchVolumeKeyEventBeforeJellyBean(event)
                || super.dispatchKeyEvent(event);
    }

    private MenuItem mediaRouteMenuItem;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.webview_menu, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuHelp:
                startActivity(new Intent(getApplicationContext(),HelpActivity.class));
                return true;
            case R.id.menuShowVideoList:
                if(mFoundVideos.size()>0)
                {
                    //popupVideoList();
                    showVideoListDialog();
                }
                return true;
            case R.id.menuReload:
                if(mFocusEditText.getText().toString().trim().length()>0) {
                    performSearch(mFocusEditText.getText().toString().trim());
                }
                return true;
            case R.id.menuHistory:
                mFocusEditText.setText("web://history");
                performSearch(mFocusEditText.getText().toString().trim());
                return true;
            case R.id.menuBookmark: {
                //String url = mFocusEditText.getText().toString().trim();
                if(webViewPanel.getVisibility() == View.VISIBLE) {
                    String url = webView.getUrl();
                    if(!TextUtils.isEmpty(url)) {
                        String title = webView.getTitle();
                        Bitmap favicon = webView.getFavicon();

                        if (TextUtils.isEmpty(title)) {
                            title = Utils.getHost(url);
                        }

                        if (!dbHelper.isBookmarkExist(url)) {

                            dbHelper.addBookmark(title, url, false, favicon);
                        }
                    }
                }

                return true;
            }
            case R.id.menuQuickAccess: {
//                                String url = mFocusEditText.getText().toString().trim();
                //String url = mFocusEditText.getText().toString().trim();
                if(webViewPanel.getVisibility() == View.VISIBLE) {
                    String url = webView.getUrl();
                    if(!TextUtils.isEmpty(url)) {
                        String title = webView.getTitle();
                        Bitmap favicon = webView.getFavicon();

                        if (TextUtils.isEmpty(title)) {
                            title = Utils.getHost(url);
                        }

                        if (!dbHelper.isQuickAccessExist(url)) {
                            dbHelper.addBookmark(title, url, true, favicon);
                            quickAccessAdapter.notifyDataSetChanged();
                        }
                    }
                }
                return true;
            }
            case R.id.menuClearOnExit:{
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WebViewActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                boolean isclearonexit  = prefs.getBoolean("clearonexit",false);

                if(isclearonexit)
                {
                    editor.putBoolean("clearonexit",false);
                }
                else{
                    editor.putBoolean("clearonexit",true);
                }
                editor.commit();

                return true;
            }
            case R.id.menuClearHistory: {
                AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
                builder.setTitle(getString(R.string.clearhistory_title))
                        .setMessage(getString(R.string.clearhistory))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.clearhistory_button),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dbHelper.clearHistory();
                                        if(historyAdapter!=null)
                                        {
                                            historyAdapter.notifyDataSetChanged();
                                        }
                                    }
                                })
                        .setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                AlertDialog dialog = builder.create();
                if(!((Activity) WebViewActivity.this).isFinishing()) {
                    dialog.show();
                }

                return true;
            }
            case R.id.menuMyBookmark:
                mFocusEditText.setText("web://bookmarks");
                performSearch(mFocusEditText.getText().toString().trim());
                return true;
            case R.id.menuBlockAds: {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WebViewActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                boolean isadblock  = prefs.getBoolean("isadblock",false);

                boolean isUserPro  = prefs.getBoolean("remove_ads",false);

                if (isUserPro)
                {
                    if(isadblock)
                    {
                        isadblock = false;
                        editor.putBoolean("isadblock",isadblock);
                    }
                    else{
                        isadblock = true;
                        editor.putBoolean("isadblock",isadblock);
                    }
                    editor.commit();
                    webView.setAdblockEnabled(isadblock);
                }
                else
                {
                    showRemoveAdDialog();

                }


                return true;
            }
            case R.id.menuUAMobile:
               webView.setUserAgent(0);
                return true;
            case R.id.menuApple:
                webView.setUserAgent(1);
                return true;
            case R.id.menuFirefox:
               webView.setUserAgent(2);
                return true;
            case R.id.menuClearCookie:{
                clearCacheAndCookie();
            }
            default:
                return false;
        }
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
                        inAppPurchase.purchaseRemoveAds();
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
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem defaultitem = menu.findItem(R.id.menuUAMobile);

        MenuItem safariitem =  menu.findItem(R.id.menuApple);
        MenuItem firefoxitem =  menu.findItem(R.id.menuFirefox);
        MenuItem clearonexititem =  menu.findItem(R.id.menuClearOnExit);
        MenuItem adblockitem =  menu.findItem(R.id.menuBlockAds);
        MenuItem videolistitem =  menu.findItem(R.id.menuShowVideoList);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int useragentid = prefs.getInt("useragent", 0);
        boolean isclearonexit  = prefs.getBoolean("clearonexit",false);







        if(isclearonexit){
            clearonexititem.setChecked(true);
        }
        else{
            clearonexititem.setChecked(false);
        }
        boolean remove_ads  = prefs.getBoolean("remove_ads",false);


        if(remove_ads){
            boolean isadblock  = prefs.getBoolean("isadblock",false);
            if (isadblock)

             adblockitem.setChecked(true);
            else
                adblockitem.setChecked(false);
        }
        else{
            adblockitem.setChecked(false);
        }

        if(mFoundVideos.size()==0)
        {
            videolistitem.setEnabled(false);
        }
        else{
            videolistitem.setEnabled(true);
        }

        switch (useragentid) {
            case 0: {
                SpannableString s = new SpannableString(defaultitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), 0, s.length(), 0);
                defaultitem.setTitle(s);

                s = new SpannableString(safariitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)), 0, s.length(), 0);
                safariitem.setTitle(s);

                s = new SpannableString(firefoxitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)), 0, s.length(), 0);
                firefoxitem.setTitle(s);
            }
            break;
            case 1: {
                SpannableString s = new SpannableString(defaultitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)), 0, s.length(), 0);
                defaultitem.setTitle(s);

                s = new SpannableString(safariitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), 0, s.length(), 0);
                safariitem.setTitle(s);

                s = new SpannableString(firefoxitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)), 0, s.length(), 0);
                firefoxitem.setTitle(s);
            }
            break;
            case 2: {
                SpannableString s = new SpannableString(defaultitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)), 0, s.length(), 0);
                defaultitem.setTitle(s);

                s = new SpannableString(safariitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)), 0, s.length(), 0);
                safariitem.setTitle(s);

                s = new SpannableString(firefoxitem.getTitle());
                s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), 0, s.length(), 0);
                firefoxitem.setTitle(s);
            }
            break;
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void showQuickAccessMenu(View view, final String url, final long id)
        {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.quick_access_context_menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.menuOpen:
                        mFocusEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                        logoLayout.setVisibility(View.GONE);
                        quickaccesspanel.setVisibility(View.GONE);
                       // menuButton.setVisibility(View.VISIBLE);
                        mFocusEditText.setText(url);
                        performSearch(url);

                        RelativeLayout rlTB = (RelativeLayout) findViewById(R.id.textboxRL);
                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTB.getLayoutParams();
                        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        //lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        rlTB.setLayoutParams(lp);
                        return true;
                    case R.id.menuRemove:
                        dbHelper.deleteBookmark(id);
                        if(quickAccessAdapter!=null)
                        {
                            quickAccessAdapter.notifyDataSetChanged();
                        }
                        return true;
                }

                    return false;
            }
        });
    }

    private void popupVideoList()
    {
       // final Dialog dialog = new Dialog(this);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_video_title))
                .setView(R.layout.video_list)
                .create();
      //  dialog.setContentView(R.layout.video_list);
       // dialog.setTitle(getString(R.string.select_video_title));
        dialog.setCancelable(true);
        if(!((Activity) WebViewActivity.this).isFinishing()) {
            dialog.show();
        }
        ListView list = (ListView) dialog.findViewById(R.id.videoListView);
        list.setAdapter(new VideoListAdapter(this,mFoundVideos));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mediaRouteMenuItem.isVisible() == true) {
                    if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                        Video video = mFoundVideos.get(i);
                        dialog.cancel();


                        castVideo = new CastVideo(WebViewActivity.this);
                        castVideo.setStatusListener(WebViewActivity.this);
                        castVideo.Cast(video);
                    } else {
                        if (mediaRouteMenuItem != null) {
                            ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);

                            Video video = mFoundVideos.get(i);
                            dialog.cancel();

                            PlayAfterConnect = video;
                            provider.onPerformDefaultAction();
                        }
                        //Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    dialog.cancel();
                    if(!((Activity) WebViewActivity.this).isFinishing()) {
                        Toast.makeText(WebViewActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

    }


    private void clearCacheAndCookie()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else
        {
            CookieSyncManager cookieSyncMngr=CookieSyncManager.createInstance(this);
            cookieSyncMngr.startSync();
            CookieManager cookieManager=CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
            cookieSyncMngr.startSync();
        }

        webView.clearCache(true);
    }

    private void performSearch(String search) {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator();
        if (urlValidator.isValid(search)) {
            loadUrl(search);

        }  else if (search.contentEquals("web://history") == true) {
            loadHistory();
        } else if (search.contentEquals("web://bookmarks")) {
            loadBookmarks();
        }
        else if (search.startsWith("http") == false) {
            String testurl = "http://" + search;
            if (urlValidator.isValid(testurl)) {
                loadUrl(testurl);

            } else {
                String request = "https://www.google.com/search?q=" + search;
                loadUrl(request);
            }
        }
        else {
            String request = "https://www.google.com/search?q=" + search;
            loadUrl(request);
        }
    }

    private void loadUrl(String url) {
        // webView.setWebViewClient(new WebViewClient());
        webViewPanel.setVisibility(View.VISIBLE);
        historyPanel.setVisibility(View.GONE);
        quickaccesspanel.setVisibility(View.GONE);

        int a = 0;
        AdblockHelper.get().waitForReady();

//        org.adblockplus.libadblockplus.android.Subscription[] subscriptions = AdblockHelper.get().getEngine().getListedSubscriptions();
//        List<String> urls = new ArrayList<String>();
//        for (org.adblockplus.libadblockplus.android.Subscription subscription:subscriptions
//                ) {
//            urls.add(subscription.url);
//        }
//
//        urls.add("https://www.fanboy.co.nz/fanboy-indian.txt");
//
//        AdblockHelper.get().getEngine().setSubscriptions(urls);
//        subscriptions = AdblockHelper.get().getEngine().getListedSubscriptions();

        webView.setAdblockEngine(AdblockHelper.get().getEngine());

        webView.loadUrl(url);

        RelativeLayout rlTB = (RelativeLayout) findViewById(R.id.textboxRL);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTB.getLayoutParams();
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        //lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
        rlTB.setLayoutParams(lp);
    }

    @Override
    protected void onPause() {
        ChromecastApp.Instance().mCastContext.removeCastStateListener(mCastStateListener);
        ChromecastApp.Instance().mCastContext.getSessionManager().removeSessionManagerListener(
                mSessionManagerListener, CastSession.class);
        super.onPause();
        mFocusEditText.clearFocus();

        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.getInstance().stopSync();
        }


        try {
            Class.forName("android.webkit.WebView")
                    .getMethod("onPause", (Class[]) null)
                    .invoke(webView, (Object[]) null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {

        ChromecastApp.Instance().mCastContext.addCastStateListener(mCastStateListener);
        ChromecastApp.Instance().mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);

        super.onResume();
        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.getInstance().startSync();
        }

        try {
            Class.forName("android.webkit.WebView")
                    .getMethod("onResume", (Class[]) null)
                    .invoke(webView, (Object[]) null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mFocusEditText.isFocused() == false) {

            if(keyCode == KeyEvent.KEYCODE_BACK ) {
                if (webViewPanel.getVisibility() != View.VISIBLE && historyPanel.getVisibility() != View.VISIBLE) {
                    finish();
                } else if (historyPanel.getVisibility() == View.VISIBLE) {
                    String lastUrl = webView.getUrl();
                    mFocusEditText.setText(lastUrl);
                    historyPanel.setVisibility(View.GONE);
                    webViewPanel.setVisibility(View.VISIBLE);
                    return true;
                } else if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                } else {

                    finish();
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    AlertDialog findFilesDialog = null;
    @Override
    public void FindVideo(final Video video) {
        WebViewActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                if (TextUtils.isEmpty(video.getTitle())) {
                    video.setTitle(webView.getTitle());
                }
                mFoundVideos.add(video);
                String format = getString(R.string.dialog_new_video_link_found);
                String message = String.format(format, video.getTitle());
                Log.v("suraj","video link found:"+video.getUrl());
                Log.v("suraj","video LocalPath found:"+video.getLocalPath());
                AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
                builder.setTitle(getString(R.string.dialog_new_video_link_found_title))
                        .setMessage(message)
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.cast),
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int id) {
                                        if(mediaRouteMenuItem.isVisible() == true) {
                                            if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {

                                                dialog.cancel();
                                                castVideo = new CastVideo(WebViewActivity.this);
                                                castVideo.setStatusListener(WebViewActivity.this);
                                                castVideo.Cast(video);
                                            } else {

                                                dialog.cancel();

                                                PlayAfterConnect = video;
                                                ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                                                provider.onPerformDefaultAction();
                                                // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                                            }
                                        }
                                        else{
                                            dialog.cancel();
                                            if(!((Activity) WebViewActivity.this).isFinishing()) {
                                                Toast.makeText(WebViewActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                })
                        .setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                if(findFilesDialog!=null) {
                    findFilesDialog.dismiss();
                }
                    findFilesDialog = builder.create();
                if(!((Activity) WebViewActivity.this).isFinishing()) {
                    findFilesDialog.show();
                }

            }
        });
    }


    private int selectedVideo = 0;

    private void showVideoListDialog()
    {
        final ArrayList<Video> videos = new ArrayList<>(mFoundVideos);
        Collections.reverse(videos);
        List<CharSequence> titles = new ArrayList<CharSequence>();
        for (Video video : videos) {
            Log.d("ChromecastApp", video.getTitle() + " || " + video.getUrl());
            if (TextUtils.isEmpty(video.getTitle())) {
                video.setTitle(PageTitle);
            }
            titles.add(video.getTitle());
        }
        selectedVideo = 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
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
                                if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                                    castVideo = new CastVideo(WebViewActivity.this);
                                    castVideo.setStatusListener(WebViewActivity.this);
                                    castVideo.Cast(videos.get(selectedVideo));
                                    selectedVideo = -1;
                                } else {

                                    dialog.cancel();
                                    Video video = videos.get(selectedVideo);
                                    PlayAfterConnect = video;
                                    ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                                    provider.onPerformDefaultAction();


                                    // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            dialog.cancel();
                            if (!((Activity) WebViewActivity.this).isFinishing()) {
                                Toast.makeText(WebViewActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
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

        if (findFilesDialog != null) {
            findFilesDialog.dismiss();
        }
        findFilesDialog = builder.create();
        if (!((Activity) WebViewActivity.this).isFinishing()) {
            findFilesDialog.show();
        }
    }

    @Override
    public void FindVideos(final ArrayList<Video> videos) {
        final int oldsize = mFoundVideos.size();
        mFoundVideos.clear();
        mFoundVideos.addAll(videos);
        final int newsize = mFoundVideos.size();

        WebViewActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                if(newsize-oldsize>0) {
                   // String message = String.format(getString(R.string.find_new_videos));
                    String message = getString(R.string.find_new_videos);
                    Toast.makeText(WebViewActivity.this,message, Toast.LENGTH_SHORT).show();
                }

                List<CharSequence> titles = new ArrayList<CharSequence>();
                for (Video video : videos) {
                    Log.d("ChromecastApp", video.getTitle() + " || " + video.getUrl());
                    if (TextUtils.isEmpty(video.getTitle())) {
                        video.setTitle(PageTitle);
                    }
                    titles.add(video.getTitle());
                }
                selectedVideo = 0;
                AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
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
                                        if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                                            castVideo = new CastVideo(WebViewActivity.this);

                                            castVideo.setStatusListener(WebViewActivity.this);
                                            castVideo.Cast(videos.get(selectedVideo));
                                            selectedVideo = -1;
                                        } else {

                                            dialog.cancel();
                                            Video video = videos.get(selectedVideo);
                                            PlayAfterConnect = video;
                                            ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                                            provider.onPerformDefaultAction();


                                            // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                                        }
                                    }
                                } else {
                                    dialog.cancel();
                                    if (!((Activity) WebViewActivity.this).isFinishing()) {
                                        Toast.makeText(WebViewActivity.this, getString(R.string.not_device), Toast.LENGTH_LONG).show();
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
                if (!((Activity) WebViewActivity.this).isFinishing()) {
                //    findFilesDialog.show();
                }
            }
        });
    }

    @Override
    public void SetTitle(String title) {
        PageTitle = title;
    }

    @Override
    protected void onDestroy(){
        Log.d("Chromecast", "Destroy");
        inAppPurchase.onDestroy();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isclearonexit  = prefs.getBoolean("clearonexit",false);
        if(isclearonexit)
        {
            clearCacheAndCookie();
        }

        if(webView!=null) {
            webView.dispose(new Runnable() {
                @Override
                public void run() {
                    AdblockHelper.get().release();
                }
            });
        }

        super.onDestroy();
    }

    @Override
    public void setUrl(final String str) {
       // PageTitle = "";
    //    Toast.makeText(WebViewActivity.this, "m1962b: " + str, Toast.LENGTH_LONG).show();
        WebViewActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                mFocusEditText.setText(str);
                Log.d("ChromecastApp", "Client: " + str + " WebView: " + webView.getUrl());
                mFoundVideos.clear();
            }
        });

    }

    public void setHistory(final String str)
    {
        WebViewActivity.this.runOnUiThread(new Runnable() {
            public void run() {

                String url = webView.getUrl();
                String title = webView.getTitle();
                Bitmap favicon = webView.getFavicon();

                if(TextUtils.isEmpty(title))
                {
                    title = Utils.getHost(url);
                }

                if (!TextUtils.isEmpty(url)) {
                    if (dbHelper.isHistroryExist(url.trim())) {
                        dbHelper.deleteHistory(url.trim());
                    }

                        dbHelper.addHistory(title, url.trim(), favicon);

                }
            }
        });

        if(Build.VERSION.SDK_INT<21)
        {
            CookieSyncManager.getInstance().sync();
        }
        else
        {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.flush();
        }
    }

    public void setBitmap(Bitmap bitmap)
    {

        Log.d("Chromecast", "setBitmap");
        PageBitmap = bitmap;

        WebViewActivity.this.runOnUiThread(new Runnable() {
            public void run() {

                String url = webView.getUrl();
                String title = webView.getTitle();
                Bitmap favicon = webView.getFavicon();

                if(TextUtils.isEmpty(title))
                {
                    title = Utils.getHost(url);
                }

                if (!TextUtils.isEmpty(url)) {
                    if (dbHelper.isHistroryExist(url.trim())) {
                        dbHelper.deleteHistory(url.trim());
                    }

                    dbHelper.addHistory(title, url.trim(), favicon);

                }
            }
        });
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onUpdateProgress(int progress) {
        progressBar.setProgress(progress);
        if (progress == 100) {
            Log.d("Chromecast", "FINISH");

            progressBar.setVisibility(View.INVISIBLE);

        }
    }

    @Override
    public void onPageFinished() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onPageStarted() {
        progressBar.setVisibility(View.VISIBLE);
    }

    public void loadHistory()
    {
        webViewPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.VISIBLE);

        ListView listView = (ListView)findViewById(R.id.listView);
        historyAdapter = new HistoryAdapter(this,new HistoryDataSource(this));
        listView.setAdapter(historyAdapter);
    }

    public void loadBookmarks()
    {
        webViewPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.VISIBLE);

        ListView listView = (ListView)findViewById(R.id.listView);
       bookmarkAdapter = new BookmarkAdapter(this,new BookmarkDataSource(this));
        listView.setAdapter(bookmarkAdapter);
    }


    private int f1537u;
    private WebChromeClient.CustomViewCallback f1539w;
    private View f1542z;
    private FrameLayout f1540x;
    private VideoView f1541y;
    public synchronized void m1954a(View view, int i, WebChromeClient.CustomViewCallback customViewCallback) {
        if (view != null) {
            if (this.f1542z == null) {
                try {
                    view.setKeepScreenOn(true);
                } catch (SecurityException e) {
                    //FirebaseCrash.report(new Exception("WebView is not allowed to keep the screen on: " + e.getClass().getSimpleName()));
                }
                this.f1537u = getRequestedOrientation();
                this.f1539w = customViewCallback;
                this.f1542z = view;
                setRequestedOrientation(i);
                FrameLayout frameLayout = (FrameLayout) getWindow().getDecorView();
                this.f1540x = new FrameLayout(this);
                this.f1540x.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
                if (view instanceof FrameLayout) {
                    if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                        this.f1541y = (VideoView) ((FrameLayout) view).getFocusedChild();
                        this.f1541y.setOnErrorListener(new C1214h(this));
                        this.f1541y.setOnCompletionListener(new C1214h(this));
                    }
                } else if (view instanceof VideoView) {
                    this.f1541y = (VideoView) view;
                    this.f1541y.setOnErrorListener(new C1214h(this));
                    this.f1541y.setOnCompletionListener(new C1214h(this));
                }
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1);
                frameLayout.addView(this.f1540x, layoutParams);
                this.f1540x.addView(this.f1542z, layoutParams);
                frameLayout.requestLayout();
               // this.c = true;
               // m1948a();
            }
        }
        if (customViewCallback != null) {
            try {
                customViewCallback.onCustomViewHidden();
            } catch (Exception e2) {
                //FirebaseCrash.report(new Exception("Error hiding custom view: " + e2.getClass().getSimpleName()));
            }
        }
    }

    public void m2068e() {
        if (this.f1540x != null) {
            this.f1540x.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            ViewGroup viewGroup = (ViewGroup) this.f1540x.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(this.f1540x);
            }
            this.f1540x.removeAllViews();
        }
        if (this.f1542z != null && this.f1539w != null) {
            try {
                this.f1542z.setKeepScreenOn(false);
            } catch (SecurityException e) {
               // FirebaseCrash.report(new Exception("WebView is not allowed to keep the screen on: " + e.getClass().getSimpleName()));
            }
            this.f1540x = null;
            this.f1542z = null;
            if (this.f1541y != null) {
                this.f1541y.stopPlayback();
                this.f1541y.setOnErrorListener(null);
                this.f1541y.setOnCompletionListener(null);
                this.f1541y = null;
            }
            if (this.f1539w != null) {
                try {
                    this.f1539w.onCustomViewHidden();
                } catch (Exception e2) {
                  //  FirebaseCrash.report(new Exception("Error hiding custom view: " + e2.getClass().getSimpleName()));
                }
            }
            this.f1539w = null;
            //this.c = this.a.m2472n();
            //m1948a();
            setRequestedOrientation(this.f1537u);
        } else if (this.f1539w != null) {
            try {
                this.f1539w.onCustomViewHidden();
            } catch (Exception e22) {
                //FirebaseCrash.report(new Exception("Error hiding custom view: " + e22.getClass().getSimpleName()));
            }
            this.f1539w = null;
        }
    }


    public synchronized void m1955a(View view, WebChromeClient.CustomViewCallback customViewCallback) {
        int requestedOrientation = getRequestedOrientation();
        this.f1537u = requestedOrientation;
        m1954a(view, requestedOrientation, customViewCallback);
    }

    private class C1214h implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        final /* synthetic */ WebViewActivity f1498a;

        private C1214h(WebViewActivity mainActivity) {
            this.f1498a = mainActivity;
        }

        public void onCompletion(MediaPlayer mediaPlayer) {
            this.f1498a.m2068e();
        }

        public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
            return false;
        }
    }

    protected void handleMenuSearch(){
        ActionBar action = getSupportActionBar(); //get the actionbar

        if(isSearchOpened){ //test if the search is open

            action.setDisplayShowCustomEnabled(false); //disable a custom view inside the actionbar
            action.setDisplayShowTitleEnabled(true); //show the title in the action bar

            //hides the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edtSeach.getWindowToken(), 0);

            //add the search icon in the action bar
            //mSearchAction.setIcon(getResources().getDrawable(R.drawable.ic_open_search));

            isSearchOpened = false;
        } else { //open the search entry

            action.setDisplayShowCustomEnabled(true); //enable it to display a
            // custom view in the action bar.
            action.setCustomView(R.layout.search_bar);//add the custom view
            action.setDisplayShowTitleEnabled(false); //hide the title

            edtSeach = (EditText)action.getCustomView().findViewById(R.id.edtSearch); //the text editor

            //this is a listener to do a search when the user clicks on search button
            edtSeach.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        //doSearch();
                        return true;
                    }
                    return false;
                }
            });


            edtSeach.requestFocus();

            //open the keyboard focused in the edtSearch
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(edtSeach, InputMethodManager.SHOW_IMPLICIT);


            //add the close icon
            //mSearchAction.setIcon(getResources().getDrawable(R.drawable.ic_close_search));

            isSearchOpened = true;
        }
    }
}
