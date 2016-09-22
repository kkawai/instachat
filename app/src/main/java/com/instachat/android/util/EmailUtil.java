package com.instachat.android.util;

import com.instachat.android.Constants;

/**
 * Created by kevin on 7/18/2016.
 */
public final class EmailUtil {
    public static boolean isValidEmail(final CharSequence target) {
        if (target.length() < Constants.MIN_EMAIL_LENGTH || target.length() > Constants.MAX_EMAIL_LENGTH) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }
}
