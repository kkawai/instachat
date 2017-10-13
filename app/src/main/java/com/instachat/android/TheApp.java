package com.instachat.android;

import android.app.Activity;
import android.support.multidex.MultiDexApplication;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.di.component.DaggerAppComponent;
import com.instachat.android.util.BitmapLruCache;
import com.instachat.android.util.HttpMessage;
import com.instachat.android.util.MLog;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;

public class TheApp extends MultiDexApplication implements HasActivityInjector {

    @Inject
    DispatchingAndroidInjector<Activity> activityDispatchingAndroidInjector;

    private static final String TAG = "TheApp";
    private static TheApp sInstance;
    public static boolean isGcmSupported;
    public static boolean isAdmSupported;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private MessagesRecyclerAdapterHelper mMessagesRecyclerAdapterHelper = new MessagesRecyclerAdapterHelper();

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerAppComponent.builder()
                .application(this)
                .build()
                .inject(this);
        sInstance = this;
        HttpMessage.initializeSSL();
        initAdm();
        initGcm();
        initVolley();
        mMessagesRecyclerAdapterHelper = new MessagesRecyclerAdapterHelper();
    }

    public static TheApp getInstance() {
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

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return activityDispatchingAndroidInjector;
    }
}
