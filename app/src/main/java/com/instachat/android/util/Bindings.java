package com.instachat.android.util;

/**
 * Created by kevin on 8/6/2016.
 */

import android.app.Activity;
import android.databinding.BindingAdapter;
import android.graphics.Typeface;
import android.support.design.widget.TextInputLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom bindings for XML attributes using data binding.
 * (http://developer.android.com/tools/data-binding/guide.html)
 */
public class Bindings {

    private Bindings(){}

    private static Map<String,Typeface> cache = new HashMap<>(20);

    @BindingAdapter("imageUrl")
    public static void setImageUrl(ImageView imageView, String url) {
        Glide.with((Activity)imageView.getContext()).load(url).into(imageView);
    }

    @BindingAdapter("customTypeface")
    public static void setCustomTypeface(TextInputLayout textInputLayout, String name) {
        setCustomTypeface(textInputLayout.getEditText(), name);
    }

    @BindingAdapter("customTypeface")
    public static void setCustomTypeface(TextView textView, String name) {
        Typeface typeface = cache.get(name);
        if (typeface == null) {
            typeface = Typeface.createFromAsset(textView.getContext().getAssets(), "fonts/"+name+".ttf");
            cache.put(name, typeface);
        }
        textView.setTypeface(typeface);
    }

}