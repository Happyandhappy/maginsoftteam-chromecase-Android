package com.oxycast.chromecastapp.media;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.oxycast.chromecastapp.utils.Utils;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by sergey on 26.02.17.
 */


public abstract class XVideoUriExtractor extends AsyncTask<String, Void, ArrayList<Video>> {
    private ArrayList<Video> m2089a(String str) throws JSONException {
        ArrayList<Video> arrayList = new ArrayList();
        String a = Utils.ProcessString(str, "html5player.setVideoTitle('", "');");
        String a2 = Utils.ProcessString(str, "html5player.setVideoHLS('", "');");
        if (!TextUtils.isEmpty(a2)) {
            arrayList.add(new Video(a2, "application/x-mpegURL", "auto", a));
        }
        a2 = Utils.ProcessString(str, "html5player.setVideoUrlLow('", "');");
        if (!TextUtils.isEmpty(a2)) {
            arrayList.add(new Video(a2, "videos/mp4", "low", a));
        }
        a2 = Utils.ProcessString(str, "html5player.setVideoUrlHigh('", "');");
        if (!TextUtils.isEmpty(a2)) {
            arrayList.add(new Video(a2, "videos/mp4", "high", a));
        }
        return arrayList;
    }

    protected ArrayList<Video> m2090a(String... strArr) {
        try {
            return m2089a(Utils.getWebPage(strArr[0]));
        } catch (Exception e) {
         //   FirebaseCrash.report(new Exception("error when extract xvideos"));
            return null;
        }
    }

    public abstract void m2091a(ArrayList<Video> arrayList);

    protected void m2092b(ArrayList<Video> arrayList) {
        if (arrayList != null) {
            m2091a((ArrayList) arrayList);
        }
        super.onPostExecute(arrayList);
    }

    protected ArrayList<Video> doInBackground(String[] objArr) {
        return m2090a((String[]) objArr);
    }

    protected void onPostExecute(ArrayList<Video> obj) {
        m2092b((ArrayList) obj);
    }
}