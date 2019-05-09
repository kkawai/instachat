package com.instachat.android;

import android.app.Activity;
import android.support.multidex.MultiDexApplication;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.instachat.android.di.component.DaggerAppComponent;
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
    public static boolean isSavedDeviceId;

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

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return activityDispatchingAndroidInjector;
    }
}
