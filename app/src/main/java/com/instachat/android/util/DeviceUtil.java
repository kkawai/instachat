package com.instachat.android.util;

import android.content.Context;
import android.provider.Settings.Secure;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

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

    public static String getFirebaseUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public static Map<String, Object> getAndroidIdMap(final Context context) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("d", getFirebaseUid());
        return map;
    }

}
