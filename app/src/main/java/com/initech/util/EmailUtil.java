package com.initech.util;

/**
 * Created by kevin on 7/18/2016.
 */
public final class EmailUtil {
    public static boolean isValidEmail(final CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public static boolean isValidPassword(final String password) {
        return password.trim().length() > 4;
    }
}
