package com.cloudrail.fileviewer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudrail.si.exceptions.AuthenticationException;
import com.cloudrail.si.types.CloudMetaData;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.SpaceAllocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 * Use the {@link Files#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Files extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SERVICE = "service";
    private static final int FILE_SELECT = 0;
    private final static String[] DISPLAYABLES = new String[] {"wav", "mpeg", "mp4",".webm","mkv","m3u8","mpd"};

    private int currentService;
    private ListView list = null;
    private String currentPath;
    private View selectedItem;
    private ProgressBar spinner;
    private TextView textView_noFiles;
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    private Context context;
    private Activity activity = null;
    private String gDriveLink="https://drive.google.com/uc?export=download&id=";
    private String dropBoxDriveLink="https://dl.dropboxusercontent.com/s/";
    private String oneDriveLink="https://drive.google.com/uc?export=download&id=";

    public Files() {
        // Required empty public constructor
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT: {
                if(resultCode == Activity.RESULT_OK) {
                    final Uri uri = data.getData();
                    final String name;
                    String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
                    Cursor metaCursor = getOwnActivity().getContentResolver().query(uri, projection, null, null, null);

                    if(metaCursor == null) {
                        throw new RuntimeException("Could not read file name.");
                    }

                    try {
                        metaCursor.moveToFirst();
                        name = metaCursor.getString(0);
                    } finally {
                        metaCursor.close();
                    }

                    this.uploadItem(name, uri);
                }
                break;
            }
            default: super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Files.
     */
    public static Files newInstance(int service) {
        Files fragment = new Files();
        Bundle args = new Bundle();
        args.putInt(ARG_SERVICE, service);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentService = getArguments().getInt(ARG_SERVICE);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_files, container, false);
        sp = getActivity().getPreferences(Context.MODE_PRIVATE);

        this.list = (ListView) v.findViewById(R.id.listView);
        this.textView_noFiles = (TextView) v.findViewById(R.id.textView_noFiles);

        this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = view;
                startSpinner();
                RelativeLayout ll = (RelativeLayout) view;
                TextView tv = (TextView) ll.findViewById(R.id.list_item);
                final String name = (String) tv.getText();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String next = currentPath;
                        if (!currentPath.equals("/")) {
                            next += "/";
                        }
                        next += name;

                        CloudMetaData info = getService().getMetadata(next);
                        if (info.getFolder()) {
                            setNewPath(next);
                        } else {
                            startCasting();
                        }
                    }
                }).start();
            }
        });



        this.spinner = (ProgressBar) v.findViewById(R.id.spinner);
        this.spinner.setVisibility(View.GONE);
        this.currentPath = "/";
        this.updateList();

        return v;
    }

    private String getSizeString(Long size) {
        String units[] = {"Bytes", "kB", "MB", "GB", "TB"};
        String unit = units[0];
        for (int i = 1; i < 5; i++) {
            if (size > 1024) {
                size /= 1024;
                unit = units[i];
            }
        }

        return size + unit;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        this.context = context;
        this.activity = context;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_bar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.action_refresh){

            this.updateList();
        }else  if (item.getItemId()==R.id.action_search){

            getOwnActivity().onSearchRequested();
        }

        return true;
    }

    private void clickCreateFolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter Folder Name");

        // Set up the input
        final EditText input = new EditText(context);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createFolder(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void createFolder(String name) {
        String next = currentPath;
        if(!currentPath.equals("/")) {
            next += "/";
        }
        next += name;
        final String finalNext = next;
        new Thread(new Runnable() {
            @Override
            public void run() {
                getService().createFolder(finalNext);
                List<CloudMetaData> items = getService().getChildren(currentPath);
                final List<CloudMetaData> files = sortList(items);

                getOwnActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (files.size()!=0) {
                            CloudMetadataAdapter listAdapter = new CloudMetadataAdapter(context, R.layout.list_item, files);
                            list.setAdapter(listAdapter);
                            textView_noFiles.setVisibility(View.GONE);
                            list.setVisibility(View.VISIBLE);
                        }else{
                            textView_noFiles.setVisibility(View.VISIBLE);
                            list.setVisibility(View.GONE);
                        }
                        stopSpinner();
                    }
                });
            }
        }).start();
    }

    public boolean onBackPressed() {
        if(this.currentPath.equals("/")) {
            return true;
        } else {
            int pos = this.currentPath.lastIndexOf("/");
            String newPath = "/";
            if(pos != 0) {
                newPath = this.currentPath.substring(0, pos);
            }
            this.setNewPath(newPath);
        }
        return false;
    }

    private CloudStorage getService() {
        return Services.getInstance().getService(this.currentService);
    }

    private void updateList() {
        this.startSpinner();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    List<CloudMetaData> items = getService().getChildren(currentPath);
                    final List<CloudMetaData> files = sortList(items);

                    getOwnActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (files.size()!=0) {
                                CloudMetadataAdapter listAdapter = new CloudMetadataAdapter(context, R.layout.list_item, files);
                                list.setAdapter(listAdapter);
                                spe = sp.edit();
                                spe.putInt("service", currentService);
                                spe.apply();
                                textView_noFiles.setVisibility(View.GONE);
                                list.setVisibility(View.VISIBLE);
                            }else{
                                textView_noFiles.setVisibility(View.VISIBLE);
                                list.setVisibility(View.GONE);
                            }

                            stopSpinner();

                        }
                    });
                }catch (AuthenticationException e){
                    getOwnActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(),"Authentication failed",Toast.LENGTH_LONG).show();
                            stopSpinner();
                            spe = sp.edit();
                            spe.putInt("service", 0);
                            spe.apply();
                        }
                    });
                }
            }
        }).start();
    }

    public void search(final String search) {
        this.startSpinner();
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<CloudMetaData> items = getService().search(search);
                final List<CloudMetaData> files = sortList(items);

                getOwnActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (files.size()!=0) {
                            CloudMetadataAdapter listAdapter = new CloudMetadataAdapter(context, R.layout.list_item, files);
                            list.setAdapter(listAdapter);
                            textView_noFiles.setVisibility(View.GONE);
                            list.setVisibility(View.VISIBLE);
                        }else{
                            textView_noFiles.setVisibility(View.VISIBLE);
                            list.setVisibility(View.GONE);
                        }

                        stopSpinner();
                    }
                });


            }
        }).start();
    }

    private void setNewPath(String path) {
        this.currentPath = path;
        this.updateList();
    }

    private void pipe(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int size = is.read(buffer);
        while(size > -1) {
            os.write(buffer, 0, size);
            size = is.read(buffer);
        }
    }

    private boolean isDisplayable(String ext) {
        for(String a : DISPLAYABLES) {
            if(a.equals(ext)) return true;
        }
        return false;
    }

    private String getMimeType(String name) {
        int pos = name.lastIndexOf(".");

        if(pos == -1) return null;

        return name.substring(pos + 1);
    }

    private List<CloudMetaData> sortList(List<CloudMetaData> list) {
        List<CloudMetaData> folders = new ArrayList<>();
        List<CloudMetaData> files = new ArrayList<>();

        for(CloudMetaData cmd : list) {
            if(cmd == null) continue;

            if(cmd.getFolder()) {
                folders.add(cmd);
            } else {
                String ext = getMimeType(cmd.getName());
                if (isDisplayable(ext))
                    files.add(cmd);
            }
        }

        folders.addAll(files);
        return folders;
    }

    private void removeItem() {
        this.startSpinner();
        TextView tv = (TextView) this.selectedItem.findViewById(R.id.list_item);
        final String name = (String) tv.getText();
        CloudMetaData cloudMetaData = new CloudMetaData();
        cloudMetaData.setName(name);
        ArrayAdapter<CloudMetaData> adapter = (ArrayAdapter<CloudMetaData>) this.list.getAdapter();
        adapter.remove(cloudMetaData);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String next = currentPath;
                if(!currentPath.equals("/")) {
                    next += "/";
                }
                next += name;
                getService().delete(next);
                updateList();
            }
        }).start();
    }

    private void copyItem() {

    }

    private void moveItem() {

    }
    public void startCasting(){
        this.startSpinner();
        TextView tv = (TextView) this.selectedItem.findViewById(R.id.list_item);
        final String name = (String) tv.getText();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String next = currentPath;
                if(!currentPath.equals("/")) {
                    next += "/";
                }
                next += name;
                final String shareLink = getService().createShareLink(next);

                getOwnActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String downloadLink="";
                        Log.v("wasim","share link:"+shareLink);
                        if (currentService==1){
                            downloadLink=shareLink.replace("https://www.dropbox.com/s/",dropBoxDriveLink);
                        }else if (currentService==2){
                            int indexId=shareLink.indexOf("/d/")+3;
                            String file_id=shareLink.substring(indexId,shareLink.indexOf("/",indexId));
                            downloadLink=gDriveLink+file_id;
                        }else if (currentService==3){
                            String fileID=shareLink.substring(shareLink.indexOf("s!"),shareLink.length());
                            downloadLink="https://api.onedrive.com/v1.0/shares/"+fileID+"/root/content";
                        }
                        Intent data = new Intent();
                        data.putExtra("link",downloadLink);
                        if (getActivity().getParent() == null) {
                            getActivity().setResult(Activity.RESULT_OK, data);
                        } else {
                            getActivity().getParent().setResult(Activity.RESULT_OK, data);
                        }
                        getActivity().finish();
                    }
                });
                stopSpinner();
            }
        }).start();
    }
    private void createShareLink() {
        this.startSpinner();
        TextView tv = (TextView) this.selectedItem.findViewById(R.id.list_item);
        final String name = (String) tv.getText();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String next = currentPath;
                if(!currentPath.equals("/")) {
                    next += "/";
                }
                next += name;
                final String shareLink = getService().createShareLink(next);

                getOwnActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Sharable Link", shareLink);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, "Copied link to clipboard\n" + shareLink, Toast.LENGTH_LONG).show();
                    }
                });
                stopSpinner();
            }
        }).start();
    }

    private void uploadItem(final String name, final Uri uri) {
        startSpinner();
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fs = null;
                long size = -1;
                try {
                    fs = getOwnActivity().getContentResolver().openInputStream(uri);
                    size = getOwnActivity().getContentResolver().openAssetFileDescriptor(uri, "r").getLength();
                } catch (Exception e) {
                    stopSpinner();
                    getOwnActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Unable to access file!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                String next = currentPath;
                if(!currentPath.equals("/")) {
                    next += "/";
                }
                next += name;
                getService().upload(next, fs, size, true);

                getOwnActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateList();
                    }
                });
            }
        }).start();
    }

    private void startSpinner() {
        getOwnActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void stopSpinner() {
        getOwnActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.GONE);
            }
        });
    }

    private Activity getOwnActivity() {
        if(this.activity == null) {
            return this.getActivity();
        } else {
            return this.activity;
        }
    }
}
