package com.oxycast.chromecastapp.cast;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity;
import com.oxycast.chromecastapp.MainActivity;
import com.oxycast.chromecastapp.R;
import com.oxycast.chromecastapp.adapters.SubtitleAdapter;
import com.oxycast.chromecastapp.app.ChromecastApp;
import com.oxycast.chromecastapp.logger.LoggerWrapper;
import com.oxycast.chromecastapp.media.Video;
import com.oxycast.chromecastapp.subtitle.AsyncSubtitles;
import com.oxycast.chromecastapp.subtitle.ORequest;
import com.oxycast.chromecastapp.subtitle.OSubtitle;
import com.oxycast.chromecastapp.utils.Utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import subtitleFile.FormatSRT;
import subtitleFile.TimedTextFileFormat;
import subtitleFile.TimedTextObject;

//import com.com.oxycast.chromecastapp.R;

/**
 * Created by sergey on 26.02.17.
 */

public class MyExpandedControllerActivity extends ExpandedControllerActivity implements AsyncSubtitles.SubtitlesInterface {

    private final int REQUEST_CODE_OPEN_DIRECTORY = 1001;
    private final MyHandler mHandler = new MyHandler(this);
    LoggerWrapper logger = null;
    File fSubtitle = null;
    SharedPreferences prefs = null;
    SharedPreferences.Editor editor = null;
    MediaQueueItem currentItem = null;
    RemoteMediaClient remoteMediaClient = null;
    String subtitlePath;
    AsyncSubtitles mASub;
    OSubtitle subtitle;
    AppCompatDialog dialog1;
    Handler handler2 = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == 0) {
                android.support.v7.app.AlertDialog dialog = null;

                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MyExpandedControllerActivity.this, R.style.SubtitleDialog);
                builder.setTitle(R.string.android5_protected2).setMessage(R.string.android5_protected1)
                        .setCancelable(true)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                        intent.setFlags(64);
                                        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
                                    }
                                })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                dialog = builder.create();

                dialog.show();
            }

            if (msg.what == 1) {
                Toast.makeText(MyExpandedControllerActivity.this, getString(R.string.subtitle_error), Toast.LENGTH_LONG).show();
            }

            if (msg.what == 2) {
                String uri = prefs.getString("sdroot", "");
                Uri root = Uri.parse(uri);
                saveSubtitle21(root);
            }
        }
    };
    Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            Bundle res = msg.getData();
            final String text = res.getString("text");
            Log.d("Subtitle", "Link:" + text);
            logger.info("Subtitle link: " + text);
            final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MyExpandedControllerActivity.this, R.style.SubtitleDialog);
            builder.setMessage(getString(R.string.restartvideo))
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                            if (remoteMediaClient == null) {

                                Log.d("ChromecastApp", "Remote media client is null");
                                logger.error("Remote media client is null");
                                return;
                            }
                            MediaInfo info = null;
                            MediaQueueItem item = remoteMediaClient.getCurrentItem();
                            if (item == null) {
                                item = currentItem;
                            }
                            if (item != null)
                                info = remoteMediaClient.getCurrentItem().getMedia();
                            else
                                info = remoteMediaClient.getMediaInfo();
                            MediaTrack subtitleTrack = new MediaTrack.Builder(1 /* ID */,

                                    MediaTrack.TYPE_TEXT)
                                    .setName("Subtitle")
                                    .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                                    .setContentId(text)
                                    .setLanguage(subtitle.getISO639())
                                    .build();

                            for (int i = info.getMediaTracks().size() - 1; i >= 0; i--) {
                                MediaTrack track = info.getMediaTracks().get(i);
                                if (track.getType() == MediaTrack.TYPE_TEXT && track.getSubtype() == MediaTrack.SUBTYPE_SUBTITLES) {
                                    info.getMediaTracks().remove(track);
                                }
                            }
                            info.getMediaTracks().add(subtitleTrack);


                            MediaQueueItem queueItem = new MediaQueueItem.Builder(info)
                                    .setAutoplay(true)
                                    .setPreloadTime(10)
                                    .build();


                            remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
                                @Override
                                public void onStatusUpdated() {
                                    Log.d("Subtitle", "Enable subtitle");
                                    if (remoteMediaClient.getMediaStatus().getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING) {
                                        logger.info("Start subtitle track");
                                        remoteMediaClient.setActiveMediaTracks(new long[]{1});
                                        remoteMediaClient.removeListener(this);
                                    }
                                }

                                @Override
                                public void onMetadataUpdated() {

                                }

                                @Override
                                public void onQueueStatusUpdated() {

                                }

                                @Override
                                public void onPreloadStatusUpdated() {

                                }

                                @Override
                                public void onSendingRemoteMediaRequest() {

                                }

                                @Override
                                public void onAdBreakStatusUpdated() {

                                }
                            });
                            if (item != null) {
                                int id = item.getItemId();
                                remoteMediaClient.queueInsertAndPlayItem(queueItem, id, null);
                                logger.info("Start video with subtitle");
                                // remoteMediaClient.setActiveMediaTracks(new long[]{1});
                                remoteMediaClient.queueRemoveItem(id, null);
                            } else {
                                remoteMediaClient.load(info);
                            }
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AppCompatDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(true);
            dialog.show();


