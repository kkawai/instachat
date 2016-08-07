package com.google.firebase.codelab.friendlychat;

import android.databinding.BindingAdapter;
import android.widget.TextView;

/**
 * Created by kevin on 8/6/2016.
 */

/**
 * Custom bindings for XML attributes using data binding.
 * (http://developer.android.com/tools/data-binding/guide.html)
 */
public class Bindings {

    @BindingAdapter({"bind:font"})
    public static void setFont(TextView textView, String fontName) {
        textView.setTypeface(FontCache.getInstance().get(fontName));
    }
}