package com.initech;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.ath.fuel.FuelInjector;
import com.ath.fuel.FuelModule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.initech.util.HttpMessage;
import com.initech.util.MLog;
import com.initech.util.ThreadWrapper;

/**
 * Created by kevin on 7/23/2016.
 */
public class MyApp extends Application {

    private static final String TAG = "MyApp";
    private static MyApp sInstance;
    public static boolean isGcmSupported;
    public static boolean isAdmSupported;
    private RequestQueue mRequestQueue;
    public static double lat;
    public static double lon;

    @Override
    public void onCreate() {
        super.onCreate();
        FuelInjector.setDebug(false);
        FuelInjector.initializeModule(new FuelModule(this) {
        });
        sInstance = this;
        ThreadWrapper.init();
        HttpMessage.initializeSSL();
        initAdm();
        initGcm();
        initVolley();
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

    private void initGcm() {
        isGcmSupported = false;
        try {
            final int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
            isGcmSupported = resultCode == ConnectionResult.SUCCESS;
        } catch (final Throwable t) {
            MLog.e(TAG, "Device does not support GCM", t);
        }
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    private void initVolley() {
        /*
         *  init requestQueue
		 */
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
    }
}
