package com.oxycast.chromecastapp.web;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.media.IWebBrowserResult;

/**
 * Created by sergey on 25.02.17.
 */

public class WebChromeClientEx extends WebChromeClient {


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






    private Context context;
    private IProgressListener mListener;
    private IWebBrowserResult callback;
    private Bitmap 		mDefaultVideoPoster;
    private View 		mVideoProgressView;



   public WebChromeClientEx(Context context, IProgressListener listener) {
        this.context = context;
       mListener = listener;
       this.callback = (IWebBrowserResult) context;


    }

    public void setProgressistener(IProgressListener listener)
    {
        this.mListener = listener;
    }

    @Nullable
    public Bitmap getDefaultVideoPoster() {
        Log.i("ChromecastApp", "here in on getDefaultVideoPoster");
        if (mDefaultVideoPoster == null) {
            mDefaultVideoPoster = BitmapFactory.decodeResource(context.
                    getResources(), R.drawable.default_video_poster);
        }
        return mDefaultVideoPoster;
    }

    public View getVideoLoadingProgressView() {
        return LayoutInflater.from(this.context).inflate(R.layout.loading_progress, null);
    }

/*    @Override
    public void onHideCustomView() {
       // this.f1546b.m1957e();
        Log.d("ChromecastApp","onHideCustomView");

    }
*/
    @Override
    public void onProgressChanged(WebView webView, int i) {
        super.onProgressChanged(webView, i);

        Log.d("ChromcastApp","Progress: " + i);
        mListener.onUpdateProgress(i);
        //this.f1546b.m1952a(i);
    }

    @Override
    public void onReceivedIcon(WebView webView, Bitmap bitmap) {
        super.onReceivedIcon(webView, bitmap);
       // this.f1546b.m1953a(bitmap);
        this.callback.setBitmap(bitmap);

        Log.d("ChromcastApp","onReceivedIcon: ");
    }

    @Override
    public void onReceivedTitle(WebView webView, String str) {
        super.onReceivedTitle(webView, str);
        //this.f1546b.m1956a(webView, str);

        this.callback.SetTitle(str);
        Log.d("ChromcastApp","onReceivedTitle: " +str);
    }
  /*  @Override
    public void onShowCustomView(View view, int i, CustomViewCallback customViewCallback) {
        super.onShowCustomView(view,i,customViewCallback);
        this.callback.m1954a(view, i, customViewCallback);
        Log.d("ChromcastApp","onShowCustomView: ");

    }
    @Override
    public void onShowCustomView(View view, CustomViewCallback customViewCallback) {
        super.onShowCustomView(view, customViewCallback);

        this.callback.m1955a(view, customViewCallback);
        Log.d("ChromcastApp","onShowCustomView: ");
    }*/

}
