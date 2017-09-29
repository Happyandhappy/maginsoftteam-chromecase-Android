package com.oxycast.chromecastapp.services;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.cast.framework.CastSession;
import com.oxycast.chromecastapp.app.ChromecastApp;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Administrator on 30.05.2017.
 */

public class ProxyHandler extends NanoHTTPD {
    String proxyhost;
    String useragent;
    String redirectProxyHost;
    String host;
    String ip;
    String firstLink;
    public ProxyHandler(int port,String proxy,String host,String useragent,String ip) {
        super(port);
        this.proxyhost = proxy;
        this.useragent = useragent;
        this.host = host;
        this.ip = ip;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String url = session.getUri();


        // Initialize variables
        URL currenturl = null;
        HttpURLConnection connection = null;
        InputStream in = null;
        String hideurl = null;
        if(TextUtils.isEmpty(firstLink) || session.getUri().contentEquals(firstLink)) {
            hideurl = proxyhost + session.getUri();
            firstLink = session.getUri();

        Log.d("Proxy", session.getMethod().name() + " " + hideurl);
        // Open the HTTP connection
        Connection.Response response = null;
        Map<String,String> headers = session.getHeaders();
        try {



            if(headers.containsKey("user-agent"))
            {
                headers.remove("user-agent");
            }
            headers.put("user-agent",useragent);
            response = Jsoup.connect(hideurl).followRedirects(false).ignoreContentType(true).headers(headers).execute();


            /*currenturl = new URL(hideurl);

            connection = (HttpURLConnection) currenturl.openConnection();

            connection.setRequestMethod(session.getMethod().name());
           // connection.setAllowUserInteraction(true);
            Map<String, String> headers = session.getHeaders();

            for (Map.Entry<String, String> head : headers.entrySet()) {
                if (head.getKey().equalsIgnoreCase("user-agent") == true) {
                    Log.d("Proxy", "Set user agent: " + useragent);
                    connection.setRequestProperty(head.getKey(), useragent);
                } else if(head.getKey().equalsIgnoreCase("host") == true)
                {
                    connection.setRequestProperty(head.getKey(), host);
                }else
                    connection.setRequestProperty(head.getKey(), head.getValue());
            }
   */
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String datatype = "";
        int length = 0;
      //  Log.d("Proxy","HttpRequest:"+response.toString());
        try {
            // Read the response.
            in = new ByteArrayInputStream(response.bodyAsBytes());
            datatype = response.contentType();
            length = response.bodyAsBytes().length;
            //in = response.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Boolean redirectResponse = false;
        String redirectUrl = null;


   ///     String datatype = response.contentType(); //NanoHTTPD.MIME_DEFAULT_BINARY

        Response.Status currentStatus = Response.Status.OK;
        try {
         //   Log.d("Proxy", "Response code: " + connection.getResponseCode() + " Content-type:" + datatype);
            if (response.statusCode() >= 200 && response.statusCode()<300) {
                currentStatus = Response.Status.OK;
            }
            else if(response.statusCode() >= 300 && response.statusCode()<400 && response.hasHeader("location"))
            {
                Uri uri = Uri.parse(response.header("location"));
                redirectProxyHost = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
                redirectUrl = response.header("location");
                redirectResponse = true;
            }
            else if(response.statusCode() == 401){
                currentStatus = Response.Status.UNAUTHORIZED;
            }
            else if(response.statusCode() == 403)
            {
                currentStatus = Response.Status.FORBIDDEN;
            }

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        if(redirectResponse)
        {
            try {
                response = Jsoup.connect(redirectUrl).followRedirects(false).ignoreContentType(true).headers(headers).execute();
                in = new ByteArrayInputStream(response.bodyAsBytes());
                currentStatus = Response.Status.OK;
                datatype = response.contentType();
                length = response.bodyAsBytes().length;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }


        Response resp = NanoHTTPD.newFixedLengthResponse(currentStatus,datatype,in,length);//NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", hello);
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Headers", "origin,accept,content-type");
        resp.addHeader("Access-Control-Allow-Credentials", "true");
        resp.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        resp.addHeader("Access-Control-Max-Age", "" + MAX_AGE);

     /*   if(currentStatus == Response.Status.FOUND && response.hasHeader("location"))
        {
            Uri uri = Uri.parse(response.header("location"));
            proxyhost = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
            String newUrl = "http://" + ip + ":9500" + uri.getPath();
            resp.addHeader("location",newUrl);
        }
*/
        return resp;
        }
        else{
            try{
            hideurl = redirectProxyHost + session.getUri();
            currenturl = new URL(hideurl);

            connection = (HttpURLConnection) currenturl.openConnection();

            connection.setRequestMethod(session.getMethod().name());
           // connection.setAllowUserInteraction(true);
            Map<String, String> headers = session.getHeaders();

            for (Map.Entry<String, String> head : headers.entrySet()) {
                if (head.getKey().equalsIgnoreCase("user-agent") == true) {
                    Log.d("Proxy", "Set user agent: " + useragent);
                    connection.setRequestProperty(head.getKey(), useragent);
                } else if(head.getKey().equalsIgnoreCase("host") == true)
                {
                    connection.setRequestProperty(head.getKey(), host);
                }else
                    connection.setRequestProperty(head.getKey(), head.getValue());
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String datatype = "";
            int length = 0;
            try {
                // Read the response.
                in = connection.getInputStream();
                datatype = connection.getContentType();
                length = connection.getContentLength();
                //in = response.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Response.Status currentStatus = Response.Status.OK;
            Response resp = NanoHTTPD.newFixedLengthResponse(currentStatus,datatype,in,length);//NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", hello);
            resp.addHeader("Access-Control-Allow-Origin", "*");
            resp.addHeader("Access-Control-Allow-Headers", "origin,accept,content-type");
            resp.addHeader("Access-Control-Allow-Credentials", "true");
            resp.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            resp.addHeader("Access-Control-Max-Age", "" + MAX_AGE);

            return resp;
        }
    }

    private final static int MAX_AGE = 42 * 60 * 60;
}
