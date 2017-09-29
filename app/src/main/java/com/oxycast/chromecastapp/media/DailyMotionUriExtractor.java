package com.oxycast.chromecastapp.media;

import android.os.AsyncTask;

import com.oxycast.chromecastapp.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by sergey on 26.02.17.
 */

public abstract class DailyMotionUriExtractor extends AsyncTask<String, Void, ArrayList<Video>> {
    private ArrayList<Video> m2084a(String str) throws JSONException {
        ArrayList<Video> arrayList = new ArrayList();
        String str2 = "url";
        str2 = "type";
        str2 = "title";
        str2 = "auto";
        str2 = "metadata";
        str2 = "qualities";
        JSONObject jSONObject = new JSONObject(str).getJSONObject("metadata");
        String string = jSONObject.getString("title");
        JSONObject jSONObject2 = jSONObject.getJSONObject("qualities");
        Iterator keys = jSONObject2.keys();
        while (keys.hasNext()) {
            str2 = (String) keys.next();
            if (!"auto".equals(str2)) {
                arrayList.add(0, new Video(jSONObject2.getJSONArray(str2).getJSONObject(1).getString("url"), jSONObject2.getJSONArray(str2).getJSONObject(1).getString("type"), str2 + "p", string));
            }
        }
        return arrayList;
    }

    protected ArrayList<Video> m2085a(String... strArr) {
        ArrayList<Video> arrayList = null;
        try {
            String str = "http://dailymotion.com/video/video_id";
            CharSequence a = Utils.ProcessString(strArr[0], "video/", "?");
            if (a != null) {
                arrayList = m2084a(Utils.ProcessString(Utils.getWebPage(str.replace("video_id", a)), "var config = ", "};") + "}");
            }
        } catch (Exception e) {
           // FirebaseCrash.report(new Exception("error when extract dailymotion"));
        }
        return arrayList;
    }

    public abstract void m2086a(ArrayList<Video> arrayList);

    protected void m2087b(ArrayList<Video> arrayList) {
        if (arrayList != null) {
            m2086a((ArrayList) arrayList);
        }
        super.onPostExecute(arrayList);
    }

    protected ArrayList<Video> doInBackground(String[] objArr) {
        return m2085a((String[]) objArr);
    }

    protected void onPostExecute(ArrayList<Video> obj) {
        m2087b((ArrayList) obj);
    }
}