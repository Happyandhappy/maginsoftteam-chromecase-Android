package com.oxycast.chromecastapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.oxycast.chromecastapp.media.Video;
import com.squareup.picasso.Downloader;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.mozilla.universalchardet.Constants;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Created by sergey on 25.02.17.
 */

public class Utils {
    public static String getHost(String str) {
        try {
            String toLowerCase = str.toLowerCase();
            int indexOf = toLowerCase.indexOf(47, 8);
            if (indexOf != -1) {
                toLowerCase = toLowerCase.substring(0, indexOf);
            }
            String host = new URI(toLowerCase).getHost();
            return host == null ? toLowerCase : host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Quietly closes a closeable object like an InputStream or OutputStream without
     * throwing any errors or requiring you do do any checks.
     *
     * @param closeable the object to close
     */
    public static void close(Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getWebPage(String str) {
        HttpURLConnection httpURLConnection = null;
        BufferedReader bufferedReader;
        Throwable th;
        Throwable th2;
        BufferedReader bufferedReader2 = null;
        try {
            HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(str).openConnection();
            try {
                httpURLConnection2.setRequestMethod("GET");
                httpURLConnection2.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0");
                httpURLConnection2.connect();
                InputStream inputStream = httpURLConnection2.getInputStream();
                StringBuilder stringBuilder = new StringBuilder();
                BufferedReader bufferedReader3 = new BufferedReader(new InputStreamReader(inputStream));
                while (true) {
                    try {
                        String readLine = bufferedReader3.readLine();
                        if (readLine == null) {
                            break;
                        }
                        stringBuilder.append(readLine).append("\n");
                    } catch (Exception e) {
                        BufferedReader bufferedReader4 = bufferedReader3;
                        httpURLConnection = httpURLConnection2;
                        bufferedReader = bufferedReader4;
                    } catch (Throwable th3) {
                        th = th3;
                        bufferedReader2 = bufferedReader3;
                        httpURLConnection = httpURLConnection2;
                        th2 = th;
                    }
                }
                String stringBuilder2 = stringBuilder.toString();
                if (httpURLConnection2 != null) {
                    httpURLConnection2.disconnect();
                }
                if (bufferedReader3 == null) {
                    return stringBuilder2;
                }
                try {
                    bufferedReader3.close();
                    return stringBuilder2;
                } catch (IOException e2) {
                    // FirebaseCrash.report(new Exception("Error closing stream"));
                    return stringBuilder2;
                }
            } catch (Exception e3) {
                httpURLConnection = httpURLConnection2;
                bufferedReader = bufferedReader2;
                try {
                    // FirebaseCrash.report(new Exception("Error get page response"));
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    if (bufferedReader != null) {
                        return bufferedReader2.toString();
                    }
                    try {
                        bufferedReader.close();
                        return bufferedReader2.toString();
                    } catch (IOException e4) {
                        //FirebaseCrash.report(new Exception("Error closing stream"));
                        return bufferedReader2.toString();
                    }
                } catch (Throwable th32) {
                    th = th32;
                    bufferedReader2 = bufferedReader;
                    th2 = th;
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    if (bufferedReader2 != null) {
                        try {
                            bufferedReader2.close();
                        } catch (IOException e5) {
                            //    FirebaseCrash.report(new Exception("Error closing stream"));
                        }
                    }
                    throw th2;
                }
            } catch (Throwable th4) {
                th = th4;
                httpURLConnection = httpURLConnection2;
                th2 = th;
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (bufferedReader2 != null) {
                    bufferedReader2.close();
                }
                throw th2;
            }
        } catch (Exception e6) {
            bufferedReader = bufferedReader2;
            Object obj = bufferedReader2;
            //FirebaseCrash.report(new Exception("Error get page response"));
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (bufferedReader != null) {
                return bufferedReader2.toString();
            }
            //bufferedReader.close();
            return bufferedReader2.toString();
        } catch (Throwable th5) {
            /*th2 = th5;
            httpURLConnection = bufferedReader2;
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (bufferedReader2 != null) {
                bufferedReader2.close();
            }
            throw th2;*/
            return bufferedReader2.toString();
        }
    }

    public static String ProcessString(String str, String str2, String str3) {
        if (str == null || !str.contains(str2) || !str.contains(str3)) {
            return null;
        }
        int indexOf = str.indexOf(str2);
        return str.substring(indexOf + str2.length(), str.indexOf(str3, indexOf));
    }

    public static boolean isAdsBlocker(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean isadblock  = prefs.getBoolean("isadblock",false);

        return isadblock;
    }

    public static Video prepareVideo(String video, String webServer)
    {
        File videoFile = new File(video);
        String url = webServer +preparedVideoPath(videoFile); //videoFile.getAbsolutePath().replace(" ", "%20");
        Video cVideo = new Video(videoFile.getName(),url);
        String fileNameWithOutExt = FilenameUtils.removeExtension(videoFile.getName());
        String workingDir = videoFile.getParent();
        File subtitlevtt = new File(workingDir, fileNameWithOutExt + ".vtt");
        if(subtitlevtt.exists() == true) {
            cVideo.setSubtitle(webServer + Utils.preparedVideoPath(subtitlevtt));
        }

        File subtitlettml = new File(workingDir, fileNameWithOutExt + ".ttml");
        if(subtitlettml.exists() == true) {
            cVideo.setSubtitle( webServer + Utils.preparedVideoPath(subtitlettml));
        }

        subtitlettml = new File(workingDir, fileNameWithOutExt + ".dfxp");
        if(subtitlettml.exists() == true) {
            cVideo.setSubtitle( webServer + Utils.preparedVideoPath(subtitlettml));
        }

        subtitlettml = new File(workingDir, fileNameWithOutExt + ".xml");
        if(subtitlettml.exists() == true) {
            cVideo.setSubtitle( webServer + Utils.preparedVideoPath(subtitlettml));
        }
        String mime = getMimeType("file://" + video);
        cVideo.setMimeType(mime);
return  cVideo;
    }

    public static String getMimeTypeFromNetwork(String url)
    {
        String contentType = "";
        try {
            URL connurl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)  connurl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            contentType = connection.getContentType();
        }
        catch (Exception e)
        {

        }

        if(TextUtils.isEmpty(contentType))
        {
            contentType = getMimeType(url);
        }

        return contentType;
    }

    public static String getMimeType(String url) {
         if(url.toLowerCase().endsWith("manifest"))
        {
            return "application/vnd.ms-sstr+xml";
        }
        else {
            String type = null;
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if (!TextUtils.isEmpty(extension)) {
                if (extension.equalsIgnoreCase("m3u8")) {
                    type = "application/x-mpegURL";
                } else if (extension.equalsIgnoreCase("mpd")) {
                    type = "application/dash+xml";
                } else
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            } else {
                //logger.info("Extension is null. " + url);
                int index = url.lastIndexOf(".");
                extension = url.substring(index + 1);
                if (!TextUtils.isEmpty(extension)) {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                } else {
                    // logger.info("Unexpected error to receive mime. Use default video/mp4. " + url);
                    type = "video/mp4";
                }
            }

            Log.d("MIMETYPE", "Type: " + type);
            return type;
        }
    }

    public static String getEncoding(File file)
    {
        try {
            String encoding = UniversalDetector.detectCharset(file);
            if (encoding != null) {
                return encoding;
            } else {
                return Constants.CHARSET_WINDOWS_1252;
            }
        }catch (Exception ex)
        {
return Constants.CHARSET_WINDOWS_1252;
        }
    }

    public static String getRedirect(String url)
    {
        try {
            Connection.Response response = Jsoup.connect(url).followRedirects(false).execute();

            Log.d("REDIRECT", "Status code: " + response.statusCode());

            if(response.statusCode()>=300 && response.statusCode()<400 && response.hasHeader("location")==true)
            {
                return response.header("location");
            }
        }
        catch (Exception e)
        {

        }

        return url;
    }

    public static String preparedVideoPath(File file)
    {
        String [] paths = file.getAbsolutePath().split("/");
        String result = "";
        for (String path:paths)
        {
            if(TextUtils.isEmpty(path)==false)
            {
            result +="/";
            try {
                result += java.net.URLEncoder.encode(path, "UTF-8").replace("+", "%20");
            }
            catch (UnsupportedEncodingException ex){}}
        }

        return result;
    }
}

