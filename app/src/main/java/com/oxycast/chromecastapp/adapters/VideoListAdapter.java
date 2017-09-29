package com.oxycast.chromecastapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.media.Video;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;

/**
 * Created by Sergey on 03.03.2017.
 */

public class VideoListAdapter extends BaseAdapter {
    Context context;
    ArrayList<Video> videos;
    public VideoListAdapter(Context context, ArrayList<Video> videos)
    {
        this.context = context;
        this.videos =  videos;
    }

    @Override
    public int getCount() {
        return videos.size();
    }

    @Override
    public Object getItem(int i) {
        return videos.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Activity act = (Activity) context;
        if (view == null) {

            LayoutInflater li = act.getLayoutInflater();
            view = li.inflate(R.layout.video_item, null);

        }

        Video video = videos.get(i);

        String title = video.getTitle();

        String url = video.getUrl();

        String ext = FilenameUtils.getExtension(url);
        int qindex = ext.indexOf("?");
        if(qindex>=0)
        {
            ext = ext.substring(0,qindex);
        }

        TextView tvTitle = (TextView) view.findViewById(R.id.title);
        TextView tvExt = (TextView) view.findViewById(R.id.ext);

        tvTitle.setText(title);
        tvExt.setText(ext);

        return  view;
    }
}
