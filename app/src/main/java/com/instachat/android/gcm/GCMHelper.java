package com.instachat.android.gcm;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.instachat.android.Constants;
import com.instachat.android.TheApp;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.SimpleRxWrapper;
import com.instachat.android.util.StringUtil;

import javax.inject.Inject;

/**
 * Invoke registerIfNecessary() method AFTER user authenticates with IG
 * and we have stored the USER
 * <p>
 * Supports both Google Cloud Messaging and Amazon Device Messaging
 *
 * @author kkawai
 */

public class GCMHelper {

    private static final String TAG = "GCMHelper";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final NetworkApi networkApi;
    private final GCMRegistrationManager gcmRegistrationManager;
    private final Context context;

    @Inject
    public GCMHelper(@NonNull Context context,
                     @NonNull NetworkApi networkApi,
                     @NonNull GCMRegistrationManager gcmRegistrationManager) {
        this.context = context;
        this.networkApi = networkApi;
        this.gcmRegistrationManager = gcmRegistrationManager;
    }

    private void registerIfNecessary(final Context context) {

        MLog.i(TAG, "registerIfNecessary()");

        if (TheApp.isAdmSupported && Constants.IS_FOR_AMAZON_ONLY) {

            try {
                MLog.i(TAG, "about to verify if adm is really supported");
                ADMManifest.checkManifestAuthoredProperly(context);
                MLog.i(TAG, "after check adm");
                final ADM adm = new ADM(context);
                MLog.i(TAG, "after instantiate adm");
                if (StringUtil.isEmpty(adm.getRegistrationId())) {
                    // startRegister() is asynchronous; your app is notified via
                    // the
                    // onRegistered() callback when the registration ID is
                    // available.
                    MLog.i(TAG, "registerIfNecessary adm");
                    adm.startRegister();
                }

            } catch (final Throwable t) {
                MLog.e(TAG, "Device does not support ADM", t);
                TheApp.isAdmSupported = false;
            }
            return;
        }

        if (TheApp.isGcmSupported && !Constants.IS_FOR_AMAZON_ONLY) {
            MLog.i(TAG, "debugx about to registerIfNecessary gcm.  isGcmSupported:" + TheApp.isGcmSupported);
            gcmRegistrationManager.registerGCM();
        }

    }

    public void onCreate(final Activity activity) {

        if (!UserPreferences.getInstance().isLoggedIn()) {
            MLog.i(TAG, "not signed in; skip cloud messaging registration");
            return;
        }

        /**
         * Google targeted builds must have Google Play Services
         * in order to work properly
         */
        if (!Constants.IS_FOR_AMAZON_ONLY && !checkPlayServices(activity)) {
            return;
        }

        registerIfNecessary(activity.getApplicationContext());
    }

    public void onResume(final Activity activity) {

        if (!Constants.IS_FOR_AMAZON_ONLY) {
            checkPlayServices(activity);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If it
     * doesn't, display a dialog that allows users to download the APK from the
     * Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices(final Activity activity) {

        if (Constants.IS_FOR_AMAZON_ONLY) {
            return false;
        }

        final int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(TheApp.getInstance());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(resultCode)) {
                GoogleApiAvailability.getInstance().getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                MLog.i(TAG, "This device does not have google play services support, so cannot do gcm.  Too bad.");
                FirebaseAnalytics.getInstance(activity).logEvent(Events.NO_GOOGLE_PLAY_SERVICES,null);
            }
            return false;
        }
        TheApp.isGcmSupported = true;
        return true;
    }

    public void unregister(final String userid) {

        SimpleRxWrapper.executeInWorkerThread(
                new Runnable() {
                    @Override
                    public void run() {

                        if (TheApp.isAdmSupported) {

                            try {
                                MLog.i(TAG, "adm starting unregister");
                                new ADM(TheApp.getInstance()).startUnregister();
                                MLog.i(TAG, "adm unregistered");
                            } catch (final Throwable t) {
                                MLog.i(TAG, "adm failed to unregister", t);
                            }

                        }

                        if (TheApp.isGcmSupported) {

                            final String regId = GCMRegistrationManager.getRegistrationId(context);
                            MLog.i(TAG, "gcm starting unregister for regId: " + regId);
                            if (StringUtil.isNotEmpty(regId)) {
                                try {
                                    GCMRegistrationManager.removeRegistrationId(context);
                                    networkApi.gcmunreg(context, userid, regId);
                                    MLog.i(TAG, "gcm unregistered");
                                } catch (final Exception e) {
                                    MLog.e(TAG, "gcm failed to unregister", e);
                                }
                            }

                        }
                    }
                }
        );
    }



}
