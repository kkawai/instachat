package com.instachat.android.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.instachat.android.TheApp;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.api.BasicResponse;
import com.instachat.android.util.DeviceUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.SimpleRxWrapper;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.io.IOException;

import javax.inject.Inject;

import io.reactivex.functions.Consumer;

/**
 * The important assumption is that GCM is already supported on this device
 * AND that Google Play Services is installed on the device.
 *
 * @author kkawai
 */
public final class GCMRegistrationManager {

    private static final String TAG = "GCMRegistrationManager";

    private static final String PROPERTY_APP_VERSION = "appVersion";
    public static final String PROPERTY_REG_ID = "registration_id";
    private String regId;
    private final Context context;
    private final DataManager dataManager;
    private final SchedulerProvider schedulerProvider;

    @Inject
    public GCMRegistrationManager(@NonNull Context context,
                                  @NonNull DataManager dataManager,
                                  @NonNull SchedulerProvider schedulerProvider) {
        this.context = context;
        this.dataManager = dataManager;
        this.schedulerProvider = schedulerProvider;
    }

    public void registerGCM() {

        regId = getRegistrationId(context);
        MLog.d(TAG,"kevinFirbaseMessaging registerGCM regId: " + regId);

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {

                if (task.isSuccessful()) {
                    MLog.d(TAG, "kevinFirbaseMessaging registerGCM getInstanceId.getToken(): " + task.getResult().getToken());
                    regId = task.getResult().getToken();
                    registerInBackground();
                }
            }

        });

    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {

        SimpleRxWrapper.executeInWorkerThread(new Runnable() {

            @Override
            public void run() {
                try {


                    MLog.i(TAG, "Device registered, registration ID=" + regId);

                    // You should send the registration ID to your server over
                    // HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your
                    // app.
                    // The request to your server should be authenticated if
                    // your app
                    // is using accounts.
                    dataManager.registerGCM(UserPreferences.getInstance().getUserId().toString(),
                                            DeviceUtil.getAndroidId(context),
                                            regId).subscribe(new Consumer<BasicResponse>() {
                        @Override
                        public void accept(BasicResponse basicResponse) throws Exception {
                            MLog.d(TAG,"registerInBackground() response.status: "+basicResponse.status);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            MLog.d(TAG,"registerInBackground() failed: "+throwable);
                        }
                    });

                    saveRegistrationId(TheApp.getInstance(), regId);

                } catch (final Exception e) {
                    MLog.e(TAG, "debugx", e);
                }
            }

        });
    }

    public static void saveRegistrationId(Context context, final String regId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int appVersion = getAppVersion(context);
        MLog.i(TAG, "Saving regId on app version " + appVersion);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to registerIfNecessary.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    public static String getRegistrationId(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            MLog.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must cleanup the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        final int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        final int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            MLog.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    public static void removeRegistrationId(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int appVersion = getAppVersion(context);
        MLog.i(TAG, "Removing regId on app version " + appVersion);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PROPERTY_REG_ID);
        editor.remove(PROPERTY_APP_VERSION);
        editor.apply();

        /*
        try {
            FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FirebaseAuth.getInstance().signOut();
        */
    }

    private static int getAppVersion(final Context context) {

        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (final Exception e) {
            return 1;
        }
    }
}
