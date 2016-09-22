package com.instachat.android.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.instachat.android.Constants;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ThreadWrapper;

/**
 * The important assumption is that GCM is already supported on this device
 * AND that Google Play Services is installed on the device.
 *
 * @author kkawai
 */
public final class GCMRegistrationManager {

    private static final String TAG = GCMRegistrationManager.class.getSimpleName();

    private static final String PROPERTY_APP_VERSION = "appVersion";
    public static final String PROPERTY_REG_ID = "registration_id";
    private GoogleCloudMessaging mGcm;
    private String mRegId;
    private Context mContext;

    public GCMRegistrationManager(final Context context) {
        mContext = context;
    }

    public void registerGCM() {

        mGcm = GoogleCloudMessaging.getInstance(mContext);
        mRegId = getRegistrationId(mContext);

        if (mRegId.isEmpty()) {
            registerInBackground();
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {

        ThreadWrapper.executeInWorkerThread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (mGcm == null) {
                        mGcm = GoogleCloudMessaging.getInstance(mContext);
                    }
                    mRegId = InstanceID.getInstance(mContext).getToken(Constants.GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE);

                    MLog.i(TAG, "debugx Device registered, registration ID=" + mRegId);

                    // You should send the registration ID to your server over
                    // HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your
                    // app.
                    // The request to your server should be authenticated if
                    // your app
                    // is using accounts.
                    NetworkApi.gcmreg(mContext, mRegId);

                    // For this demo: we don't need to send it because the
                    // device
                    // will send upstream messages to a server that echo back
                    // the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to registerIfNecessary again.
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                    final int appVersion = getAppVersion(mContext);
                    MLog.i(TAG, "debugx Saving regId on app version " + appVersion);
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(PROPERTY_REG_ID, mRegId);
                    editor.putInt(PROPERTY_APP_VERSION, appVersion);
                    editor.commit();

                } catch (final Exception e) {
                    MLog.e(TAG, "debugx", e);
                }
            }

        });
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
        // Check if app was updated; if so, it must clear the registration ID
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
        editor.commit();
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
