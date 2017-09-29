package com.oxycast.chromecastapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.utils.Utils;

/**
 * Created by Sergey on 01.03.2017.
 */

public class HistoryAdapter extends BaseAdapter {
    private Context mContext;
    private final DataSource mDataSource;

    private int mSize = 0;
    private Cursor mRowIds = null;

    public HistoryAdapter(Context c,DataSource dataSource) {
        mContext = c;
        mDataSource = dataSource;

        doQuery();
    }

    private void doQuery(){
        if(mRowIds!=null){
            mRowIds.close();
        }
        mRowIds = mDataSource.getRowIds();
        mSize = mRowIds.getCount();
    }

    @Override
    public int getCount() {
        return mSize;
    }

    @Override
    public Object getItem(int i) {
        if(mRowIds.moveToPosition(i)){
            long rowId = mRowIds.getLong(0);
            Cursor c = mDataSource.getRowById(rowId);
            return c;
        }else{
            return null;
        }
    }

    @Override
    public long getItemId(int i) {
        if(mRowIds.moveToPosition(i)){
            long rowId = mRowIds.getLong(0);
            return rowId;
        }else{
            return 0;
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        Activity act = (Activity) mContext;
        if (view == null) {

            LayoutInflater li = act.getLayoutInflater();
            view = li.inflate(R.layout.historyitem_layout, null);

        }

        if(mRowIds!=null &&  mRowIds.moveToPosition(i)) {

            long rowId = mRowIds.getLong(0);
            Cursor cursor = mDataSource.getRowById(rowId);
            if (cursor != null && cursor.moveToFirst()) {
                cursor.moveToFirst();

                String title = cursor.getString(1);

                String url = cursor.getString(2);

                if (TextUtils.isEmpty(title)) {
                    title = Utils.getHost(url);
                }

                url = Utils.getHost(url);
                byte[] thumb = cursor.getBlob(4);

                Bitmap thumbnail = null;

                if (thumb != null) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(thumb, 0, thumb.length);
                    thumbnail = bmp.copy(Bitmap.Config.ARGB_8888, true);
                } else {
                    thumbnail = BitmapFactory.decodeResource(act.getResources(), R.drawable.defaultwebicon);
                }

                ImageView thumbIV = (ImageView) view.findViewById(R.id.thumbnail);
                thumbIV.setImageBitmap(thumbnail);

                TextView titleTV = (TextView) view.findViewById(R.id.title);
                titleTV.setText(title);

                TextView urlTV = (TextView) view.findViewById(R.id.url);
                urlTV.setText(url);

                ImageView delButton = (ImageView) view.findViewById(R.id.deleteButton);
                delButton.setTag(cursor.getLong(0));

                delButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long id = ((Long) v.getTag()).longValue();
                        mDataSource.deleteRow(id);
                        doQuery();
                        notifyDataSetChanged();
                    }
                });


                cursor.close();
            }
        }
        return view;
    }


    public interface DataSource {
        Cursor getRowIds();
        Cursor getRowById(long rowId);
        void deleteRow(long rowId);
    }
}
