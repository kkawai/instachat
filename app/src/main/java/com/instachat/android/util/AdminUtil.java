package com.instachat.android.util;

import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public class AdminUtil {
    private AdminUtil(){}

    /**
     *
     * @param admins - comma separated list of user ids that are admins
     * @return
     */
    public static Set<String> getAdmins(@NonNull String admins) {
        String[] list = admins.split(", ");
        Set<String> adminSet = new HashSet<>();
        for (String s : list) {
            adminSet.add(s);
        }
        return adminSet;
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
