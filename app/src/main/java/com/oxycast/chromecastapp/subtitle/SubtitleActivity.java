package com.oxycast.chromecastapp.subtitle;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.adapters.SubtitleAdapter;
import com.oxycast.chromecastapp.app.ChromecastApp;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.List;

public class SubtitleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_dialog);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        android.support.v7.widget.SearchView sv = (android.support.v7.widget.SearchView) findViewById(R.id.searchBox);
        final ListView listResult = (ListView) findViewById(R.id.listResult);
        if(sv!=null && ChromecastApp.currentVideo!=null)
        {
            String fileNameWithOutExt = FilenameUtils.removeExtension(new File(ChromecastApp.currentVideo.getUrl()).getName());
            sv.setQuery(fileNameWithOutExt,false);
        }
        if(sv!=null)
        {
            sv.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    progressBar.setVisibility(View.VISIBLE);
                    try {
                        AsyncSubtitles mASub = new AsyncSubtitles(SubtitleActivity.this, new AsyncSubtitles.SubtitlesInterface() {
                            @Override
                            public void onSubtitlesListFound(List<OSubtitle> list) {
                                Log.d("Subtitle","Finded " + list.size());
                                progressBar.setVisibility(View.INVISIBLE);
                                SubtitleAdapter adapter = new SubtitleAdapter(SubtitleActivity.this,list);
                                if(listResult!=null)
                                {
                                    listResult.setAdapter(adapter);
                                }
                            }

                            @Override
                            public void onSubtitleDownload(boolean b) {

                            }

                            @Override
                            public void onError(int error) {
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        });
                        mASub.setLanguagesArray(new String[]{"en"});
                        ORequest req = new ORequest("", query, null, new String[]{"spa","eng"});
                        mASub.setNeededParamsToSearch(req);
                        mASub.getPossibleSubtitle();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }

    }
}
