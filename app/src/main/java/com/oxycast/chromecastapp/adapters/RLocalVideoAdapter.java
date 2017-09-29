package com.oxycast.chromecastapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.oxycast.chromecastapp.LocalVideoActivity;
import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.logger.LoggerWrapper;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by satyam on 9/10/2017.
 */

public class RLocalVideoAdapter  extends RecyclerView.Adapter<RLocalVideoAdapter.MyViewHolder> {
    Context context;
    ArrayList<String> videos;
    int width = 0;
    int height = 0;
    Picasso picasso = null;
    LoggerWrapper logger;
    MediaMetadataRetriever retriever;
    public RLocalVideoAdapter(Context context, ArrayList<String> videos) {
        this.context = context;
        this.videos=videos;
        Resources r = context.getResources();
        width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 133, r.getDisplayMetrics());
        height =(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());
        retriever = new MediaMetadataRetriever();
        // initCacheBitmap();
        // create Picasso.Builder object
        Picasso.Builder picassoBuilder = new Picasso.Builder(context);

        // add our custom eat foody request handler (see below for full class)
        picassoBuilder.addRequestHandler(new ThumbnailRequestHandler());

        // Picasso.Builder creates the Picasso object to do the actual requests
        picasso = picassoBuilder.build();
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_video_list, parent, false);
        MyViewHolder myViewHolder = new MyViewHolder(view);

        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        String video = videos.get(position);
        File f = new File(video);
        holder.textViewTitle.setText(f.getName());
        final ImageView imageViewThumb=holder.imageViewThumb;
        picasso.load(video)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN){
                            imageViewThumb.setBackground(new BitmapDrawable(context.getResources(),bitmap));

                        }else {
                            imageViewThumb.setBackgroundDrawable(new BitmapDrawable(context.getResources(),bitmap));
                        }
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });

        holder.textViewDuration.setText(duration(f));
        holder.cardRoot.setOnClickListener(new MyClickListener(video));
    }

    @Override
    public int getItemCount() {
        return videos.size();


    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        Log.v("wasim","onDetachedFromRecyclerView");
        retriever.release();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        ImageView imageViewThumb;
        TextView textViewTitle,textViewDuration;
        public MyViewHolder(View itemView) {
            super(itemView);
            cardRoot=itemView.findViewById(R.id.layout_video);
            imageViewThumb = (ImageView) itemView.findViewById(R.id.imageView_albumArt);
            textViewTitle = (TextView) itemView.findViewById(R.id.textViewTitle);
            textViewDuration = (TextView) itemView.findViewById(R.id.textViewDuration);

        }


    }

    class MyClickListener implements View.OnClickListener {
        String video;

        public MyClickListener(String video) {
            this.video = video;
        }

        @Override
        public void onClick(View v) {
            ((LocalVideoActivity)context).listClick(video);
        }
    }
    public  void setLogger(LoggerWrapper wrapper)
    {
        this.logger = wrapper;
    }
    public void updateVideoList(ArrayList<String> newvideos)
    {
        this.videos.clear();
        this.videos.addAll(newvideos);
        notifyDataSetChanged();
    }

    public String duration(File file)
    {
        String formatted = "00:00";
        try
        {

            //use one of overloaded setDataSource() functions to set your data source
            retriever.setDataSource(context, Uri.fromFile(file));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (!TextUtils.isEmpty(time)) {
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
