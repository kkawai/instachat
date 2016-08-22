package com.initech;

import android.app.Application;
import android.content.res.Configuration;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.ath.fuel.FuelInjector;
import com.ath.fuel.FuelModule;
import com.initech.util.HttpMessage;
import com.initech.util.ThreadWrapper;

/**
 * Created by kevin on 7/23/2016.
 */
public class MyApp extends Application {

    private static final String TAG = "MyApp";
    private static MyApp sInstance;
    private static boolean isAdmSupported;
    private RequestQueue mRequestQueue;
    public static double lat;
    public static double lon;

    @Override
    public void onCreate() {
        super.onCreate();
        FuelInjector.setDebug(false);
        FuelInjector.initializeModule(new FuelModule(this){});
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

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
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
