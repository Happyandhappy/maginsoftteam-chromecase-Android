package com.oxycast.chromecastapp.web;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.oxycast.chromecastapp.media.DailyMotionUriExtractor;
import com.oxycast.chromecastapp.media.IWebBrowserResult;
import com.oxycast.chromecastapp.media.Video;
import com.oxycast.chromecastapp.media.VimeoUriExtractor;
import com.oxycast.chromecastapp.media.XVideoUriExtractor;
import com.oxycast.chromecastapp.utils.Utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Created by sergey on 25.02.17.
 */

public class WebViewClientEx extends WebViewClient {

    private String currentHost = "";
    private String host; //f1580h
    private String param;//f1577e;
    Context context;
    ArrayList<Video> videoList;
    //AdsBlockHelper adsBlockHelper;
    private IWebBrowserResult callback;
    private int videoCount;
    HashSet<String> urls;
    boolean isLoading = false;
    //   private AdBlock mAdBlock;
    //  AdsBlockHelper mAdBlock2;
    public WebViewClientEx(Context context) {
        this.context = context;
//        mAdBlock = AdBlock.getInstance(context.getApplicationContext());
//        mAdBlock2 = new AdsBlockHelper(context);
//        this.adsBlockHelper = new AdsBlockHelper(context);
        videoList = new ArrayList<Video>();
        urls = new HashSet<String>();
        this.callback = (IWebBrowserResult) context;

    }

    private IProgressListener mListener;
    public void setProgressListener(IProgressListener listener)
    {
        mListener = listener;
    }


