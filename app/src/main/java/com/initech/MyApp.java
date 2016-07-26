package com.initech;

import android.app.Application;
import android.content.res.Configuration;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.initech.util.HttpMessage;
import com.initech.util.MLog;
import com.initech.util.ThreadWrapper;

/**
 * Created by kevin on 7/23/2016.
 */
public class MyApp extends Application {

    private static final String TAG = "MyApp";
    private static MyApp sInstance;
    private static boolean isAdmSupported;
    private RequestQueue mRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        ThreadWrapper.init();
        HttpMessage.initializeSSL();
        initAdm();
        initVolley();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static MyApp getInstance() {
        return sInstance;
    }

    private void initAdm() {
        try {
            Class.forName("com.amazon.device.messaging.ADM");
            isAdmSupported = true;
        } catch (final ClassNotFoundException e) {
            isAdmSupported = false;
        }
    }

    public boolean isAdmSupported() {
        return isAdmSupported;
    }

    private void initVolley() {
		/*
		 *  init requestQueue
		 */
        this.mRequestQueue = Volley.newRequestQueue(this
                .getApplicationContext());
    }
}
