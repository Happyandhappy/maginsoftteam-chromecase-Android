package com.oxycast.chromecastapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.subtitle.OSubtitle;

import java.util.List;

/**
 * Created by sergey on 03.04.17.
 */

public class SubtitleAdapter extends BaseAdapter {
    Context context;
    List<OSubtitle> subtitles;
    public SubtitleAdapter(Context context, List<OSubtitle> subtitles)
    {
        this.context = context;
        this.subtitles = subtitles;
    }

    @Override
    public int getCount() {
        return subtitles.size();
    }

    @Override
    public Object getItem(int position) {
        return subtitles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Activity act = (Activity) context;
        if (convertView == null) {

            LayoutInflater li = act.getLayoutInflater();
            convertView = li.inflate(R.layout.subtitle_item, null);

        }

        OSubtitle subtitle = subtitles.get(position);

        String title = "[" + subtitle.getLanguageName()+"]" +" " + subtitle.getMovieName() + " (" + subtitle.getMovieYear() + ")";

        TextView tvTitle = (TextView) convertView.findViewById(R.id.subtitle);

        tvTitle.setText(title);

        return  convertView;
    }
}
