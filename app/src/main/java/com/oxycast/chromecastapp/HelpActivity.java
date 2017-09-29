package com.oxycast.chromecastapp;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.framework.CastButtonFactory;

public class HelpActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(MainActivity.getAppTitle(this,getResources().getString(R.string.app_name)));

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_help, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.menuContact) {
            /* Create the Intent */
            final Intent emailIntent = new Intent(Intent.ACTION_SEND);

                /* Fill it with Data */
            emailIntent.setType("plain/text");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"android_support@oxycast.tv"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "");

                /* Send it off to the Activity-Chooser */
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        }
        return false;
    }
}
