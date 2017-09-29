package com.oxycast.chromecastapp.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastContext;
import com.oxycast.chromecastapp.media.Video;

import org.adblockplus.libadblockplus.android.settings.AdblockHelper;

import java.io.File;

/**
 * Created by sergey on 24.02.17.
 */

public class ChromecastApp extends Application {
    public static final String base64encodedStr= "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArLHYb/wuoei1ljvz95NTRGnAHXM4iSsNAkRegC0u3I+JxBjkTR86MTz0n4tDbbJWBvkfuLCCTzOpE/JeDCAaudfRCH5x1zIaovTKgrgHJtvTMOBtE8F80SQencyDTbnEelaeZF8xg5iu0s/H9kJ8Fms4JvnW2wQnlknbqKS5FgmdIvH1G20RZZ59fuqjZlAafF0GAIAwNMDnO2bNxmzIAheKy0cMhR80QKdtdeQtyFwy6dHNLl7YQ7aLDVgmtQBs58Waqmtg8Eyy/CFlAbIncNpcsnJBV7Znfy7ZQOvXoSc8O0Z/jHBC+zuLw9n1qtkRuUO/rslKFOsYLY2VRtORzQIDAQAB";

    private class Insts
    {
        public ChromecastApp App;
        public Insts(ChromecastApp app)
        {
            App = app;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        AdblockHelper.get().init(this, false, AdblockHelper.PREFERENCE_NAME);
    }

    private static Insts _Instance;

    public ChromecastApp(){
        _Instance = new Insts(this);
    }

    public static ChromecastApp Instance()
    {
        return _Instance.App;
    }

    public  CastContext mCastContext;

    public static Video currentVideo;
    public static CastDevice castDevice;

    public static String localWebServer;

    public static File[] mountedDir;

    /**
     * Returns an instance of {@link ChromecastApp} attached to the passed activity.
     */
    public static ChromecastApp get(Activity activity) {
        return (ChromecastApp) activity.getApplication();
    }


}
