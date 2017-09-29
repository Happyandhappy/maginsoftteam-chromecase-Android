package com.oxycast.chromecastapp.media;

import android.os.AsyncTask;

import com.oxycast.chromecastapp.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sergey on 25.02.17.
 */

public abstract class VimeoUriExtractor extends AsyncTask<String, Void, ArrayList<Video>> {
    private ArrayList<Video> m2079a(String str) throws JSONException {
        ArrayList<Video> arrayList = new ArrayList();
        String str2 = "request";
        str2 = "files";
        str2 = "progressive";
        str2 = "mime";
        str2 = "url";
        str2 = "quality";
        str2 = "video";
        str2 = "title";
        JSONObject jSONObject = new JSONObject(str);
        JSONArray jSONArray = jSONObject.getJSONObject("request").getJSONObject("files").getJSONArray("progressive");
        String string = jSONObject.getJSONObject("video").getString("title");
        for (int length = jSONArray.length() - 1; length >= 0; length--) {
            arrayList.add(0, new Video(jSONArray.getJSONObject(length).getString("url"), jSONArray.getJSONObject(length).getString("mime"), jSONArray.getJSONObject(length).getString("quality"), string));
        }
        return arrayList;
    }

    protected ArrayList<Video> m2080a(String... strArr) {
        try {
            return m2079a(Utils.getWebPage("https://player.vimeo.com/video/video_id/config".replace("video_id", strArr[0].substring(31, 40))));
        } catch (Exception e) {
          //  FirebaseCrash.report(new Exception("error when extract vimeo"));
            return null;
        }
    }

    public abstract void m2081a(ArrayList<Video> arrayList);

    protected void m2082b(ArrayList<Video> arrayList) {
        if (arrayList != null) {
            m2081a((ArrayList) arrayList);
        }
        super.onPostExecute(arrayList);
    }

    protected ArrayList<Video> doInBackground(String... objArr) {
        return m2080a((String[]) objArr);
    }

    protected void onPostExecute(ArrayList<Video> obj) {
        m2082b((ArrayList) obj);
    }
}