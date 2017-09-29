package com.oxycast.chromecastapp.media;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sergey on 25.02.17.
 */

public class Video implements Serializable {
    private String Title;
    private String Url;
    private String MimeType;
    private String Quality;
    private String Subtitle;
    private boolean isproxy = false;
    public List<Video> otherVideos;
    public Video(String url)
    {
        Url = url;
    }

    public List<Video> getOtherVideos()
    {
        return otherVideos;
    }

    public void setOtherVideo(ArrayList<Video> other)
    {
        this.otherVideos = other;
    }

    public Video(String title,String url)
    {
        Url = url;
        Title = title;
    }

    public Video(String url,String mime,String quality, String title)
    {
        Url = url;
        Title = title;
        MimeType=mime;
        Quality = quality;
    }

    public String getTitle()
    {
        return Title;
    }

    public void setTitle(String title)
    {
        Title = title;
    }

    public void setUrl(String url)
    {
       Url = url;
    }

    public String getUrl(){
        return Url;
    }

    public void setMimeType(String str) {
        if (str != null) {
            this.MimeType = str;
        }
    }

    public void setQuality(String quality)
    {
        Quality = quality;
    }

    public String getQuality()
    {
        return Quality;
    }

    public String getMimeType()
    {
        return MimeType;
    }


    public void setSubtitle(String subtitle)
    {
        this.Subtitle = subtitle;
    }

    public String getSubtitle()
    {
        return Subtitle;
    }

    private String localPath;

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean isproxy() {
        return isproxy;
    }

    public void setIsproxy(boolean isproxy) {
        this.isproxy = isproxy;
    }
}
