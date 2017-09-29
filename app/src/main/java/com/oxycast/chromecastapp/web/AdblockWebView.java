/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2016 Eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oxycast.chromecastapp.web;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;  // makes android min version to be 21
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.oxycast.chromecastapp.Ads.AdBlock;
import com.oxycast.chromecastapp.Ads.AdsBlockHelper;
import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.media.DailyMotionUriExtractor;
import com.oxycast.chromecastapp.media.IWebBrowserResult;
import com.oxycast.chromecastapp.media.Video;
import com.oxycast.chromecastapp.media.VimeoUriExtractor;
import com.oxycast.chromecastapp.media.XVideoUriExtractor;

import org.adblockplus.libadblockplus.FilterEngine;
import org.adblockplus.libadblockplus.Subscription;
import org.adblockplus.libadblockplus.android.AdblockEngine;
import org.adblockplus.libadblockplus.android.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * WebView with ad blocking
 */
public class AdblockWebView extends WebView
{
  private static final String TAG = Utils.getTag(AdblockWebView.class);

  /**
   * Default (in some conditions) start redraw delay after DOM modified with injected JS (millis)
   */
  public static final int ALLOW_DRAW_DELAY = 200;
  /*
     The value could be different for devices and completely unclear why we need it and
     how to measure actual value
  */

  protected static final String HEADER_REFERRER = "Referer";
  protected static final String HEADER_REQUESTED_WITH = "X-Requested-With";
  protected static final String HEADER_REQUESTED_WITH_XMLHTTPREQUEST = "XMLHttpRequest";

  private static final String BRIDGE_TOKEN = "{{BRIDGE}}";
  private static final String DEBUG_TOKEN = "{{DEBUG}}";
  private static final String HIDE_TOKEN = "{{HIDE}}";
  private static final String BRIDGE = "jsBridge";
  private static final String[] EMPTY_ARRAY = {};
  private static final String EMPTY_ELEMHIDE_ARRAY_STRING = "[]";

  private static final Pattern RE_JS = Pattern.compile("\\.js$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RE_CSS = Pattern.compile("\\.css$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RE_IMAGE = Pattern.compile("\\.(?:gif|png|jpe?g|bmp|ico)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RE_FONT = Pattern.compile("\\.(?:ttf|woff)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RE_HTML = Pattern.compile("\\.html?$", Pattern.CASE_INSENSITIVE);
  private static String currentHost="";
  private volatile boolean addDomListener = true;
  private boolean adblockEnabled = true;
  private boolean debugMode;
  private AdblockEngine adblockEngine;
  private boolean disposeEngine;
  private Integer loadError;
  private int allowDrawDelay = ALLOW_DRAW_DELAY;
  private WebChromeClient extWebChromeClient;
  private WebViewClient extWebViewClient;
  private WebViewClient intWebViewClient;
  private Map<String, String> url2Referrer = Collections.synchronizedMap(new HashMap<String, String>());
  private String url;
  private String domain;
  private String injectJs;
  private CountDownLatch elemHideLatch;
  private String elemHideSelectorsString;
  private Object elemHideThreadLockObject = new Object();
  private ElemHideThread elemHideThread;
  private boolean loading;
  private volatile boolean elementsHidden = false;
  private final Handler handler = new Handler();

  // used to prevent user see flickering for elements to hide
  // for some reason it's rendered even if element is hidden on 'dom ready' event
  private volatile boolean allowDraw = true;
  private AdBlock mAdBlock;
  AdsBlockHelper mAdBlock2;
  Context context;
  private List<String> whitelist;
  private IWebBrowserResult callback;

  public void setWebBrowserListener(IWebBrowserResult callback)
  {
    this.callback = callback;
  }
  public AdblockWebView(Context context)
  {
    super(context);
    this.context = context;

    mAdBlock = AdBlock.getInstance(context.getApplicationContext());
    mAdBlock2 = new AdsBlockHelper(context);
    prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    initAbp();
  }

  public AdblockWebView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    this.context = context;
    mAdBlock = AdBlock.getInstance(context.getApplicationContext());
    mAdBlock2 = new AdsBlockHelper(context);
    prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    initAbp();
  }
  SharedPreferences prefs = null;
  public AdblockWebView(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
    this.context = context;
    mAdBlock = AdBlock.getInstance(context.getApplicationContext());
    mAdBlock2 = new AdsBlockHelper(context);
    prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    initAbp();
  }

  public void UpdateAdBlock()
  {
    if(mAdBlock!=null) {
      mAdBlock.updatePreference();

    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean blockAds = prefs.getBoolean("isadblock",false);
  }

  public void setUserAgent(int userAgentid)
  {
    String useragent = "";

    switch (userAgentid)
    {
      case 0:
        useragent = this.context.getString(R.string.uachromemobile);
        break;
      case 1:
        useragent = this.context.getString(R.string.uasafari);
        break;
      case 2:
        useragent = this.context.getString(R.string.uafirefox);
        break;
    }

    getSettings().setUserAgentString(useragent);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt("useragent", userAgentid);
    editor.commit();
  }
  /**
   * Warning: do not rename (used in injected JS by method name)
   * @param value set if one need to set DOM listener
   */
  @JavascriptInterface
  public void setAddDomListener(boolean value)
  {
    d("addDomListener=" + value);
    this.addDomListener = value;
  }

  @JavascriptInterface
  public boolean getAddDomListener()
  {
    return addDomListener;
  }

  public boolean isAdblockEnabled()
  {
    return adblockEnabled;
  }

  private void applyAdblockEnabled()
  {
    super.setWebViewClient(adblockEnabled ? intWebViewClient : extWebViewClient);
    super.setWebChromeClient(adblockEnabled ? intWebChromeClient : extWebChromeClient);
  }

  public void setAdblockEnabled(boolean adblockEnabled)
  {
    this.adblockEnabled = adblockEnabled;
    applyAdblockEnabled();

    UpdateAdBlock();
  }

  @Override
  public void setWebChromeClient(WebChromeClient client)
  {
    extWebChromeClient = client;

    applyAdblockEnabled();
  }

  public boolean isDebugMode()
  {
    return debugMode;
  }

  /**
   * Set to true to see debug log output int AdblockWebView and JS console
   * @param debugMode is debug mode
   */
  public void setDebugMode(boolean debugMode)
  {
    this.debugMode = debugMode;
  }

  private void d(String message)
  {
    if (debugMode)
    {
      Log.d(TAG, message);
    }
  }

  private void w(String message)
  {
    if (debugMode)
    {
      Log.w(TAG, message);
    }
  }

  private void e(String message, Throwable t)
  {
    Log.e(TAG, message, t);
  }

  private void e(String message)
  {
    Log.e(TAG, message);
  }

  private String readScriptFile(String filename) throws IOException
  {
    return Utils
            .readAssetAsString(getContext(), filename)
            .replace(BRIDGE_TOKEN, BRIDGE)
            .replace(DEBUG_TOKEN, (debugMode ? "" : "//"));
  }

  private void runScript(String script)
  {
    d("runScript started");
    if(Build.VERSION.SDK_INT >=19) {
      evaluateJavascript(script, null);
    }
    d("runScript finished");
  }

  public AdblockEngine getAdblockEngine()
  {
    return adblockEngine;
  }

  /**
   * Set external adblockEngine. A new (internal) is created automatically if not set
   * Don't forget to invoke {@link #dispose(Runnable)} later and dispose external adblockEngine
   * @param adblockEngine external adblockEngine
   */
  public void setAdblockEngine(final AdblockEngine adblockEngine)
  {
    if (this.adblockEngine != null && adblockEngine != null && this.adblockEngine == adblockEngine)
    {
      return;
    }

    final Runnable setRunnable = new Runnable()
    {
      @Override
      public void run()
      {
        AdblockWebView.this.adblockEngine = adblockEngine;
        AdblockWebView.this.disposeEngine = false;
      }
    };

    if (this.adblockEngine != null && disposeEngine)
    {
      // as adblockEngine can be busy with elemhide thread we need to use callback
      this.dispose(setRunnable);
    }
    else
    {
      setRunnable.run();
    }
  }

  private WebChromeClient intWebChromeClient = new WebChromeClient()
  {
    // Fullscreen
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private int mOriginalOrientation;

    private FrameLayout mFullscreenContainer;

    private final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);


    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        if (mCustomView != null) {
          callback.onCustomViewHidden();
          return;
        }

        mOriginalOrientation = ((Activity)context).getRequestedOrientation();
        FrameLayout decor = (FrameLayout) ((Activity)context).getWindow().getDecorView();
        mFullscreenContainer = new FullscreenHolder(context);
        mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        mCustomView = view;
        setFullscreen(true);
        mCustomViewCallback = callback;
//          mActivity.setRequestedOrientation(requestedOrientation);
      }

      super.onShowCustomView(view, callback);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {
      this.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
      if (mCustomView == null) {
        return;
      }

      setFullscreen(false);
      FrameLayout decor = (FrameLayout) ((Activity)context).getWindow().getDecorView();
      decor.removeView(mFullscreenContainer);
      mFullscreenContainer = null;
      mCustomView = null;
      mCustomViewCallback.onCustomViewHidden();
      ((Activity)context).setRequestedOrientation(mOriginalOrientation);
    }

    private void setFullscreen(boolean enabled) {
      Window win = ((Activity)context).getWindow();
      WindowManager.LayoutParams winParams = win.getAttributes();
      final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
      if (enabled) {
        winParams.flags |= bits;
      } else {
        winParams.flags &= ~bits;
        if (mCustomView != null) {
          mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
      }
      win.setAttributes(winParams);
    }

    class FullscreenHolder extends FrameLayout {
      public FullscreenHolder(Context ctx) {
        super(ctx);
        setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.black));
      }

      @Override
      public boolean onTouchEvent(MotionEvent evt) {
        return true;
      }
    }




