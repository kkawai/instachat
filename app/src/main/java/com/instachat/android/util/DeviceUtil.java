package com.instachat.android.util;

import android.content.Context;
import android.provider.Settings.Secure;

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

    public static Map<String, Object> getAndroidIdMap(final Context context) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("d", getAndroidId(context));
        return map;
    }

}
