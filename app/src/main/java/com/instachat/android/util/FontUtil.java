package com.instachat.android.util;

import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.instachat.android.R;

import androidx.core.content.res.ResourcesCompat;

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
