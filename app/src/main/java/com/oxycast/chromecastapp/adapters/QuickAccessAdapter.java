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
 * Created by sergey on 03.03.17.
 */

public class QuickAccessAdapter extends BaseAdapter {
    private Context mContext;
    private final QuickAccessAdapter.DataSource mDataSource;

    private int mSize = 0;
    private Cursor mRowIds = null;

    public QuickAccessAdapter(Context c,QuickAccessAdapter.DataSource dataSource) {
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

    public void Update()
    {
        doQuery();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        mRowIds.moveToPosition(i);
        long rowId = mRowIds.getLong(0);
        Cursor cursor = mDataSource.getRowById(rowId);
        cursor.moveToFirst();
        Activity act = (Activity) mContext;
        if (view == null) {

            LayoutInflater li = act.getLayoutInflater();
            view = li.inflate(R.layout.quick_access_item, null);

        }

        String title = cursor.getString(1);

        String url = cursor.getString(2);

        if(TextUtils.isEmpty(title))
        {
            title = Utils.getHost(url);
        }

        url = Utils.getHost(url);
        byte[] thumb=cursor.getBlob(5);

        Bitmap thumbnail = null;

        if(thumb!=null)
        {
            Bitmap bmp = BitmapFactory.decodeByteArray(thumb, 0, thumb.length);
            thumbnail = bmp.copy(Bitmap.Config.ARGB_8888, true);
        }
        else {
            thumbnail = BitmapFactory.decodeResource(act.getResources(), R.drawable.defaultwebicon);
        }

        ImageView thumbIV = (ImageView) view.findViewById(R.id.thumbnail);
        thumbIV.setImageBitmap(thumbnail);

        TextView titleTV = (TextView) view.findViewById(R.id.title);
        titleTV.setText(title);

        view.setTag(rowId);

        cursor.close();
        return view;
    }

    public interface DataSource {
        Cursor getRowIds();
        Cursor getRowById(long rowId);
        void deleteRow(long rowId);
    }
}