    private void YoutubeExtractLink() {
        final WebViewClientEx client = this;
        {
            //  this.f1575c.add(this.param);

            String youtubeLink = "https://m.youtube.com/watch?v=" + this.param;

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
                                    urls.add(videoId);
                                    downloadUrl = file.getUrl();

                                    Video video = new Video(videoTitle, downloadUrl);
                                    String mime = com.oxycast.chromecastapp.utils.Utils.getMimeType(downloadUrl);
                                    if(TextUtils.isEmpty(mime))
                                    {
                                        mime = "video/mp4";
                                    }

                                    video.setMimeType(mime);

                                    client.FindVideo(video);
                                    //new Thread(new CheckMimeType(client, video)).start();
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
        new Thread(new CheckMimeType(this, video)).start();
        Log.d("ChromecastApp", "m2097a: " + video.getUrl());
    }

    class CheckMimeType implements Runnable {
        final Video video;
        final WebViewClientEx webViewClient;

        CheckMimeType(WebViewClientEx webViewClient, Video video) {
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
                    String url = httpURLConnection2.getURL().toString();
                    String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType);
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
                this.callback.FindVideo(video);
            } else {
                this.callback.FindVideos(videoList);
            }

            this.videoCount++;
        }
    }

    class VimeoUriExtractorEx extends VimeoUriExtractor {
        final WebViewClientEx webViewClient;

        VimeoUriExtractorEx(WebViewClientEx webViewClient) {
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
                    this.webViewClient.callback.FindVideo(video);
                } else {
                    this.webViewClient.callback.FindVideos(videoList);
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
        final WebViewClientEx webViewClient;

        DailyMotionUriExtractorEx(WebViewClientEx webViewClient) {
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
                    this.webViewClient.callback.FindVideo(video);
                } else {
                    this.webViewClient.callback.FindVideos(videoList);
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
        new VimeoUriExtractorEx(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{str});
        Log.d("ChromecastApp","Vimeo processing: " + str);

    }

    private void m2103c(String str) {
        new DailyMotionUriExtractorEx(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{str});

        Log.d("ChromecastApp"," Dailymotion processing: " + str);

    }

    class XVideoUriExtractorEx extends XVideoUriExtractor {
        WebViewClientEx webViewClient;

        XVideoUriExtractorEx(WebViewClientEx webViewClient) {
            this.webViewClient = webViewClient;
        }

        @Override
        public void m2091a(ArrayList<Video> arrayList) {
            int size = arrayList.size();
            videoList.addAll(arrayList);
            if(isLoading == false) {
                if (size == 1) {
                    Video video = (Video) arrayList.get(0);
                    this.webViewClient.callback.FindVideo(video);
                } else if (size > 1) {
                    this.webViewClient.callback.FindVideos(arrayList);
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
        new XVideoUriExtractorEx(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{str});

        Log.d("ChromecastApp"," xvideos processing: " + str);
    }

    private void m2098a(String str) {
        //new Thread(new C12281(this, str)).start();

        Log.d("ChromecastApp", " other processing: " + str);
    }

    /*
        private void m2098a(String str) {
            new Thread(new C12281(this, str)).start();
        }

        private void m2101b(C1332c c1332c) {
            if (c1332c.m2382a()) {
                c1332c.m2381a(this.f1576d == 0);
            }
            if (!c1332c.m2382a()) {
                C1372e.m2536a(2131296504, 0);
            }
            this.f1578f.m1960a(c1332c);
            this.f1576d++;
        }






    */
    @Override
    public void onPageFinished(WebView webView, String str) {
        // this.f1578f.m1962b(str);
        isLoading = false;
        super.onPageFinished(webView, str);
        mListener.onPageFinished();
        callback.setHistory(str);
        callback.setUrl(str);
        currentHost = str;
     /*   if(videoList.size() == 1)
        {
            callback.FindVideo(videoList.get(0));
        }
        else if(videoList.size()>1)
        {
            callback.FindVideos(videoList);
        }*/
        Log.d("ChromecastApp", "onPageFinished: " + str);
    }
    @Override
    public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
        //  if (!mAdBlock.isAd(str) && !this.adsBlockHelper.isAdsHost(str)) {
        callback.setUrl(str);
        currentHost = str;
        // }

        urls.clear();

        isLoading = true;
        this.host = Utils.getHost(str);
        this.param = null;
        //progressBar.setVisibility(View.VISIBLE);
        mListener.onPageStarted();
        //this.f1577e = null;
        this.videoList.clear();
        //this.f1578f.m1961a(str);
        //this.f1576d = 0;
        Log.d("ChromecastApp", "onPageStarted: " + str);
        super.onPageStarted(webView, str, bitmap);
    }

    @TargetApi(21)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
        return shouldInterceptRequest(webView, webResourceRequest.getUrl().toString());
    }
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView webView, String str) {
       /* if (this.f1581i.m2112a(str)) {
            return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(BuildConfig.VERSION_NAME.getBytes()));
        }*/
//        if (mAdBlock.isAd(str) || mAdBlock2.isAdsHost(str)) {
//            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
//            return new WebResourceResponse("text/plain", "UTF-8", EMPTY);
//        }

        Log.d("ChromecastApp", "shouldInterceptRequest " + str);
        if(str.contains("m.youtube.com") && str.contains("watch?ajax="))
        {
            //  urls.clear();
            videoList.clear();
        }
        WebResourceResponse shouldInterceptRequest = super.shouldInterceptRequest(webView, str);

        if (("youtube.com".equals(this.host) || "m.youtube.com".equals(this.host)) && str.contains("docid=")) {
            String substring = str.substring(str.indexOf("docid=") + 6);
            this.param = substring.substring(0, substring.indexOf("&"));
            YoutubeExtractLink();
            return shouldInterceptRequest;
        } else if (str.contains("youtube.com") && str.contains("video_id")) {
            int indexOf = str.indexOf("video_id") + 9;
            this.param = str.substring(indexOf, indexOf + 11);
            YoutubeExtractLink();
            return shouldInterceptRequest;
        } else if (((str.contains("videoplayback") || str.contains("lh3.googleusercontent.com")) && this.param == null)&& currentHost.contains("youtube.com") == false) {
            DirectVideo(new Video(str));
            return shouldInterceptRequest;
        } else if (str.contains("player.vimeo.com/video")) {
            m2102b(str);
            return shouldInterceptRequest;
        } else if (str.contains("dailymotion.com/player/metadata/video/")) {
            m2103c(str);
            return shouldInterceptRequest;
        } else if (str.contains("http://www.xvideos.com/video")) {
            m2104d(str);
            return shouldInterceptRequest;
        } else if (this.param != null || "dailymotion.com".equals(this.host) || "vimeo.com".equals(this.host) || "youtube.com".equals(this.host) || "m.youtube.com".equals(this.host) || str.contains(".html") || str.contains(".json") || str.contains(".php") || str.contains(".png") || str.contains(".gif") || str.contains(".jpg") || str.contains(".css") || str.contains(".ico") || str.contains(".js") ) {
            return shouldInterceptRequest;
        } else {
            Video video = new Video(str);
            //   this.videoList.add(video);
            if (str.contains(".m3u8") || (str.contains(".mp4") )) {
                DirectVideo(new Video(str));
                return shouldInterceptRequest;
            } else if (this.host == null) {
                return shouldInterceptRequest;
            } else {
                m2098a(str);


                return shouldInterceptRequest;
            }
        }
    }

    @TargetApi(21)
    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {

        return shouldOverrideUrlLoading(webView, webResourceRequest.getUrl().toString());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, String str) {
        Log.d("ChromecastApp", "shouldOverrideUrlLoading " + str);

        if(str.contains("m.youtube.com") && str.contains("watch?ajax="))
        {
            //  urls.clear();
            videoList.clear();
        }

        String b = Utils.getHost(str);

        this.host = b;
        if (str.contains(".m3u8") || str.contains(".mp4") || str.contains(".webm") || str.contains(".mkv") || str.contains(".mpd") || str.contains(".ismv") || str.contains(".ism/Manifest")|| str.contains(".ism/manifest")) {
            DirectVideo(new Video(str));
            return true;
        }
        if (str.equals("https://vimeo.com/")) {
            //C1372e.m2550b(this.f1579g, 2131296513);
            Log.d("ChromecastApp", "it is vimeo");
        }
        return super.shouldOverrideUrlLoading(webView, str);
    }
}