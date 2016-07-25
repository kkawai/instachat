package com.initech;

import android.app.Application;
import android.content.res.Configuration;

import com.initech.util.ThreadWrapper;

/**
 * Created by kevin on 7/23/2016.
 */
public class MyApp extends Application {

    private static MyApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        ThreadWrapper.init();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static MyApp getInstance() {
        return sInstance;
    }
}
