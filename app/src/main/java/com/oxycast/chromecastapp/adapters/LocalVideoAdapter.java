package com.oxycast.chromecastapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.logger.LoggerWrapper;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sergey on 26.03.17.
 */

public class LocalVideoAdapter extends BaseAdapter {

    Context context;
    ArrayList<String> videos = new ArrayList<String>();
    int width = 0;
    int height = 0;
    Picasso picasso = null;
    LoggerWrapper logger;
    public LocalVideoAdapter(Context context, ArrayList<String> videos)
    {
        this.context = context;
        this.videos.addAll(videos);

        Resources r = context.getResources();
        width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 133, r.getDisplayMetrics());
        height =(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());
;
       // initCacheBitmap();
        // create Picasso.Builder object
        Picasso.Builder picassoBuilder = new Picasso.Builder(context);

// add our custom eat foody request handler (see below for full class)
        picassoBuilder.addRequestHandler(new ThumbnailRequestHandler());

// Picasso.Builder creates the Picasso object to do the actual requests
         picasso = picassoBuilder.build();
    }

    public  void setLogger(LoggerWrapper wrapper)
    {
        this.logger = wrapper;
    }

    @Override
    public int getCount() {
        return videos.size();
    }

    public void updateVideoList(ArrayList<String> newvideos)
    {
        this.videos.clear();
        this.videos.addAll(newvideos);
        notifyDataSetChanged();
    }

    @Override
    public Object getItem(int position) {
        return videos.get(position);
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
            convertView = li.inflate(R.layout.localvideo_item, null);
        }

        String video = videos.get(position);
        File f = new File(video);
        TextView textView = (TextView)convertView.findViewById(R.id.title);
        if(textView!=null)
        {


            textView.setText(f.getName());
        }

        ImageView thumbnailView = (ImageView)convertView.findViewById(R.id.thumbnail);
        if(thumbnailView!=null)
        {
            picasso
                    .load(video)
                    .into(thumbnailView);

        }

        String durationStr = duration(f);

        TextView tvDuration = (TextView) convertView.findViewById(R.id.duration);
        if(tvDuration!=null)
        {
            tvDuration.setText(durationStr);
        }

        return convertView;
    }

    public String duration(File file)
    {

        String formatted = "00:00";
        try
        {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//use one of overloaded setDataSource() functions to set your data source
            retriever.setDataSource(context, Uri.fromFile(file));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (TextUtils.isEmpty(time) == false) {
                Log.d("LocalVideo", "File:" + file.getName() + " Duration : " + time);
                long timeInMillisec = Long.parseLong(time);


                int duration = (int) timeInMillisec / 1000;

                int hours = duration / 3600;
                int minutes = (duration / 60) - (hours * 60);
                int seconds = duration - (hours * 3600) - (minutes * 60);
                if (hours > 0) {
                    formatted = String.format("%d:%02d:%02d", hours, minutes, seconds);
                } else {
                    formatted = String.format("%02d:%02d", minutes, seconds);
                }
            }
        }
        catch (Exception ex)
        {
            if(logger!=null)
            {
                logger.error("Get duration error",ex);
            }
        }
        return formatted;
    }

    public class ThumbnailRequestHandler extends RequestHandler {
        private static final String THUMBNAIL_SCHEME = "file";
        private HashMap<String,Bitmap> cache = new HashMap<String,Bitmap>();
        @Override
        public boolean canHandleRequest(Request data) {
            return true;
        }

        @Override
        public Result load(Request request, int networkPolicy) throws IOException {

            Log.d("ThumbnailRequestHandler", "Process: " + request.uri.toString());
            Bitmap thumbnail = null;

            thumbnail = cache.get(request.uri.toString());
            if(thumbnail == null) {
                Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(request.uri.toString(), MediaStore.Video.Thumbnails.MINI_KIND);

                thumbnail = ThumbnailUtils.extractThumbnail(bitmap, width, height);
                cache.put(request.uri.toString(),thumbnail);
            }
            // return the result with the bitmap and the source info
            return new Result(thumbnail, Picasso.LoadedFrom.DISK);
        }
    }
}
