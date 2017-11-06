package com.instachat.android.util;

import android.graphics.Typeface;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.widget.TextView;

import com.instachat.android.R;

public final class FontUtil {
    private FontUtil() {
    }

    public static void setTextViewFont(TextInputLayout textInputLayout) {
        setTextViewFont(textInputLayout.getEditText());
    }

    public static void setTextViewFont(TextView textView) {
        textView.setTypeface(ResourcesCompat.getFont(textView.getContext(), R.font.lato_regular));
    }
}
