package com.initech.gcm;

import android.app.Activity;
import android.content.Context;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.initech.Constants;
import com.initech.Events;
import com.initech.MyApp;
import com.initech.api.NetworkApi;
import com.initech.util.AnalyticsHelper;
import com.initech.util.MLog;
import com.initech.util.Preferences;
import com.initech.util.StringUtil;
import com.initech.util.ThreadWrapper;

/**
 * Invoke registerIfNecessary() method AFTER user authenticates with IG
 * and we have stored the USER
 * <p>
 * Supports both Google Cloud Messaging and Amazon Device Messaging
 *
 * @author kkawai
 */

public final class GCMHelper {

    private static final String TAG = GCMHelper.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private GCMHelper() {
    }

    private static void registerIfNecessary(final Context context) {

        MLog.i(TAG, "registerIfNecessary()");

        if (MyApp.isAdmSupported && Constants.IS_FOR_AMAZON_ONLY) {

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
                MyApp.isAdmSupported = false;
            }
            return;
        }

        if (MyApp.isGcmSupported && !Constants.IS_FOR_AMAZON_ONLY) {
            MLog.i(TAG, "debugx about to registerIfNecessary gcm.  isGcmSupported:" + MyApp.isGcmSupported);
            new GCMRegistrationManager(context).registerGCM();
        }

    }

    public static void onCreate(final Activity activity) {

        if (!Preferences.getInstance().isLoggedIn()) {
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

        registerIfNecessary(MyApp.getInstance());
    }

    public static void onResume(final Activity activity) {

        if (!Constants.IS_FOR_AMAZON_ONLY) {
            checkPlayServices(activity);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If it
     * doesn't, display a dialog that allows users to download the APK from the
     * Google Play Store or enable it in the device's system settings.
     */
    private static boolean checkPlayServices(final Activity activity) {

        if (Constants.IS_FOR_AMAZON_ONLY) {
            return false;
        }

        final int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MyApp.getInstance());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(resultCode)) {
                GoogleApiAvailability.getInstance().getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                MLog.i(TAG, "This device does not have google play services support, so cannot do gcm.  Too bad.");
                AnalyticsHelper.logEvent(Events.NO_GOOGLE_PLAY_SERVICES);
            }
            return false;
        }
        MyApp.isGcmSupported = true;
        return true;
    }

    public static void unregister(final String userid) {

        ThreadWrapper.executeInWorkerThread(
                new Runnable() {
                    @Override
                    public void run() {

                        if (MyApp.isAdmSupported) {

                            try {
                                MLog.i(TAG, "adm starting unregister");
                                new ADM(MyApp.getInstance()).startUnregister();
                                MLog.i(TAG, "adm unregistered");
                            } catch (final Throwable t) {
                                MLog.i(TAG, "adm failed to unregister", t);
                            }

                        }

                        if (MyApp.isGcmSupported) {

                            final String regId = GCMRegistrationManager.getRegistrationId(MyApp.getInstance());
                            MLog.i(TAG, "gcm starting unregister for regId: " + regId);
                            if (StringUtil.isNotEmpty(regId)) {
                                try {
                                    GCMRegistrationManager.removeRegistrationId(MyApp.getInstance());
                                    NetworkApi.gcmunreg(MyApp.getInstance(), userid, regId);
                                    MLog.i(TAG, "gcm unregistered");
                                } catch (final Exception e) {
                                    MLog.e(TAG, "", e);
                                    MLog.i(TAG, "gcm failed to unregister", e);
                                }
                            }
                        }
                    }
                }
        );
    }

}
