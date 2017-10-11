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
}