    //



    @Override
    public void onReceivedTitle(WebView view, String title)
    {
      callback.SetTitle(title);
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onReceivedTitle(view, title);
      }
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon)
    {
      callback.setBitmap(icon);
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onReceivedIcon(view, icon);
      }
    }

    @Override
    public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed)
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
      }
    }

    /* @Override
     public void onShowCustomView(View view, CustomViewCallback callback)
     {
       if (extWebChromeClient != null)
       {
         extWebChromeClient.onShowCustomView(view, callback);
       }
     }

     @Override
     public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback)
     {
       if (extWebChromeClient != null)
       {
         extWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
       }
     }

     @Override
     public void onHideCustomView()
     {
       if (extWebChromeClient != null)
       {
         extWebChromeClient.onHideCustomView();
       }
     }
 */
    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                  Message resultMsg)
    {
      if (extWebChromeClient != null)
      {
        return extWebChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
      }
      else
      {
        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
      }
    }

    @Override
    public void onRequestFocus(WebView view)
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onRequestFocus(view);
      }
    }

    @Override
    public void onCloseWindow(WebView window)
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onCloseWindow(window);
      }
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result)
    {
      if (extWebChromeClient != null)
      {
        return extWebChromeClient.onJsAlert(view, url, message, result);
      }
      else
      {
        return super.onJsAlert(view, url, message, result);
      }
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, JsResult result)
    {
      if (extWebChromeClient != null)
      {
        return extWebChromeClient.onJsConfirm(view, url, message, result);
      }
      else
      {
        return super.onJsConfirm(view, url, message, result);
      }
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                              JsPromptResult result)
    {
      if (extWebChromeClient != null)
      {
        return extWebChromeClient.onJsPrompt(view, url, message, defaultValue, result);
      }
      else
      {
        return super.onJsPrompt(view, url, message, defaultValue, result);
      }
    }

    @Override
    public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result)
    {
      if (extWebChromeClient != null)
      {
        return extWebChromeClient.onJsBeforeUnload(view, url, message, result);
      }
      else
      {
        return super.onJsBeforeUnload(view, url, message, result);
      }
    }

    @Override
    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota,
                                        long estimatedDatabaseSize, long totalQuota,
                                        WebStorage.QuotaUpdater quotaUpdater)
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota,
                estimatedDatabaseSize, totalQuota, quotaUpdater);
      }
      else
      {
        super.onExceededDatabaseQuota(url, databaseIdentifier, quota,
                estimatedDatabaseSize, totalQuota, quotaUpdater);
      }
    }

    @Override
    public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
                                         WebStorage.QuotaUpdater quotaUpdater)
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
      }
      else
      {
        super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
      }
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin,
                                                   GeolocationPermissions.Callback callback)
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
      }
      else
      {
        super.onGeolocationPermissionsShowPrompt(origin, callback);
      }
    }

    @Override
    public void onGeolocationPermissionsHidePrompt()
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onGeolocationPermissionsHidePrompt();
      }
      else
      {
        super.onGeolocationPermissionsHidePrompt();
      }
    }

    @Override
    public boolean onJsTimeout()
    {
      if (extWebChromeClient != null)
      {
        return extWebChromeClient.onJsTimeout();
      }
      else
      {
        return super.onJsTimeout();
      }
    }

    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID)
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.onConsoleMessage(message, lineNumber, sourceID);
      }
      else
      {
        super.onConsoleMessage(message, lineNumber, sourceID);
      }
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage)
    {
      d("JS: level=" + consoleMessage.messageLevel()
              + ", message=\"" + consoleMessage.message() + "\""
              + ", sourceId=\"" + consoleMessage.sourceId() + "\""
              + ", line=" + consoleMessage.lineNumber());

      if (extWebChromeClient != null)
      {
        return extWebChromeClient.onConsoleMessage(consoleMessage);
      }
      else
      {
        return super.onConsoleMessage(consoleMessage);
      }
    }

    @Override
    public Bitmap getDefaultVideoPoster()
    {
      if (extWebChromeClient != null)
      {
        return extWebChromeClient.getDefaultVideoPoster();
      }
      else
      {
        return super.getDefaultVideoPoster();
      }
    }

    @Override
    public View getVideoLoadingProgressView()
    {
      if (extWebChromeClient != null)
      {
        return extWebChromeClient.getVideoLoadingProgressView();
      }
      else
      {
        return super.getVideoLoadingProgressView();
      }
    }

    @Override
    public void getVisitedHistory(ValueCallback<String[]> callback)
    {
      if (extWebChromeClient != null)
      {
        extWebChromeClient.getVisitedHistory(callback);
      }
      else
      {
        super.getVisitedHistory(callback);
      }
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress)
    {
      d("Loading progress=" + newProgress + "%");
      mListener.onUpdateProgress(newProgress);

      // addDomListener is changed to 'false' in `setAddDomListener` invoked from injected JS
      if (getAddDomListener() && loadError == null && injectJs != null)
      {
        d("Injecting script");
        runScript(injectJs);

        if (allowDraw && loading)
        {
          startPreventDrawing();
        }
      }

      if (extWebChromeClient != null)
      {
        extWebChromeClient.onProgressChanged(view, newProgress);
      }
    }
  };

  public int getAllowDrawDelay()
  {
    return allowDrawDelay;
  }

  /**
   * Set start redraw delay after DOM modified with injected JS
   * (used to prevent flickering after 'DOM ready')
   * @param allowDrawDelay delay (in millis)
   */
  public void setAllowDrawDelay(int allowDrawDelay)
  {
    if (allowDrawDelay < 0)
    {
      throw new IllegalArgumentException("Negative value is not allowed");
    }

    this.allowDrawDelay = allowDrawDelay;
  }

  @Override
  public void setWebViewClient(WebViewClient client)
  {
    extWebViewClient = client;
    applyAdblockEnabled();
  }

  private void clearReferrers()
  {
    d("Clearing referrers");
    url2Referrer.clear();
  }

  /**
   * WebViewClient for API 21 and newer
   * (has Referrer since it overrides `shouldInterceptRequest(..., request)` with referrer)
   */
  private class AdblockWebViewClient extends WebViewClient
  {
    /////// Search Link //////////
    HashSet<String> urls = new HashSet<String>();;
    private String param;//f1577e;
    private String host; //f1580h
    ArrayList<Video> videoList = new ArrayList<Video>();;
    private int videoCount;
    boolean isLoading = false;



    public AdblockWebViewClient() {


      videoList = new ArrayList<Video>();
      urls = new HashSet<String>();
    }



    private void YoutubeExtractLink() {
      final AdblockWebViewClient client = this;
      {
        //  this.f1575c.add(this.param);

        final String youtubeLink = "https://m.youtube.com/watch?v=" + this.param;

        Log.d("YoutubePerformance", "Start Link:" + youtubeLink);
        YouTubeUriExtractor ytEx = new YouTubeUriExtractor(context) {
          @Override
          public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
            if (ytFiles != null) {
              if(urls.contains(videoId) == false) {

                int[] itag = {37, 46, 22, 45, 44, 18, 43, 96, 95, 94, 93, 92, 299, 137, 298, 136, 135, 134, 133, 264};
                String downloadUrl = "";
                for (int i = 0; i < itag.length; i++) {
                  YtFile file = ytFiles.get(itag[i]);//.getUrl();
                  if (file != null) {
                    Log.d("Youtube", "ITAG: " + itag[i]);


                    urls.add(videoId);
                    downloadUrl = file.getUrl();

                    Video video = new Video(videoTitle, downloadUrl);

                    String mime = com.oxycast.chromecastapp.utils.Utils.getMimeType(downloadUrl);
                    if(TextUtils.isEmpty(mime))
                    {
                      mime = "video/mp4";
                      Log.d("Youtube", "Default mime");
                    }

                    video.setMimeType(mime);
                    Log.d("YoutubePerformance", "End Link:" + youtubeLink);
                    client.FindVideo(video);
                    //  new Thread(new AdblockWebViewClient.CheckMimeType(client, video)).start();
                    break;
                  }
                }
              }
            }
          }
        };

        ytEx.execute(youtubeLink);
        //C1329f.m2371b().m2372a(this.param).enqueue(new C12303(this));
        Log.d("ChromecastApp", "m2095a:" + this.param);
      }
    }
    private void DirectVideo(Video video) {
      new Thread(new AdblockWebViewClient.CheckMimeType(this, video)).start();
      Log.d("ChromecastApp", "m2097a: " + video.getUrl());
    }
    class CheckMimeType implements Runnable {
      final Video video;
      final AdblockWebViewClient webViewClient;

      CheckMimeType(AdblockWebViewClient webViewClient, Video video) {
        this.webViewClient = webViewClient;
        this.video = video;
      }

      public void run() {
        Exception exception;
        Throwable th;
        HttpURLConnection httpURLConnection = null;
        try {
          HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(this.video.getUrl()).openConnection();
          try {
            httpURLConnection2.connect();
            String contentType = httpURLConnection2.getContentType();
            if(this.video.getUrl().contains(".mpd"))
            {
              contentType = "application/dash+xml";
            }

            Log.d("CheckMime", "URL: " + video.getUrl() + " Mime: " + contentType);

            String url = httpURLConnection2.getURL().toString();
            String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType);
            Log.d("CheckMime", "URL: " + video.getUrl() + " extensionFromMimeType: " + extensionFromMimeType);

            if (TextUtils.isEmpty(extensionFromMimeType) || extensionFromMimeType.equals("mp4") ||extensionFromMimeType.equals("webm") || extensionFromMimeType.equals("mkv") || extensionFromMimeType.equals("ismv") || extensionFromMimeType.equals("m3u8") || extensionFromMimeType.equals("mpd")) {
              this.video.setMimeType(contentType);
              this.video.setUrl(url);

              // videoList.add(this.video);


              this.webViewClient.FindVideo(this.video);
            }
            if (httpURLConnection2 != null) {
              httpURLConnection2.disconnect();
            }
          } catch (Exception e) {
            Exception exception2 = e;
            httpURLConnection = httpURLConnection2;
            exception = exception2;
            try {
              exception.printStackTrace();
              if (httpURLConnection != null) {
                httpURLConnection.disconnect();
              }
            } catch (Throwable th2) {
              th = th2;
              if (httpURLConnection != null) {
                httpURLConnection.disconnect();
              }
              //throw th;
            }
          } catch (Throwable th3) {
            Throwable th4 = th3;
            httpURLConnection = httpURLConnection2;
            th = th4;
            if (httpURLConnection != null) {
              httpURLConnection.disconnect();
            }
            // throw th;
          }
        } catch (Exception e2) {
          exception = e2;
          exception.printStackTrace();
          if (httpURLConnection != null) {
            httpURLConnection.disconnect();
          }
        }
      }
    }

    private void FindVideo(Video video) {

      if(urls.contains(video.getUrl()) == false) {
        urls.add(video.getUrl());
        videoList.add(video);
        if (videoList.size() == 1) {
          callback.FindVideo(video);
        } else {
          callback.FindVideos(videoList);
        }

        this.videoCount++;
      }
    }

    class VimeoUriExtractorEx extends VimeoUriExtractor {
      final AdblockWebViewClient webViewClient;

      VimeoUriExtractorEx(AdblockWebViewClient webViewClient) {
        this.webViewClient = webViewClient;
      }

      @Override
      public void m2081a(ArrayList<Video> arrayList) {
        int size = arrayList.size();
        Video video = null;
        ArrayList<Integer> quality = new ArrayList<Integer>();
        for(Video v:arrayList)
        {
          String q = v.getQuality().replace("p","");
          quality.add(Integer.parseInt(q));
        }

        Collections.sort(quality);
        Collections.reverse(quality);

        String qualityMax = quality.get(0) + "p";

        for(Video v:arrayList)
        {
          if(v.getQuality().contentEquals(qualityMax)==true)
          {
            video = v;
          }
        }

        if(video == null)
        {
          video = arrayList.get(0);
        }
        if(urls.contains(video.getUrl()) == false) {
          urls.add(video.getUrl());
          videoList.add(video);
          if (videoList.size() == 1) {
            callback.FindVideo(video);
          } else {
            callback.FindVideos(videoList);
          }
        }
      /*      if(isLoading == false) {
                if (size == 1) {
                    Video video = (Video) arrayList.get(0);
                    this.webViewClient.callback.FindVideo(video);
                } else if (size > 1) {



                    this.webViewClient.callback.FindVideos(arrayList);
                }
            }*/

        //  this.webViewClient.callback.FindVideo(video);

            /* for (int i = 0; i < size; i++) {
                Video video = (Video) arrayList.get(i);
              //  c1332c.m2381a(this.webViewClient.f1574b.m2410c() == 0);
                this.webViewClient.callback.FindVideo(video);
            }*/


         /*   if (size == 1) {
                C1372e.m2535a(2131296504);
            } else if (size > 1) {
                C1372e.m2537a(2131296505, String.valueOf(size - 1));
            }*/
      }
    }

    class DailyMotionUriExtractorEx extends DailyMotionUriExtractor {
      final AdblockWebViewClient webViewClient;

      DailyMotionUriExtractorEx(AdblockWebViewClient webViewClient) {
        this.webViewClient = webViewClient;
      }

      @Override
      public void m2086a(ArrayList<Video> arrayList) {
        int size = arrayList.size();
        Video video = null;
        ArrayList<Integer> quality = new ArrayList<Integer>();
        for(Video v:arrayList)
        {
          String q = v.getQuality().replace("p","");
          quality.add(Integer.parseInt(q));
        }

        Collections.sort(quality);
        Collections.reverse(quality);

        String qualityMax = quality.get(0) + "p";

        for(Video v:arrayList)
        {
          if(v.getQuality().contentEquals(qualityMax)==true)
          {
            video = v;
          }
        }

        if(video == null)
        {
          video = arrayList.get(0);
        }
        if(urls.contains(video.getUrl()) == false) {
          urls.add(video.getUrl());
          videoList.add(video);
          if (videoList.size() == 1) {
            callback.FindVideo(video);
          } else {
            callback.FindVideos(videoList);
          }
        }

            /* this.f1571a.f1574b.m2408b();
            int size = arrayList.size();
            int i = 0;
            while (i < size) {
                C1332c c1332c = (C1332c) arrayList.get(i);
                c1332c.m2381a(i == 0);
                this.f1571a.f1578f.m1960a(c1332c);
                i++;
            }
            if (size == 2) {
                C1372e.m2535a(2131296504);
            } else if (size > 2) {
                C1372e.m2537a(2131296505, String.valueOf(size - 1));
            }
            */
          /*  videoList.addAll(arrayList);
            int size = arrayList.size();
            if(isLoading == false) {
            if(size == 1)
            {
                Video video = (Video) arrayList.get(0);
                this.webViewClient.callback.FindVideo(video);
            }
            else if(size>1){
                this.webViewClient.callback.FindVideos(arrayList);
            }}

            if(urls.contains(video.getUrl()) == false) {
                urls.add(video.getUrl());
                videoList.add(video);
                if (videoList.size() == 1) {
                    this.webViewClient.callback.FindVideo(video);
                } else {
                    this.webViewClient.callback.FindVideos(videoList);
                }
            }*/
      }
    }

    private void m2102b(String str) {
      new AdblockWebViewClient.VimeoUriExtractorEx(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{str});
      Log.d("ChromecastApp","Vimeo processing: " + str);

    }

    private void m2103c(String str) {
      new AdblockWebViewClient.DailyMotionUriExtractorEx(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{str});

      Log.d("ChromecastApp"," Dailymotion processing: " + str);

    }

    class XVideoUriExtractorEx extends XVideoUriExtractor {
      AdblockWebViewClient webViewClient;

      XVideoUriExtractorEx(AdblockWebViewClient webViewClient) {
        this.webViewClient = webViewClient;
      }

      @Override
      public void m2091a(ArrayList<Video> arrayList) {
        int size = arrayList.size();
        videoList.addAll(arrayList);
        if(isLoading == false) {
          if (size == 1) {
            Video video = (Video) arrayList.get(0);
            callback.FindVideo(video);
          } else if (size > 1) {
            callback.FindVideos(arrayList);
          }
        }
            /*for (int i = 0; i < size; i++) {
                Video c1332c = (C1332c) arrayList.get(i);
                c1332c.m2381a(c1332c.m2383b().equals("application/x-mpegURL"));
                this.f1572a.f1578f.m1960a(c1332c);
            }
            if (size == 2) {
                C1372e.m2535a(2131296504);
            } else if (size > 2) {
                C1372e.m2537a(2131296505, String.valueOf(size - 1));
            }
            */
      }
    }

    private void m2104d(String str) {
      new AdblockWebViewClient.XVideoUriExtractorEx(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{str});

      Log.d("ChromecastApp"," xvideos processing: " + str);
    }

    private void m2098a(String str) {
      //new Thread(new C12281(this, str)).start();

      Log.d("ChromecastApp", " other processing: " + str);
    }



    @TargetApi(21)
    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
      boolean isXmlHttpRequest = false;

      if(webResourceRequest.getRequestHeaders()!=null) {
        isXmlHttpRequest =
                webResourceRequest.getRequestHeaders().containsKey(HEADER_REQUESTED_WITH) &&
                        HEADER_REQUESTED_WITH_XMLHTTPREQUEST.equals(
                                webResourceRequest.getRequestHeaders().get(HEADER_REQUESTED_WITH));
      }

      Log.d("shouldOverride", webResourceRequest.getMethod() + " isXmlHttpRequest: " + isXmlHttpRequest + " URL: " +  webResourceRequest.getUrl().toString() );

      Map<String, String> headers = webResourceRequest.getRequestHeaders();
      if(headers!=null)
      {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          Log.d("shouldOverride",entry.getKey() + " : " + entry.getValue());
        }}

      return shouldOverrideUrlLoading(webView, webResourceRequest.getUrl().toString());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url)
    {

      Log.d("shouldOverride"," URL: " +  url + "WebView URL: " +view.getUrl() );

      if(url.contains("m.youtube.com") && url.contains("watch?ajax="))
      {
        //  urls.clear();
        videoList.clear();
      }

      HitTestResult hit= view.getHitTestResult();
      if(hit!=null)
      {
        Log.d("shouldOverride", "TYPE: " + hit.getType() + " Extra: " + hit.getExtra());
      }
      String webUrl;
      if(view.getUrl()!=null) {
        webUrl = view.getUrl().toString();
      }
      else {
        webUrl = url;
      }
      String webViewHost = com.oxycast.chromecastapp.utils.Utils.getHost(webUrl);

      String b = com.oxycast.chromecastapp.utils.Utils.getHost(url);
      boolean ifcontainhost = false;
      if(b != null)
      {
        ifcontainhost = b.contains(webViewHost);
      }

      Log.d("shouldOverride", "HOST: " + b + " WEBVIEW: " + webViewHost);
      if (mAdBlock.isAd(url) || mAdBlock2.isAdsHost(url) || ((hit.getType() != 7 || hit.getType() !=8) && hit.getExtra()==null && ifcontainhost ==false )) {
        //   C1372e.m2541a(this.f1579g, 2131296515, b);
        Log.d("ChromecastApp", "it is ads");
        if(!url.startsWith("data:"))
        {
          boolean isexception = false;

          for (String item : whitelist){
            if (b.endsWith(item)){
              isexception = true;
              break;
            }
          }

          if(isexception == true)
          {
            return  false;
          }

          String message = context.getString(R.string.blocked_ads) + " " + b;
          callback.showToast(message);

        }
        return true;
      }

      this.host = b;
      if (url.contains(".m3u8") || url.contains(".mp4") || url.contains(".webm") || url.contains(".mkv") || url.contains(".mpd") || url.contains(".ismv") || url.contains(".ism/Manifest")|| url.contains(".ism/manifest")) {
        DirectVideo(new Video(url));
        return true;
      }
      if (url.equals("https://vimeo.com/")) {
        //C1372e.m2550b(this.f1579g, 2131296513);
        Log.d("ChromecastApp", "it is vimeo");
      }


      if (extWebViewClient != null)
      {
        return extWebViewClient.shouldOverrideUrlLoading(view, url);
      }
      else
      {
        return super.shouldOverrideUrlLoading(view, url);
      }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon)
    {
      if (loading)
      {
        stopAbpLoading();
      }

      String host1 = com.oxycast.chromecastapp.utils.Utils.getHost(url);
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

   /*   int useragentid = prefs.getInt("useragent",0);
      if(host1.endsWith("youtube.com"))
      {
         if(useragentid == 0)
         {
            webSettings.setUserAgentString(context.getString(R.string.uachromemobile_alt));
         }
        else {
           switch (useragentid)
           {
             case 0:
               webSettings.setUserAgentString(context.getString(R.string.uachromemobile));
               break;
             case 1:
               webSettings.setUserAgentString(context.getString(R.string.uasafari));
               break;
             case 2:
               webSettings.setUserAgentString(context.getString(R.string.uafirefox));
               break;
           }
         }
      }
      else{
        if(useragentid == 0)
        {
          webSettings.setUserAgentString(context.getString(R.string.uachromemobile));
        }        else {
          switch (useragentid)
          {
            case 0:
              webSettings.setUserAgentString(context.getString(R.string.uachromemobile));
              break;
            case 1:
              webSettings.setUserAgentString(context.getString(R.string.uasafari));
              break;
            case 2:
              webSettings.setUserAgentString(context.getString(R.string.uafirefox));
              break;
          }
        }
      }
      */

      if (!mAdBlock.isAd(url) && !mAdBlock2.isAdsHost(url)) {
        callback.setUrl(url);
        currentHost = url;
      }

      isLoading = true;
      urls.clear();
      videoList.clear();
      mListener.onPageStarted();
      startAbpLoading(url);

      if (extWebViewClient != null)
      {
        extWebViewClient.onPageStarted(view, url, favicon);
      }
      else
      {
        super.onPageStarted(view, url, favicon);
      }
    }



    @Override
    public void onPageFinished(WebView view, String url)
    {
      loading = false;
      isLoading = false;
      mListener.onPageFinished();
      callback.setHistory(url);
      callback.setUrl(url);
      currentHost = url;
      if (extWebViewClient != null)
      {
        extWebViewClient.onPageFinished(view, url);
      }
      else
      {
        super.onPageFinished(view, url);
      }
    }

    @Override
    public void onLoadResource(WebView view, String url)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.onLoadResource(view, url);
      }
      else
      {
        super.onLoadResource(view, url);
      }
    }

    @Override
    public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.onTooManyRedirects(view, cancelMsg, continueMsg);
      }
      else
      {
        super.onTooManyRedirects(view, cancelMsg, continueMsg);
      }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
    {
      e("Load error:" +
              " code=" + errorCode +
              " with description=" + description +
              " for url=" + failingUrl);
      loadError = errorCode;

      stopAbpLoading();

      if (extWebViewClient != null)
      {
        extWebViewClient.onReceivedError(view, errorCode, description, failingUrl);
      }
      else
      {
        super.onReceivedError(view, errorCode, description, failingUrl);
      }
    }

    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.onFormResubmission(view, dontResend, resend);
      }
      else
      {
        super.onFormResubmission(view, dontResend, resend);
      }
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.doUpdateVisitedHistory(view, url, isReload);
      }
      else
      {
        super.doUpdateVisitedHistory(view, url, isReload);
      }
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.onReceivedSslError(view, handler, error);
      }
      else
      {
        super.onReceivedSslError(view, handler, error);
      }
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
      }
      else
      {
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
      }
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event)
    {
      if (extWebViewClient != null)
      {
        return extWebViewClient.shouldOverrideKeyEvent(view, event);
      }
      else
      {
        return super.shouldOverrideKeyEvent(view, event);
      }
    }

    @Override
    public void onUnhandledKeyEvent(WebView view, KeyEvent event)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.onUnhandledKeyEvent(view, event);
      }
      else
      {
        super.onUnhandledKeyEvent(view, event);
      }
    }

    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.onScaleChanged(view, oldScale, newScale);
      }
      else
      {
        super.onScaleChanged(view, oldScale, newScale);
      }
    }

    @Override
    public void onReceivedLoginRequest(WebView view, String realm, String account, String args)
    {
      if (extWebViewClient != null)
      {
        extWebViewClient.onReceivedLoginRequest(view, realm, account, args);
      }
      else
      {
        super.onReceivedLoginRequest(view, realm, account, args);
      }
    }

    protected WebResourceResponse shouldInterceptRequest(
            WebView webview, String url, boolean isMainFrame,
            boolean isXmlHttpRequest, String[] referrerChainArray) {
      // if dispose() was invoke, but the page is still loading then just let it go
      if (adblockEngine == null) {
        e("FilterEngine already disposed, allow loading");

        // allow loading by returning null
        return null;
      }

      if (isMainFrame) {
        // never blocking main frame requests, just subrequests
        w(url + " is main frame, allow loading");

        // allow loading by returning null
        return null;
      }

      // whitelisted
      if (adblockEngine.isDomainWhitelisted(url, referrerChainArray)) {
        w(url + " domain is whitelisted, allow loading");

        // allow loading by returning null
        return null;
      }

      if (adblockEngine.isDocumentWhitelisted(url, referrerChainArray)) {
        w(url + " document is whitelisted, allow loading");

        // allow loading by returning null
        return null;
      }

      String host = com.oxycast.chromecastapp.utils.Utils.getHost(url);

      if (host.contains("youtube.com") == true)
      {
        w(url + " document is whitelisted, allow loading");

        // allow loading by returning null
        return null;
      }

      // determine the content
      FilterEngine.ContentType contentType;
      if (isXmlHttpRequest)
      {
        contentType = FilterEngine.ContentType.XMLHTTPREQUEST;
      }
      else
      {
        if (RE_JS.matcher(url).find())
        {
          contentType = FilterEngine.ContentType.SCRIPT;
        }
        else if (RE_CSS.matcher(url).find())
        {
          contentType = FilterEngine.ContentType.STYLESHEET;
        }
        else if (RE_IMAGE.matcher(url).find())
        {
          contentType = FilterEngine.ContentType.IMAGE;
        }
        else if (RE_FONT.matcher(url).find())
        {
          contentType = FilterEngine.ContentType.FONT;
        }
        else if (RE_HTML.matcher(url).find())
        {
          contentType = FilterEngine.ContentType.SUBDOCUMENT;
        }
        else
        {
          contentType = FilterEngine.ContentType.OTHER;
        }
      }

      // check if we should block
      if (adblockEngine.matches(url, contentType, referrerChainArray))
      {
        w("Blocked loading " + url);

        // if we should block, return empty response which results in 'errorLoading' callback
        return new WebResourceResponse("text/plain", "UTF-8", null);
      }

      //d("Allowed loading " + url);

      // continue by returning null
      return null;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView webView, String url) {
      String host1 = com.oxycast.chromecastapp.utils.Utils.getHost(url);
      boolean isexception = false;

      for (String item : whitelist){
        if (host1.endsWith(item)){
          isexception = true;
          break;
        }
      }

      if(url.contains("m.youtube.com") && url.contains("watch?ajax="))
      {
        //urls.clear();
        videoList.clear();
      }

      //if(host1.endsWith("doubleclick.net") || host1.endsWith("googlesyndication.com") || (host1.endsWith("google.com") && url.contains("ads")==true))
      if(isexception == true)
      {
        return super.shouldInterceptRequest(webView,url);

      }


      if (mAdBlock.isAd(url) || mAdBlock2.isAdsHost(url)) {
        Log.d("ChromecastApp","It is ad: " + url);
        ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
        return new WebResourceResponse("text/plain", "UTF-8", EMPTY);
      }

      WebResourceResponse shouldInterceptRequest = super.shouldInterceptRequest(webView, url);

      if (("youtube.com".equals(this.host) || "m.youtube.com".equals(this.host)) && url.contains("docid=")) {
        String substring = url.substring(url.indexOf("docid=") + 6);
        this.param = substring.substring(0, substring.indexOf("&"));
        Log.d("Youtube", "This is youtube "+url);
        YoutubeExtractLink();
        return shouldInterceptRequest;
      } else if (url.contains("youtube.com") && url.contains("video_id")) {
        int indexOf = url.indexOf("video_id") + 9;
        this.param = url.substring(indexOf, indexOf + 11);
        Log.d("Youtube", "This is youtube "+url);
        YoutubeExtractLink();
        return shouldInterceptRequest;
      } else if (((url.contains("videoplayback") || url.contains("lh3.googleusercontent.com")) && this.param == null) && currentHost.contains("youtube.com") == false) {
        Log.d("Youtube", "This is direct "+url);
        DirectVideo(new Video(url));
        return shouldInterceptRequest;
      } else if (url.contains("player.vimeo.com/video")) {
        m2102b(url);
        return shouldInterceptRequest;
      } else if (url.contains("dailymotion.com/player/metadata/video/")) {
        m2103c(url);
        return shouldInterceptRequest;
      } else if (url.contains("http://www.xvideos.com/video")) {
        m2104d(url);
        return shouldInterceptRequest;
      } else if (this.param != null || "dailymotion.com".equals(this.host) || "vimeo.com".equals(this.host) || "youtube.com".equals(this.host) || "m.youtube.com".equals(this.host) || url.contains(".html") || url.contains(".json") || url.contains(".php") || url.contains(".png") || url.contains(".gif") || url.contains(".jpg") || url.contains(".css") || url.contains(".ico") || url.contains(".js") || mAdBlock2.isInBlackList(url)) {
        return shouldInterceptRequest;
      } else {
        Video video = new Video(url);
        //   this.videoList.add(video);
        if ((url.contains(".m3u8") || (url.contains(".mp4")) || (url.contains(".webm")) || (url.contains(".mkv")) || (url.contains(".mpd")) || (url.contains(".ismv") )) && mAdBlock2.isInWhiteList(this.host)) {
          Log.d("Youtube", "This is direct "+url);
          DirectVideo(new Video(url));
          return shouldInterceptRequest;
        } else if (this.host == null || mAdBlock2.isInWhiteList(this.host)) {
          return shouldInterceptRequest;
        } else {
          m2098a(url);


          return shouldInterceptRequest;
        }
      }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request)
    {
      String url = request.getUrl().toString();
      String host1 = com.oxycast.chromecastapp.utils.Utils.getHost(url);
      boolean isexception = false;

      for (String item : whitelist){
        if (host1.endsWith(item)){
          Log.d("Adblock", "Exception: " + host1);
          isexception = true;
          break;
        }
      }

      if(url.contains("m.youtube.com") && url.contains("watch?ajax="))
      {
        //  urls.clear();
        videoList.clear();
      }
      //if(host1.endsWith("doubleclick.net") || host1.endsWith("googlesyndication.com") || (host1.endsWith("google.com") && url.contains("ads")==true))
      if(isexception == true)
      {
        return super.shouldInterceptRequest(view,request);

      }

      if (mAdBlock.isAd(url) || mAdBlock2.isAdsHost(url)) {
        Log.d("ChromecastApp","It is ad: " + url);
        ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
        return new WebResourceResponse("text/plain", "UTF-8", EMPTY);
      }
      boolean isXmlHttpRequest =
              request.getRequestHeaders().containsKey(HEADER_REQUESTED_WITH) &&
                      HEADER_REQUESTED_WITH_XMLHTTPREQUEST.equals(
                              request.getRequestHeaders().get(HEADER_REQUESTED_WITH));

      Log.d("ChromecastAppRequest", request.getMethod() + " isXmlHttpRequest: " + isXmlHttpRequest + " URL: " +  url );
      String referrer = request.getRequestHeaders().get(HEADER_REFERRER);
      String[] referrers;

      if (referrer != null)
      {
        //  d("Header referrer for " + url + " is " + referrer);
        url2Referrer.put(url, referrer);

        referrers = new String[]
                {
                        referrer
                };
      }
      else
      {
        w("No referrer header for " + url);
        referrers = EMPTY_ARRAY;
      }

      if (("youtube.com".equals(this.host) || "m.youtube.com".equals(this.host)) && url.contains("docid=")) {
        String substring = url.substring(url.indexOf("docid=") + 6);
        this.param = substring.substring(0, substring.indexOf("&"));
        Log.d("Youtube", "This is youtube "+url);
        YoutubeExtractLink();
        return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
      } else if (url.contains("youtube.com") && url.contains("video_id")) {
        int indexOf = url.indexOf("video_id") + 9;
        this.param = url.substring(indexOf, indexOf + 11);
        Log.d("Youtube", "This is youtube "+url);
        YoutubeExtractLink();
        return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
      } else if (((url.contains("videoplayback") || url.contains("lh3.googleusercontent.com")) && this.param == null) && currentHost.contains("youtube.com") == false) {
        Log.d("Youtube", "This is direct "+url);
        DirectVideo(new Video(url));
        return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
      } else if (url.contains("player.vimeo.com/video")) {
        m2102b(url);
        return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
      } else if (url.contains("dailymotion.com/player/metadata/video/")) {
        m2103c(url);
        return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
      } else if (url.contains("http://www.xvideos.com/video")) {
        m2104d(url);
        return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
      } else if (this.param != null || "dailymotion.com".equals(this.host) || "vimeo.com".equals(this.host) || "youtube.com".equals(this.host) || "m.youtube.com".equals(this.host) || url.contains(".html") || url.contains(".json") || url.contains(".php") || url.contains(".png") || url.contains(".gif") || url.contains(".jpg") || url.contains(".css") || url.contains(".ico") || url.contains(".js") || mAdBlock2.isInBlackList(url)) {
        return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
      } else {
        Video video = new Video(url);
        //   this.videoList.add(video);
        if (url.contains(".m3u8") || (url.contains(".mp4") && mAdBlock2.isInWhiteList(this.host))) {
          Log.d("Youtube", "This is direct "+url);
          DirectVideo(new Video(url));
          return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
        } else if (this.host == null || mAdBlock2.isInWhiteList(this.host)) {
          return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
        } else {
          m2098a(url);


          return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
        }
      }

      //return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
    }
  }
  WebSettings webSettings;
  private void Init()
  {
    whitelist = Arrays.asList(getResources().getStringArray(R.array.google_ads_service));
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);

    int useragentid = prefs.getInt("useragent",0);
    String useragent = "";

    switch (useragentid)
    {
      case 0:
        useragent = this.context.getString(R.string.uachromemobile);
        break;
      case 1:
        useragent = this.context.getString(R.string.uasafari);
        break;
      case 2:
        useragent = this.context.getString(R.string.uafirefox);
        break;
    }

    // this.webViewExHandler = new WebViewEx.WebViewExHandler(this);
    //   this.scrollingHelper = new NestedScrollingChildHelper(this);
    this.webSettings = getSettings();
    this.webSettings.setUserAgentString(useragent);
    this.webSettings.setJavaScriptEnabled(true);

    this.webSettings.setPluginState(WebSettings.PluginState.ON);
    this.webSettings.setAllowFileAccess(true);
    this.webSettings.setDomStorageEnabled(true);
    this.webSettings.setDatabaseEnabled(false);
    this.webSettings.setAppCacheEnabled(false);
    this.webSettings.setBuiltInZoomControls(true);
    this.webSettings.setLoadWithOverviewMode(true);
    this.webSettings.setUseWideViewPort(true);
    this.webSettings.setDisplayZoomControls(false);
    this.webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
    setDrawingCacheEnabled(true);
    // setOnTouchListener(new C12231(this));
  }

  private IProgressListener mListener;
  public void setProgressListener(IProgressListener listener)
  {

    mListener = listener;
    if(extWebViewClient!=null) {
      ((WebViewClientEx) extWebViewClient).setProgressListener(mListener);

    }
    if(extWebChromeClient!=null)
    {
      ((WebChromeClientEx)extWebChromeClient).setProgressistener(mListener);
    }

  }

  private void initAbp()
  {

    Init();

    addJavascriptInterface(this, BRIDGE);
    initClients();
  }

  private void initClients()
  {
    intWebViewClient = new AdblockWebViewClient();
    extWebViewClient = new WebViewClientEx(context);

    extWebChromeClient = new WebChromeClientEx(context,mListener);
    applyAdblockEnabled();
  }

  private void createAdblockEngine()
  {
    w("Creating AdblockEngine");

    // assuming `this.debugMode` can be used as `developmentBuild` value
    adblockEngine = AdblockEngine.create(
            AdblockEngine.generateAppInfo(this.getContext(), debugMode),
            this.getContext().getCacheDir().getAbsolutePath(),
            true);

  }

  private class ElemHideThread extends Thread
  {
    private String selectorsString;
    private CountDownLatch finishedLatch;
    private AtomicBoolean isCancelled;

    public ElemHideThread(CountDownLatch finishedLatch)
    {
      this.finishedLatch = finishedLatch;
      isCancelled = new AtomicBoolean(false);
    }

    @Override
    public void run()
    {
      try
      {
        if (adblockEngine == null)
        {
          w("FilterEngine already disposed");
          selectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
        }
        else
        {
          String[] referrers = new String[]
                  {
                          url
                  };

          List<Subscription> subscriptions = adblockEngine.getFilterEngine().getListedSubscriptions();
          //  d("Listed subscriptions: " + subscriptions.size());
          if (debugMode)
          {
            for (Subscription eachSubscription : subscriptions)
            {
              // d("Subscribed to " + eachSubscription);
            }
          }

          // d("Requesting elemhide selectors from AdblockEngine for " + url + " in " + this);
          List<String> selectors = adblockEngine.getElementHidingSelectors(url, domain, referrers);
          // d("Finished requesting elemhide selectors, got " + selectors.size() + " in " + this);
          selectorsString = Utils.stringListToJsonArray(selectors);
        }
      }
      finally
      {
        if (!isCancelled.get())
        {
          finish(selectorsString);
        }
        else
        {
          w("This thread is cancelled, exiting silently " + this);
        }
      }
    }

    private void onFinished()
    {
      finishedLatch.countDown();
      synchronized (finishedRunnableLockObject)
      {
        if (finishedRunnable != null)
        {
          finishedRunnable.run();
        }
      }
    }

    private void finish(String result)
    {
      // d("Setting elemhide string " + result.length() + " bytes");
      elemHideSelectorsString = result;
      onFinished();
    }

    private final Object finishedRunnableLockObject = new Object();
    private Runnable finishedRunnable;

    public void setFinishedRunnable(Runnable runnable)
    {
      synchronized (finishedRunnableLockObject)
      {
        this.finishedRunnable = runnable;
      }
    }

    public void cancel()
    {
      w("Cancelling elemhide thread " + this);
      isCancelled.set(true);

      finish(EMPTY_ELEMHIDE_ARRAY_STRING);
    }
  }

  private Runnable elemHideThreadFinishedRunnable = new Runnable()
  {
    @Override
    public void run()
    {
      synchronized (elemHideThreadLockObject)
      {
        w("elemHideThread set to null");
        elemHideThread = null;
      }
    }
  };

  private void initAbpLoading()
  {
    getSettings().setJavaScriptEnabled(true);
    buildInjectJs();

    if (adblockEngine == null)
    {
      createAdblockEngine();
      disposeEngine = true;
    }
  }

  private void startAbpLoading(String newUrl)
  {
    d("Start loading " + newUrl);

    loading = true;
    addDomListener = true;
    elementsHidden = false;
    loadError = null;
    url = newUrl;

    if (url != null && adblockEngine != null)
    {
      try
      {
        domain = adblockEngine.getFilterEngine().getHostFromURL(url);
        if (domain == null)
        {
          throw new RuntimeException("Failed to extract domain from " + url);
        }

        d("Extracted domain " + domain + " from " + url);
      }
      catch (Throwable t)
      {
        e("Failed to extract domain from " + url, t);
      }

      elemHideLatch = new CountDownLatch(1);
      elemHideThread = new ElemHideThread(elemHideLatch);
      elemHideThread.setFinishedRunnable(elemHideThreadFinishedRunnable);
      elemHideThread.start();
    }
    else
    {
      elemHideLatch = null;
    }
  }

  private void buildInjectJs()
  {
    try
    {
      if (injectJs == null)
      {
        injectJs = readScriptFile("inject.js").replace(HIDE_TOKEN, readScriptFile("css.js"));
      }
    }
    catch (IOException e)
    {
      e("Failed to read script", e);
    }
  }

  @Override
  public void goBack()
  {
    if (loading)
    {
      stopAbpLoading();
    }

    super.goBack();
  }

  @Override
  public void goForward()
  {
    if (loading)
    {
      stopAbpLoading();
    }

    super.goForward();
  }

  @Override
  public void reload()
  {
    initAbpLoading();

    if (loading)
    {
      stopAbpLoading();
    }

    super.reload();
  }

  @Override
  public void loadUrl(String url)
  {
    initAbpLoading();

    if (loading)
    {
      stopAbpLoading();
    }

    super.loadUrl(url);
  }

  @Override
  public void loadUrl(String url, Map<String, String> additionalHttpHeaders)
  {
    initAbpLoading();

    if (loading)
    {
      stopAbpLoading();
    }

    super.loadUrl(url, additionalHttpHeaders);
  }

  @Override
  public void loadData(String data, String mimeType, String encoding)
  {
    initAbpLoading();

    if (loading)
    {
      stopAbpLoading();
    }

    super.loadData(data, mimeType, encoding);
  }

  @Override
  public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding,
                                  String historyUrl)
  {
    initAbpLoading();

    if (loading)
    {
      stopAbpLoading();
    }

    super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
  }

  @Override
  public void stopLoading()
  {
    stopAbpLoading();
    super.stopLoading();
  }

  private void stopAbpLoading()
  {
    d("Stop abp loading");

    loading = false;
    stopPreventDrawing();
    clearReferrers();

    synchronized (elemHideThreadLockObject)
    {
      if (elemHideThread != null)
      {
        elemHideThread.cancel();
      }
    }
  }

  // warning: do not rename (used in injected JS by method name)
  @JavascriptInterface
  public void setElementsHidden(boolean value)
  {
    // invoked with 'true' by JS callback when DOM is loaded
    elementsHidden = value;

    // fired on worker thread, but needs to be invoked on main thread
    if (value)
    {
//     handler.post(allowDrawRunnable);
//     should work, but it's not working:
//     the user can see element visible even though it was hidden on dom event

      if (allowDrawDelay > 0)
      {
        d("Scheduled 'allow drawing' invocation in " + allowDrawDelay + " ms");
      }
      handler.postDelayed(allowDrawRunnable, allowDrawDelay);
    }
  }

  // warning: do not rename (used in injected JS by method name)
  @JavascriptInterface
  public boolean isElementsHidden()
  {
    return elementsHidden;
  }

  @Override
  public void onPause()
  {
    handler.removeCallbacks(allowDrawRunnable);
    super.onPause();
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    if (allowDraw)
    {
      super.onDraw(canvas);
    }
    else
    {
      w("Prevent drawing");
      drawEmptyPage(canvas);
    }
  }

  private void drawEmptyPage(Canvas canvas)
  {
    // assuming default color is WHITE
    canvas.drawColor(Color.WHITE);
  }

  protected void startPreventDrawing()
  {
    w("Start prevent drawing");

    allowDraw = false;
  }

  protected void stopPreventDrawing()
  {
    d("Stop prevent drawing, invalidating");

    allowDraw = true;
    invalidate();
  }

  private Runnable allowDrawRunnable = new Runnable()
  {
    @Override
    public void run()
    {
      stopPreventDrawing();
    }
  };

  // warning: do not rename (used in injected JS by method name)
  @JavascriptInterface
  public String getElemhideSelectors()
  {
    if (elemHideLatch == null)
    {
      return EMPTY_ELEMHIDE_ARRAY_STRING;
    }
    else
    {
      try
      {
        // elemhide selectors list getting is started in startAbpLoad() in background thread
        d("Waiting for elemhide selectors to be ready");
        elemHideLatch.await();
        d("Elemhide selectors ready, " + elemHideSelectorsString.length() + " bytes");

        clearReferrers();

        return elemHideSelectorsString;
      }
      catch (InterruptedException e)
      {
        w("Interrupted, returning empty selectors list");
        return EMPTY_ELEMHIDE_ARRAY_STRING;
      }
    }
  }

  private void doDispose()
  {
    w("Disposing AdblockEngine");
    adblockEngine.dispose();
    adblockEngine = null;

    disposeEngine = false;
  }

  private class DisposeRunnable implements Runnable
  {
    private Runnable disposeFinished;

    private DisposeRunnable(Runnable disposeFinished)
    {
      this.disposeFinished = disposeFinished;
    }

    @Override
    public void run()
    {
      if (disposeEngine)
      {
        doDispose();
      }

      if (disposeFinished != null)
      {
        disposeFinished.run();
      }
    }
  }

  /**
   * Dispose AdblockWebView and internal adblockEngine if it was created
   * If external AdblockEngine was passed using `setAdblockEngine()` it should be disposed explicitly
   * Warning: runnable can be invoked from background thread
   * @param disposeFinished runnable to run when AdblockWebView is disposed
   */
  public void dispose(final Runnable disposeFinished)
  {
    d("Dispose invoked");

    stopLoading();

    removeJavascriptInterface(BRIDGE);
    if (!disposeEngine)
    {
      adblockEngine = null;
    }

    DisposeRunnable disposeRunnable = new DisposeRunnable(disposeFinished);
    synchronized (elemHideThreadLockObject)
    {
      if (elemHideThread != null)
      {
        w("Busy with elemhide selectors, delayed disposing scheduled");
        elemHideThread.setFinishedRunnable(disposeRunnable);
      }
      else
      {
        disposeRunnable.run();
      }
    }
  }

  /////// Поиск ссылок /////////


}
