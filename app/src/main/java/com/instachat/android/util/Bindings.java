package com.instachat.android.util;

/**
 * Created by kevin on 8/6/2016.
 */

import android.app.Activity;
import android.databinding.BindingAdapter;
import android.graphics.Typeface;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.instachat.android.R;
import com.instachat.android.data.model.PrivateChatSummary;

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
        if (TextUtils.isEmpty(url)) {
            imageView.setImageResource(R.drawable.ic_anon_person_36dp);
        } else {
            Glide.with((Activity) imageView.getContext()).load(url).error(R.drawable.ic_anon_person_36dp).into(imageView);
        }
    }

    @BindingAdapter("customTypeface")
    public static void setCustomTypeface(TextInputLayout textInputLayout, String name) {
        setCustomTypeface(textInputLayout.getEditText(), name);
    }

    @BindingAdapter("customTypeface")
    public static void setCustomTypeface(TextView textView, String name) {
        name = name.endsWith(".ttf") ? ("fonts/"+name) : ("fonts/"+name+".ttf");
        Typeface typeface = cache.get(name);
        if (typeface == null) {
            typeface = Typeface.createFromAsset(textView.getContext().getAssets(), name);
            cache.put(name, typeface);
        }
        textView.setTypeface(typeface);
    }

    @BindingAdapter("likesCount")
    public static void setLikesCount(TextView textView, int likes) {
        if (likes <= 1)
            textView.setText(textView.getContext().getString(R.string.gave_like_singular));
        else
            textView.setText(textView.getContext().getString(R.string.gave_likes_plural,(""+likes)));
    }

}