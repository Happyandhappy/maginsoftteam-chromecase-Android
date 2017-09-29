package com.oxycast.chromecastapp;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

public class Settings extends AppCompatActivity {
    SwitchCompat remember_device,pause_on_call;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        pause_on_call=(SwitchCompat) findViewById(R.id.pause_on_call);
        remember_device=(SwitchCompat) findViewById(R.id.remember_device);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Settings.this);
        final SharedPreferences.Editor editor = prefs.edit();
        boolean isclearonexit  = prefs.getBoolean("pause_on_call",false);
        pause_on_call.setChecked(isclearonexit);



        pause_on_call.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(!isChecked)
                {
                    editor.putBoolean("pause_on_call",false);
                }
                else{
                    editor.putBoolean("pause_on_call",true);
                }
                editor.commit();

            }
        });
    }
}
