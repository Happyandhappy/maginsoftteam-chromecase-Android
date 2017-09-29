package com.oxycast.chromecastapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.oxycast.chromecastapp.R;

/**
 * Created by sergey on 15.04.17.
 */

public class IptvAdapter extends BaseAdapter {

    private Context mContext;
    private final IptvAdapter.DataSource mDataSource;

    private int mSize = 0;
    private Cursor mRowIds = null;

    public IptvAdapter(Context c,IptvAdapter.DataSource dataSource) {
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
    public Object getItem(int position) {
        if(mRowIds.moveToPosition(position)){
            long rowId = mRowIds.getLong(0);
            Cursor c = mDataSource.getRowById(rowId);
            return c;
        }else{
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        if(mRowIds.moveToPosition(position)){
            long rowId = mRowIds.getLong(0);
            return rowId;
        }else{
            return 0;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        mRowIds.moveToPosition(position);
        long rowId = mRowIds.getLong(0);
        Cursor cursor = mDataSource.getRowById(rowId);
        cursor.moveToFirst();
        //View v;
        Activity act = (Activity) mContext;
        if (convertView == null) {

            LayoutInflater li = act.getLayoutInflater();
            convertView = li.inflate(R.layout.iptv_item, null);

        }

        String title = cursor.getString(1);

        TextView iptvButton = (TextView)convertView.findViewById(R.id.text);
        iptvButton.setText(title);
        iptvButton.setTag(cursor.getLong(0));

        cursor.close();
        return convertView;
    }
    public void Update()
    {
        doQuery();
        notifyDataSetChanged();
    }



    public interface DataSource {
        Cursor getRowIds();
        Cursor getRowById(long rowId);
        void deleteRow(long rowId);
    }
}
