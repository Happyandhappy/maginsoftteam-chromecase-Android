package com.oxycast.chromecastapp.cloudserver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;
import com.oxycast.chromecastapp.R;

import java.util.List;

/**
 * Created by patrick on 08.04.16.
 */
public class CloudMetadataAdapter extends ArrayAdapter<CloudMetaData> {

    private List<CloudMetaData> data;
    private CloudStorage service;

    public CloudMetadataAdapter(Context context, int resource, List<CloudMetaData> objects) {
        super(context, resource, objects);
        this.data = objects;
    }
    public CloudMetadataAdapter(Context context, int resource, List<CloudMetaData> objects, CloudStorage service) {
        super(context, resource, objects);
        this.data = objects;
        this.service = service;
    }

    @Override
    public void remove(CloudMetaData object) {
        String target = object.getName();

        for(int i = 0; i < this.data.size(); ++i) {
            CloudMetaData cloudMetaData = this.data.get(i);
            if(cloudMetaData.getName().equals(target)) {
                this.data.remove(i);
                break;
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // assign the view we are converting to a local variable
        View v = convertView;

        // first check to see if the view is null. if so, we have to inflate it.
        // to inflate it basically means to render, or show, the view.
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.list_item, null);
        }

        final CloudMetaData cmd = this.data.get(position);

        if(cmd != null) {
            final ImageView img = (ImageView) v.findViewById(R.id.icon);
            final ImageView img_cast = (ImageView) v.findViewById(R.id.icon_cast);
            if(img != null) {
                if(cmd.getFolder()) {
                    img.setImageResource(R.drawable.ic_file_folder);
                    img_cast.setVisibility(View.INVISIBLE);
                } else {
                    img.setImageResource(R.drawable.ic_video);
                    img_cast.setVisibility(View.VISIBLE);
                }
            }

            TextView tv = (TextView) v.findViewById(R.id.list_item);
            tv.setText(cmd.getName());
        }

        return v;
    }
}