//            remoteMediaClient(info,true,0,new long[]{subtitle.getId()},null);
        }
    };

    public static void writeFileTxt(String fileName, String[] totalFile, String enc) {

        CharsetEncoder encoder = Charset.forName(enc).newEncoder();
        File file = null;
        PrintWriter pw = null;
        try {
            file = new File(fileName);
            if (file.exists() == false) {
                file.createNewFile();
            }
            pw = new PrintWriter(file, enc);

            for (int i = 0; i < totalFile.length; i++) {
                Log.d("Subtitle", totalFile[i]);
                pw.println(totalFile[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Execute the "finally" to make sure the file is closed

            } catch (Exception e2) {
                e2.printStackTrace();
            }

            try {
                if (pw != null)
                    pw.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle var1) {
        super.onCreate(var1);
        ChromecastApp.Instance().mCastContext = CastContext.getSharedInstance(this);
        CastSession mCastSession = ChromecastApp.Instance().mCastContext.getSessionManager().getCurrentCastSession();
        remoteMediaClient = ChromecastApp.Instance().mCastContext.getSessionManager().getCurrentCastSession().getRemoteMediaClient();
        if (remoteMediaClient == null) {
            Log.d("ChromecastApp", "Remote media client is null");
            logger.error("Remote media client is null");
            return;
        }
        currentItem = remoteMediaClient.getCurrentItem();
        logger = new LoggerWrapper(MyExpandedControllerActivity.class);
        //     final RemoteMediaClient remoteMediaClient = ChromecastApp.mCastContext.getSessionManager().getCurrentCastSession().getRemoteMediaClient();
        prefs = PreferenceManager.getDefaultSharedPreferences(MyExpandedControllerActivity.this);
        editor = prefs.edit();
        ImageView iv = this.getButtonImageViewAt(3);

        iv.setBackgroundColor(getResources().getColor(android.R.color.black));
        iv.setImageDrawable(getDrawableResource(com.google.android.gms.R.drawable.cast_ic_expanded_controller_closed_caption));

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logger.info("Open find subtitle dialog");
                showDialog();

            }
        });
        logger.info("Open expanded controller");
        int a = 0;
    }

    private Drawable getDrawableResource(int resID) {
        return ContextCompat.getDrawable(this, resID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browse, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123) {
            if (resultCode != RESULT_OK) return;

            Uri currentUri = data.getData();
            writeFileContent(currentUri);
            // copyFile(fileToCopy, data.getData());
            File temp = new File(ChromecastApp.currentVideo.getLocalPath());
            File subtitle = new File(temp.getParent(), FilenameUtils.removeExtension(temp.getName()) + ".ttml");
            String subtitleLink = ChromecastApp.localWebServer + Utils.preparedVideoPath(subtitle);
            logger.info("Subtitle link: " + subtitleLink);
            sendMessage(subtitleLink);
        }

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY) {
            Log.d("API", String.format("Open Directory result Uri : %s", data.getData()));
            editor.putString("sdroot", data.getData().toString());
            editor.commit();
            saveSubtitle21(data.getData());
        }
    }

    private void saveSubtitle21(Uri root) {
        if (Build.VERSION.SDK_INT > 21) {

            String path = ChromecastApp.currentVideo.getLocalPath();
            String name = "";
            if (ChromecastApp.mountedDir != null) {
                for (int i = 0; i < ChromecastApp.mountedDir.length; i++) {
                    File file = ChromecastApp.mountedDir[i];

                    if (path.startsWith(file.getAbsolutePath())) {
                        path = path.replace(file.getAbsolutePath(), "");
                    }
                }
            }

            File temp = new File(ChromecastApp.currentVideo.getLocalPath());
            name = FilenameUtils.removeExtension(temp.getName()) + ".ttml";//fSubtitle.getName();
            File subtitle = new File(temp.getParent(), name);
            path = path.replace(temp.getName(), "");

            String id = DocumentsContract.getTreeDocumentId(root);
            id = id + path;
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(root, id);

            Log.d("API", String.format("Access permission to Directory result Uri : %s", docUri));

            String storage = "content://com.android.externalstorage.documents/document/9016-4EF8%3Avideo%2FHouseMd.txt";
            Uri uri = null;
            try {
                uri = DocumentsContract.createDocument(this.getContentResolver(), docUri, "application/xml", name);
                writeFileContent(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.d("API", String.format("Dest File Uri: %s", uri));
            String subtitleLink = ChromecastApp.localWebServer + subtitle.getAbsolutePath().replace(" ", "%20");
            logger.info("Subtitle link: " + subtitleLink);
            sendMessage(subtitleLink);
        }
    }

    private void writeFileContent(Uri uri) {
        try {
            FileInputStream fis = new FileInputStream(fSubtitle.getAbsolutePath());
            byte[] contents = new byte[fis.available()];
            fis.read(contents, 0, contents.length);
            String asString = new String(contents, "UTF-8");
            byte[] newBytes = asString.getBytes("UTF-8");

            ParcelFileDescriptor
                    pfd =
                    this.getContentResolver().
                            openFileDescriptor(uri, "w");

            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());

            fileOutputStream.write(newBytes);
            fileOutputStream.close();
            pfd.close();
        } catch (Exception ex) {

        }


    }

    @Override
    public void onSubtitlesListFound(List<OSubtitle> list) {

        for (OSubtitle sub : list) {
            Log.d("Subtitle", sub.getMovieName());
            logger.debug("Subtitle: " + sub.getMovieName());
        }
    }

    @Override
    public void onSubtitleDownload(boolean b) {

    }

    @Override
    public void onError(int error) {

        Log.d("Subtitle", "Error: " + error);

    }

    public void showDialog() {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.SubtitleDialog);
        builder.setTitle("Subtitles");
        // Get the layout inflater

        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        View view = inflater.inflate(R.layout.search_dialog, null);
        builder.setView(view);
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        final EditText sv = (EditText) view.findViewById(R.id.search_bar);
        final ListView listResult = (ListView) view.findViewById(R.id.listResult);

        final View empty_view = view.findViewById(R.id.empty);
        listResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("Subtitle", "Choosed item");
                dialog1.cancel();
                subtitle = (OSubtitle) parent.getItemAtPosition(position);
                logger.info("Choosed subtitle: " + "[" + subtitle.getLanguageName() + "]" + " " + subtitle.getMovieName());


                if (mASub != null) {
                    //final File file = new File(ChromecastApp.currentVideo.getLocalPath());
                    final File file = getSubtitleStorageDir();
                    File zipFileName = new File(subtitle.getZipDownloadLink());
                    //File subtitleFile = new File(file.getParent(),zipFileName.getName());

                    File subtitleFile = new File(file.getAbsolutePath(), zipFileName.getName());
                    subtitlePath = subtitleFile.getAbsolutePath();
                    // mASub.downloadSubByIdToPath(subtitle.getIDSubtitle(),subtitlePath);


                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                Log.d("Subtitle", "Starting download to " + subtitlePath);
                                logger.info("Starting download to " + subtitlePath);
                                File subfile = downloadSubtitle(subtitle.getZipDownloadLink(), subtitlePath);
                                if (subfile.exists() == true) {
                                    logger.info("Downloaded success");
                                    Log.d("Subtitle", "Downloaded success");
                                    Log.d("Subtitle", "Starting unziped");
                                    logger.info("Starting unziped");
                                    List<File> files = unzipSubtitle(subfile, file.getAbsoluteFile());
                                    Log.d("Subtitle", "Unziped success");
                                    logger.info("Unziped success");
                                    // sendMessage("Unzip complete. Subtitles count: " +files.size());
                                    Log.d("Subtitle", "Starting convert");
                                    logger.info("Starting convert");
                                    List<File> converted = convertSubtitles(files);
                                    if (converted.size() > 0) {
                                        Log.d("Subtitle", "Converted success");
                                        logger.info("Converted success");
                                        File file1 = converted.get(0);
                                        File temp = new File(ChromecastApp.currentVideo.getLocalPath());
                                        File destFile = new File(temp.getParent(), FilenameUtils.removeExtension(temp.getName()) + ".ttml");
                                        try {
                                            destFile.createNewFile();
                                            FileUtils.copyFile(file1, destFile);
                                            String subtitleLink = ChromecastApp.localWebServer + destFile.getAbsolutePath().replace(" ", "%20");
                                            logger.info("Subtitle link: " + subtitleLink);
                                            sendMessage(subtitleLink);
                                        } catch (IOException e) {
                                            logger.error("File copy error", e);
                                            fSubtitle = file1;

                                            if (Build.VERSION.SDK_INT >= 21) {
                                                String uri = prefs.getString("sdroot", "");

                                                if (TextUtils.isEmpty(uri)) {
                                                    handler2.sendEmptyMessage(0);
                                                } else {
                                                    handler2.sendEmptyMessage(2);

                                                }
                                            } else if (Build.VERSION.SDK_INT >= 19) {
                                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                                intent.setType("application/xml");
                                                intent.putExtra(Intent.EXTRA_TITLE, destFile.getName());

                                                startActivityForResult(intent, 123);
                                            } else {
                                                handler2.sendEmptyMessage(1);
                                            }
                                        }
                                    }
                                } else {

                                }
                                //   sendMessage("Convert complete. Subtitles count: " + converted.size());

                            } catch (Exception ex) {
                                Log.e("Subtitle", ex.getMessage() + " " + ex.getStackTrace());
                                logger.error("Subtitle error: ", ex);
                            }
                        }
                    });
                    thread.start();
                    Log.d("Subtitle", "Download URL: " + subtitle.getSubDownloadLink());
                    logger.info("Download URL: " + subtitle.getSubDownloadLink());
                    Log.d("Subtitle", "URL: " + subtitle.getSubtitlesLink());
                    logger.info("URL: " + subtitle.getSubtitlesLink());
                    Log.d("Subtitle", "ZIP URL: " + subtitle.getZipDownloadLink());
                    logger.info("ZIP URL: " + subtitle.getZipDownloadLink());
                }
            }
        });

        if (sv != null && ChromecastApp.currentVideo != null) {
            String fileNameWithOutExt = FilenameUtils.removeExtension(new File(ChromecastApp.currentVideo.getLocalPath()).getName());
            sv.setText(fileNameWithOutExt);
        }

        sv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    sv.clearFocus();
                    progressBar.setVisibility(View.VISIBLE);
                    try {
                        mASub = new AsyncSubtitles(MyExpandedControllerActivity.this, new AsyncSubtitles.SubtitlesInterface() {
                            @Override
                            public void onSubtitlesListFound(List<OSubtitle> list) {
                                Log.d("Subtitle", "Finded " + list.size());
                                logger.info("Finded subtitles: " + list.size());
                                progressBar.setVisibility(View.INVISIBLE);
                                SubtitleAdapter adapter = new SubtitleAdapter(MyExpandedControllerActivity.this, list);
                                if (list.size() > 0) {
                                    if (listResult != null) {
                                        listResult.setAdapter(adapter);
                                    }
                                } else {
                                    if (listResult != null) {
                                        listResult.setEmptyView(empty_view);
                                    }
                                }
                            }

                            @Override
                            public void onSubtitleDownload(boolean b) {
                                if (b == true) {
                                    Log.d("Subtitle", "Subtitle format: " + subtitle.getSubFormat());
                                    logger.info("Subtitle format: " + subtitle.getSubFormat());
                                    Toast.makeText(MyExpandedControllerActivity.this, "Download success", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MyExpandedControllerActivity.this, "Download error", Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onError(int error) {
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        });
                        mASub.setLanguagesArray(new String[]{"en"});
                        ORequest req = new ORequest("", sv.getText().toString(), null, new String[]{"spa", "eng"});

                        mASub.setNeededParamsToSearch(req);
                        logger.error("Find Subtitle query: " + sv.getText().toString());
                        mASub.getPossibleSubtitle();
                    } catch (Exception e) {
                        logger.error("Find Subtitle error: ", e);
                        e.printStackTrace();
                    }

                    return true;
                }
                return false;
            }
        });

        ImageView searchImage = (ImageView) view.findViewById(R.id.search_icon);
        searchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                try {
                    mASub = new AsyncSubtitles(MyExpandedControllerActivity.this, new AsyncSubtitles.SubtitlesInterface() {
                        @Override
                        public void onSubtitlesListFound(List<OSubtitle> list) {
                            Log.d("Subtitle", "Finded " + list.size());
                            logger.info("Finded subtitles: " + list.size());
                            progressBar.setVisibility(View.INVISIBLE);
                            if (list.size() > 0) {
                                SubtitleAdapter adapter = new SubtitleAdapter(MyExpandedControllerActivity.this, list);
                                if (listResult != null) {
                                    listResult.setAdapter(adapter);
                                }
                            } else {
                                if (listResult != null) {
                                    listResult.setEmptyView(empty_view);
                                }
                            }

                        }

                        @Override
                        public void onSubtitleDownload(boolean b) {

                        }

                        @Override
                        public void onError(int error) {
                            Log.d("Subtitle", "Error: " + error);
                            logger.error("Finded error: " + error);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    mASub.setLanguagesArray(new String[]{"en"});
                    ORequest req = new ORequest("", sv.getText().toString(), null, new String[]{"spa", "eng"});
                    mASub.setNeededParamsToSearch(req);
                    logger.error("Find Subtitle query: " + sv.getText().toString());
                    mASub.getPossibleSubtitle();
                } catch (Exception e) {
                    logger.error("Find Subtitle error: ", e);
                    e.printStackTrace();
                }
            }
        });

        dialog1 = builder.create();
        dialog1.setCanceledOnTouchOutside(true);
        dialog1.setCancelable(true);
        dialog1.show();
    }

    public File getSubtitleStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "subtitles");
        if (!file.mkdirs()) {
            Log.e("Subtitle", "Directory not created");
            logger.error("Directory not created");
        }
        return file;
    }

    private File downloadSubtitle(String url, String path) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url)
                .build();
        Response response = client.newCall(request).execute();

        InputStream in = response.body().byteStream();

        File file = new File(path);
        if (file.exists() == true) {
            file.delete();
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        int read;
        byte[] bytes = new byte[1024];

        while ((read = in.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }

        response.body().close();

        return file;
    }

    private void sendMessage(String text) {
        // Message msg = handler.obtainMessage();
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("text", text);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public List<File> unzipSubtitle(File zipFile, File targetDirectory) throws IOException {

        //    ZipFile zip =new ZipFile(zipFile);
        List<File> subtitles = new ArrayList<File>();
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            byte[] buffer = new byte[2048];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                String ext = FilenameUtils.getExtension(ze.getName());
                if (ext.equalsIgnoreCase("srt") || ext.equalsIgnoreCase("vtt")) {
                    FileOutputStream fout = new FileOutputStream(file);
                    BufferedOutputStream bufout = new BufferedOutputStream(fout);
                    int count;
                    try {
                        while ((count = zis.read(buffer)) != -1)
                            bufout.write(buffer, 0, count);
                    } finally {
                        subtitles.add(file);
                        bufout.close();
                        fout.close();
                    }
                }

            }
        } finally {
            zis.close();
        }


        return subtitles;
    }

    public List<File> convertSubtitles(List<File> files) {
        List<File> converted = new ArrayList<>();
        if (files.size() > 0) {
            for (File st : files) {
                try {
                    String enc = Utils.getEncoding(st);
                    Log.d("Encoding", "Subtitle encoding: " + enc);

                    File utffile = new File(st.getParent(), FilenameUtils.removeExtension(st.getName()) + "_utf8." + FilenameUtils.getExtension(st.getName()));
                    if (utffile.exists() == true) {
                        utffile.delete();
                    }
                    utffile.createNewFile();
                    convertToUTF8(st, enc, utffile, "UTF-8");
                    TimedTextFileFormat ttff = new FormatSRT();

                    InputStream is = new FileInputStream(utffile);
                    TimedTextObject tto = ttff.parseFile(utffile.getName(), is);
                    String newFileName = FilenameUtils.removeExtension(utffile.getName()) + ".ttml";

                    File outputPath = new File(st.getParent(), newFileName);
                    writeFileTxt(outputPath.getAbsolutePath(), tto.toTTML(), "UTF-8");
                    converted.add(outputPath);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return converted;
    }

    public void convertToUTF8(File source, String srcEncoding, File target, String tgtEncoding) {

        /*try {
            FileInputStream fileInput = new FileInputStream(source);

            BufferedReader br = new BufferedReader(new InputStreamReader(fileInput, srcEncoding));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), tgtEncoding));

            char[] buffer = new char[2048];
            int read;
            while ((read = br.read(buffer,0,buffer.length)) != -1)
                bw.write(buffer, 0, read);
        }
        catch (Exception ex){}*/
        try {
            FileInputStream fis = new FileInputStream(source.getAbsolutePath());
            byte[] contents = new byte[fis.available()];
            fis.read(contents, 0, contents.length);
            String asString = new String(contents, srcEncoding);
            byte[] newBytes = asString.getBytes(tgtEncoding);
            FileOutputStream fos = new FileOutputStream(target.getAbsolutePath());
            fos.write(newBytes);
            fos.close();
        } catch (Exception ex) {

        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MyExpandedControllerActivity> mActivity;

        public MyHandler(MyExpandedControllerActivity activity) {
            mActivity = new WeakReference<MyExpandedControllerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MyExpandedControllerActivity activity = mActivity.get();
            if (activity != null) {
                Bundle res = msg.getData();
                final String text = res.getString("text");
                Log.d("Subtitle", "Link:" + text);
                activity.logger.info("Subtitle link: " + text);
                final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(activity, R.style.SubtitleDialog);
                builder.setMessage(activity.getResources().getString(R.string.restartvideo))
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {


                                if (activity.remoteMediaClient == null) {

                                    Log.d("ChromecastApp", "Remote media client is null");
                                    activity.logger.error("Remote media client is null");
                                    return;
                                }
                                MediaInfo info = null;
                                MediaQueueItem item = activity.remoteMediaClient.getCurrentItem();
                                if (item == null) {
                                    item = activity.currentItem;
                                }
                                if (item != null)
                                    info = activity.remoteMediaClient.getCurrentItem().getMedia();
                                else
                                    info = activity.remoteMediaClient.getMediaInfo();
                                MediaTrack subtitleTrack = new MediaTrack.Builder(1 /* ID */,

                                        MediaTrack.TYPE_TEXT)
                                        .setName("Subtitle")
                                        .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                                        .setContentId(text)
                                        .setLanguage(activity.subtitle.getISO639())
                                        .build();

                                for (int i = info.getMediaTracks().size() - 1; i >= 0; i--) {
                                    MediaTrack track = info.getMediaTracks().get(i);
                                    if (track.getType() == MediaTrack.TYPE_TEXT && track.getSubtype() == MediaTrack.SUBTYPE_SUBTITLES) {
                                        info.getMediaTracks().remove(track);
                                    }
                                }
                                info.getMediaTracks().add(subtitleTrack);


                                MediaQueueItem queueItem = new MediaQueueItem.Builder(info)
                                        .setAutoplay(true)
                                        .setPreloadTime(10)
                                        .build();


                                activity.remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
                                    @Override
                                    public void onStatusUpdated() {
                                        Log.d("Subtitle", "Enable subtitle");
                                        if (activity.remoteMediaClient.getMediaStatus().getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING) {
                                            activity.logger.info("Start subtitle track");
                                            activity.remoteMediaClient.setActiveMediaTracks(new long[]{1});
                                            activity.remoteMediaClient.removeListener(this);
                                        }
                                    }

                                    @Override
                                    public void onMetadataUpdated() {

                                    }

                                    @Override
                                    public void onQueueStatusUpdated() {

                                    }

                                    @Override
                                    public void onPreloadStatusUpdated() {

                                    }

                                    @Override
                                    public void onSendingRemoteMediaRequest() {

                                    }

                                    @Override
                                    public void onAdBreakStatusUpdated() {

                                    }
                                });
                                if (item != null) {
                                    int id = item.getItemId();
                                    activity.remoteMediaClient.queueInsertAndPlayItem(queueItem, id, null);
                                    activity.logger.info("Start video with subtitle");
                                    // remoteMediaClient.setActiveMediaTracks(new long[]{1});
                                    activity.remoteMediaClient.queueRemoveItem(id, null);
                                } else {
                                    activity.remoteMediaClient.load(info);
                                }
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                AppCompatDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.setCancelable(true);
                dialog.show();


//            remoteMediaClient(info,true,0,new long[]{subtitle.getId()},null);
            }
        }
    }


}
