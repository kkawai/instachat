package com.instachat.android.util;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public final class DeviceUtil {
    public static String getAndroidId(final Context context) {
        return "IH" + Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

		/*
		 * Do not rely on the WifiManager. On Nexus devices, if the WIFI is
		 * turned off, MAC address is null.
		 */
//		try {
//			final WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
//			str = wm.getConnectionInfo().getMacAddress();
//		} catch(final Exception e) {
//			
//		}
    }

    public static void hideKeyboard(final Activity activity) {
        final View view = activity.getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
