package com.oxycast.chromecastapp.cloudserver;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.cloudrail.si.CloudRail;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.oxycast.chromecastapp.MainActivity;
import com.oxycast.chromecastapp.R;
import com.vistrav.ask.Ask;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class CloudFileViewer extends AppCompatActivity
        implements  View.OnClickListener {

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    private ImageView imageView_gDrive,imageView_dropBox,imageView_oneDrive;
    private AdView mAdView;
	private View selectorGoogle,selectorDropbox,selectorOneDrive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(MainActivity.getAppTitle(this,getResources().getString(R.string.app_name)));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAdView = (AdView) findViewById(R.id.adView);
        showAd(getIntent().getBooleanExtra("isAdFree",false));

    /**
     *  This is required for Android versions 6.0.0 and above. Android introduced a new permission
     *  police which requires a developer to not only put the required permissions into the manifest
     *  file but also prompt the user to grant the required permission during runtime. For this
     *  purpose we use a library called Ask (https://github.com/00ec454/Ask) since it makes the
     *  process easier than the using the standard Android API.
     */
        Ask.on(this).forPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).go();

        imageView_dropBox= (ImageView) findViewById(R.id.imageView_dropbox);
        imageView_gDrive= (ImageView) findViewById(R.id.imageView_gDrive);
        imageView_oneDrive= (ImageView) findViewById(R.id.imageView_oneDrive);
	    selectorDropbox=findViewById(R.id.selector_dropbox);
	    selectorGoogle=findViewById(R.id.selector_google);
	    selectorOneDrive=findViewById(R.id.selector_onedrive);

	    imageView_dropBox.setOnClickListener(this);
        imageView_gDrive.setOnClickListener(this);
        imageView_oneDrive.setOnClickListener(this);

	   Services.getInstance().prepare(this);

        sp = this.getPreferences(Context.MODE_PRIVATE);

        int service = sp.getInt("service", 0);
        setChecked(service);

    }

    public void setSelectors(int pos)
    {
	    selectorDropbox.setBackgroundResource(R.color.colorPrimary);
	    selectorGoogle.setBackgroundResource(R.color.colorPrimary);
	    selectorOneDrive.setBackgroundResource(R.color.colorPrimary);
	    switch (pos){
		    case 1:
			    selectorDropbox.setBackgroundResource(R.color.colorWhite);
		    	break;
		    case 2:
			    selectorGoogle.setBackgroundResource(R.color.colorWhite);
			    break;
		    case 3:
			    selectorOneDrive.setBackgroundResource(R.color.colorWhite);
			    break;

	    }

    }
public void setChecked(int service){
    setSelectors(service);
       if (service == 0) {
	       //imageView_logout.setVisibility(View.INVISIBLE);
            this.navigateToHome();
        } else {
	       //imageView_logout.setVisibility(View.VISIBLE);
            this.navigateToService(service);
        }
}
    private void showAd(boolean isAdFree) {

        if (!isAdFree){
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.GONE);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mAdView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    mAdView.setVisibility(View.GONE);
                }
            });
        }else{

            mAdView.setVisibility(View.GONE);
        }
    }
    @Override
    protected void onStop() {
        Services.getInstance().storePersistent();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
            Files fragment = (Files) getFragmentManager().findFragmentByTag("files");
            if(fragment == null) {
                super.onBackPressed();
                return;
            }
            if(fragment.onBackPressed()) super.onBackPressed();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.file_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==android.R.id.home){
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateToHome() {
        spe = sp.edit();
        spe.putInt("service", 0);
        spe.apply();

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment content = Home.newInstance();
        fragmentTransaction.replace(R.id.content, content);
        fragmentTransaction.commit();
    }

    private void navigateToService(int service) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment content = Files.newInstance(service);
        fragmentTransaction.replace(R.id.content, content, "files");
        fragmentTransaction.commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Files fragment = (Files) getFragmentManager().findFragmentByTag("files");
            if (fragment != null) {
                fragment.search(query);
            }
        } else if(intent.getCategories().contains("android.intent.category.BROWSABLE")) {
            CloudRail.setAuthenticationResponse(intent);
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onClick(View v) {
        if (v.getId()== R.id.imageView_dropbox){
            setChecked(1);
        }else if (v.getId()== R.id.imageView_gDrive){
            setChecked(2);
        }else if (v.getId()== R.id.imageView_oneDrive){
            setChecked(3);
        }

    }
    public void clearPersistent(){
	    final int service = sp.getInt("service", 0);
	    String currentDrive="";
	    switch (service){
		    case 1:
		    	currentDrive="DropBox";
		    	break;
		    case 2:
			    currentDrive="Google Drive";
			    break;
		    case 3:
			    currentDrive="One Drive";
			    break;

	    }
	    new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
			    .setTitleText("Logout")
			    .setContentText("do you want to logout from "+currentDrive+" ?")
			    .setCancelText("Cancel")
			    .setConfirmText("Yes")
			    .showCancelButton(true)
			    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
				    @Override
				    public void onClick(final SweetAlertDialog sweetAlertDialog) {


					    new Thread(new Runnable() {
						    @Override
						    public void run() {
							    Services.getInstance().clearPersistent(service);
							    spe = sp.edit();
							    spe.putInt("service", 0);
							    spe.apply();
							    CloudFileViewer.this.runOnUiThread(new Runnable() {
								    @Override
								    public void run() {
									    setChecked(0);
									    sweetAlertDialog.cancel();
								    }
							    });
						    }
					    }).start();


				    }
			    })
			    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
				    @Override
				    public void onClick(SweetAlertDialog sDialog) {
					    sDialog.cancel();
				    }
			    })
			    .show();

    }
}
