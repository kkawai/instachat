package com.instachat.android.view;

import android.graphics.BlurMaskFilter;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

/**
 * Created by kevin on 10/29/2016.
 */

public final class TextViewUtil {
    private TextViewUtil() {
    }

    /**
     *
     * @param textView
     * @param doBlur - if false, undoes any blur on the given textView
     */
    public static void blurText(TextView textView, boolean doBlur) {
        if (!doBlur) {
            textView.getPaint().setMaskFilter(null);
            return;
        }
        if (Build.VERSION.SDK_INT >= 11) {
            textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        float radius = textView.getTextSize() / 3;
        BlurMaskFilter filter = new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL);
        textView.getPaint().setMaskFilter(filter);
    }
}
