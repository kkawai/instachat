package com.instachat.android.font;

import android.graphics.Typeface;
import android.support.design.widget.TextInputLayout;
import android.widget.EditText;

/**
 * Created by kevin on 10/6/2016.
 */

public final class FontUtil {
    private FontUtil() {
    }

    private static Typeface sGlobalEditTextFont = null;

    public static void setEditTextFont(TextInputLayout textInputLayout) {
        setEditTextFont(textInputLayout.getEditText());
    }

    public static void setEditTextFont(EditText editText) {
        if (sGlobalEditTextFont == null)
            sGlobalEditTextFont = Typeface.createFromAsset(editText.getContext().getAssets(), "fonts/Lato-Regular.ttf");
        editText.setTypeface(sGlobalEditTextFont);
    }
}
