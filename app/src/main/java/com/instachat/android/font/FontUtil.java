package com.instachat.android.font;

import android.graphics.Typeface;
import android.support.design.widget.TextInputLayout;
import android.widget.TextView;

/**
 * Created by kevin on 10/6/2016.
 */

public final class FontUtil {
    private FontUtil() {
    }

    private static Typeface sGlobalEditTextFont = null;

    public static void setTextViewFont(TextInputLayout textInputLayout) {
        setTextViewFont(textInputLayout.getEditText());
    }

    public static void setTextViewFont(TextView textView) {
        if (sGlobalEditTextFont == null)
            sGlobalEditTextFont = Typeface.createFromAsset(textView.getContext().getAssets(), "fonts/Lato-Regular.ttf");
        textView.setTypeface(sGlobalEditTextFont);
    }
}
