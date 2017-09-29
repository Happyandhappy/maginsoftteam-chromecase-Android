package com.oxycast.chromecastapp;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import com.microsoft.onedrivesdk.picker.IPicker;
import com.microsoft.onedrivesdk.picker.IPickerResult;
import com.microsoft.onedrivesdk.picker.LinkType;
import com.microsoft.onedrivesdk.picker.Picker;
import com.oxycast.chromecastapp.app.ChromecastApp;
import com.oxycast.chromecastapp.cast.CastVideo;
import com.oxycast.chromecastapp.cast.ICastStatus;
import com.oxycast.chromecastapp.media.Video;
import com.oxycast.chromecastapp.utils.AdProvider;
import com.oxycast.chromecastapp.utils.Utils;

import java.net.HttpURLConnection;
import java.net.URL;

public class ServerActivity extends AppCompatActivity  implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, ICastStatus {
    private AdView mAdView;
    private final int REQUEST_CODE_RESOLUTION = 1001;
    private final int REQ_CODE_OPEN = 1002;
    static final int DBX_CHOOSER_REQUEST = 1003;
    private GoogleApiClient mGoogleApiClient;
    private IPicker mPicker;
    Video playAfterConnect;
    private CastStateListener mCastStateListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        ChromecastApp.Instance().mCastContext = CastContext.getSharedInstance(this);

        mAdView = (AdView) findViewById(R.id.adView);
        showAd();

        // Create the picker instance
        mPicker = Picker.createPicker(getString(R.string.onedrive_app_id));


        Button gdriveButton = (Button) findViewById(R.id.gdrivebutton);
        gdriveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    IntentSender i = Drive.DriveApi
                            .newOpenFileActivityBuilder()
                            //.setMimeType(new String[]{"text/plain"})
                            .build(mGoogleApiClient);
                    startIntentSenderForResult(i, REQ_CODE_OPEN, null, 0, 0, 0);
                }
                catch (Exception ex)
                {
                    Log.e("GOOGLEDRIVE",ex.getMessage(),ex);
                }
            }
        });

        Button onedriveButton = (Button) findViewById(R.id.onedrivebutton);
        onedriveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Determine the link type that was selected
                LinkType linkType = LinkType.DownloadLink;

                // Start the picker
                mPicker.startPicking(ServerActivity.this, linkType);
            }
        });

        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    //showIntroductoryOverlay();
                }

                if (newState == CastState.CONNECTED) {
                    if (playAfterConnect != null) {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Video video = new Video(playAfterConnect.getTitle(),playAfterConnect.getUrl().toString());
                                String mime = Utils.getMimeTypeFromNetwork(video.getUrl());
                                video.setMimeType(mime);

                                sendCastMessage(video);
                                playAfterConnect = null;
                            }
                        });
                        thread.start();
                    }
                }

            }
        };

    }
    private void showAd() {
        Log.v("suraj","showAd called");
        if (!AdProvider.isAdFree){
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
    private MenuItem mediaRouteMenuItem;

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browse, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    private void sendCastMessage(Video video)
    {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putSerializable("video",video);
        msg.setData(bundle);
        msg.what = 1;
        handler.sendMessage(msg);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info.position >= 0) {
            getMenuInflater().inflate(R.menu.iptv_list_context_menu, menu);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();


        }
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    int makeShared = 0;
    final private ResultCallback<DriveResource.MetadataResult> metadataCallback = new
            ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Toast.makeText(ServerActivity.this,"Problem while trying to fetch metadata",Toast.LENGTH_LONG).show();
                        return;
                    }
                    Metadata metadata = result.getMetadata();

                    //showMessage("Metadata successfully fetched. Title: " + metadata.getTitle());
                    Toast.makeText(ServerActivity.this,"MIME: "  + metadata.getMimeType() + " Link " + metadata.getWebContentLink(),Toast.LENGTH_LONG).show();
                    Log.d("GOOGLEDRIVE", "Link: " + metadata.getWebContentLink());
                   // AccountManager.get(this).getAuthToken()
                    //Plus.AccountApi.getAccountName(mGoogleApiClient)
                  //  String url = metadata.getAlternateLink() +

                    if(metadata.isShared() == true)
                    {
                         //File is shared. Cast it;
                    }
                    else {
                        if(makeShared == 0) {
                            DriveId driveId = metadata.getDriveId();

                            DriveFile file = driveId.asDriveFile();
                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setStarred(true)
                                    .setIndexableText("Description about the file")
                                    .setTitle("A new title").build();
                            file.updateMetadata(mGoogleApiClient, changeSet)
                                    .setResultCallback(metadataCallback);
                            makeShared = 1;
                        }
                        else{
                            makeShared = 0;
                            Toast.makeText(ServerActivity.this,"Application can't shared file", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            };

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
               Log.d("GDRIVE", "REQUEST_CODE_RESOLUTION");
                break;
            case REQ_CODE_OPEN:
                Log.d("GDRIVE", "REQ_CODE_OPEN");
                DriveId driveId = (DriveId) data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
            //    driveId.asDriveFile().

                 driveId.asDriveFile().getMetadata(mGoogleApiClient).setResultCallback(metadataCallback);
                Log.d("GOOGLEDRIVE", "DriveID" + driveId);
                Toast.makeText(ServerActivity.this,"DriveID " + driveId, Toast.LENGTH_LONG).show();
                break;

            default:
                // Get the results from the from the picker
                final IPickerResult result = mPicker.getPickerResult(requestCode, resultCode, data);

                // Handle the case if nothing was picked
                if (result == null) {
                    Toast.makeText(this, getString(R.string.onedrive_not_file), Toast.LENGTH_LONG).show();
                    return;
                }

                if (ChromecastApp.Instance().mCastContext.getCastState() == CastState.CONNECTED) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Video video = new Video(result.getName(),result.getLink().toString());
                            String mime = Utils.getMimeTypeFromNetwork(video.getUrl());
                            video.setMimeType(mime);
                            sendCastMessage(video);
                        }
                    });
                    thread.start();
                } else {
                    Video video = new Video(result.getName(),result.getLink().toString());
                    video.setMimeType("video/webm");
                    playAfterConnect = video;
                    ActionProvider provider = MenuItemCompat.getActionProvider(mediaRouteMenuItem);
                    provider.onPerformDefaultAction();


                    // Toast.makeText(WebViewActivity.this,getString(R.string.not_connected),Toast.LENGTH_LONG).show();
                }

                Toast.makeText(ServerActivity.this,"Choosed file: " + result.getLink().toString(), Toast.LENGTH_LONG).show();
                Log.d("ONEDRIVE", "Choosed file: " + result.getLink().toString());
                // Update the UI with the picker results

               break;
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 1) {
                Bundle res = msg.getData();
                Video video = (Video)res.getSerializable("video");

                CastVideo castVideo = new CastVideo(ServerActivity.this);
                castVideo.setStatusListener(ServerActivity.this);
                castVideo.Cast(video);
            }
        }
    };

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i("GOOGLEDRIVE", "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e("GOOGLEDRIVE", "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i("GOOGLEDRIVE", "API client connected.");
       // Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i("GOOGLEDRIVE", "GoogleApiClient connection suspended");
    }

    @Override
    public void onStopped() {

    }

    @Override
    public void onError(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ServerActivity.this);
        builder.setTitle(R.string.iptv_unable_cast_title)
                .setMessage(R.string.iptv_unable_cast)
                .setCancelable(true)
                .setNegativeButton("ОК",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
