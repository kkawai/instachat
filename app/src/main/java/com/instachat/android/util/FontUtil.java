package com.instachat.android.util;

import android.graphics.Typeface;
import android.support.design.widget.TextInputLayout;
import android.widget.TextView;

/**
 * Created by kevin on 10/6/2016.
 */

public final class FontUtil {
    private FontUtil() {
    }

    public static void setTextViewFont(TextInputLayout textInputLayout) {
        setTextViewFont(textInputLayout.getEditText());
    }

    public static void setTextViewFont(TextView textView) {
        Bindings.setCustomTypeface(textView, "Lato-Regular");
    }
}
