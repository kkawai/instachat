package com.instachat.android.util;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;

public class AdminUtil {
    private AdminUtil(){}

    public static boolean isMeAdmin() {
        return FirebaseRemoteConfig.getInstance().getString(Constants.KEY_ADMIN_USERS).contains("["+UserPreferences.getInstance().getUserId()+"]");
    }

    public static boolean isUserAdmin(int userid) {
        return FirebaseRemoteConfig.getInstance().getString(Constants.KEY_ADMIN_USERS).contains(""+userid);
    }

    public static boolean isMeAllowedToScreenShot() {
        try {
            return FirebaseRemoteConfig.getInstance().getString(Constants.KEY_USERS_ALLOWED_TO_SCREENSHOT).contains("[" + UserPreferences.getInstance().getUserId() + "]");
        }catch (Exception e) {
            return false;
        }
    }

    public static String encode(String string) throws Exception {
        byte bytes[] = new byte[string.length()];
        for (int j=0,i=string.length()-1;i >= 0;i--,j++) {
            bytes[j] = (byte)(string.charAt(i) - 65);
        }
        return Base64.encodeWebSafe(bytes,false);
    }

    public static String decode(String enc) throws Exception {
        StringBuilder dec = new StringBuilder();
        byte[] out = Base64.decodeWebSafe(enc);
        for (int j=0,i=out.length-1;i >= 0;i--,j++) {
            dec.append((char)(out[i]+65));
        }
        return dec.toString();
    }
}
