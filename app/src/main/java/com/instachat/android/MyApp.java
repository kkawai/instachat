package com.instachat.android;

import android.app.Application;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.ath.fuel.FuelInjector;
import com.ath.fuel.FuelModule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.instachat.android.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.util.BitmapLruCache;
import com.instachat.android.util.HttpMessage;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ThreadWrapper;

/**
 * Created by kevin on 7/23/2016.
 */
public class MyApp extends MultiDexApplication {

    private static final String TAG = "MyApp";
    private static MyApp sInstance;
    public static boolean isGcmSupported;
    public static boolean isAdmSupported;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    public static double lat;
    public static double lon;

    private MessagesRecyclerAdapterHelper mMessagesRecyclerAdapterHelper = new MessagesRecyclerAdapterHelper();

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
        mMessagesRecyclerAdapterHelper = new MessagesRecyclerAdapterHelper();
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

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    private void initVolley() {
        /*
         *  init requestQueue
		 */
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int maxCacheSize = maxMemory / 8;
        this.mImageLoader = new ImageLoader(mRequestQueue, new BitmapLruCache(
                maxCacheSize));

    }

    public MessagesRecyclerAdapterHelper getMap() {
        return mMessagesRecyclerAdapterHelper;
    }
}
