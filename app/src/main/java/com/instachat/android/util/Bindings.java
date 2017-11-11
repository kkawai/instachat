package com.instachat.android.util;

/**
 * Created by kevin on 8/6/2016.
 */

import android.app.Activity;
import android.databinding.BindingAdapter;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.instachat.android.R;
import com.instachat.android.app.activity.pm.PrivateChatViewModel;

/**
 * Custom bindings for XML attributes using data binding.
 * (http://developer.android.com/tools/data-binding/guide.html)
 */
public class Bindings {

    private Bindings(){}

    @BindingAdapter("imageUrlNoCrossFade")
    public static void setImageUrlNoCrossFade(ImageView imageView, String url) {
        if (TextUtils.isEmpty(url)) {
            imageView.setImageResource(R.drawable.ic_anon_person_36dp);
        } else {
            Glide.with((Activity) imageView.getContext())
                    .load(url)
                    .error(R.drawable.ic_anon_person_36dp)
                    .into(imageView);
        }
    }

    @BindingAdapter("imageUrlWithCrossFade")
    public static void setImageUrlWithCrossFade(ImageView imageView, String url) {
        if (TextUtils.isEmpty(url)) {
            imageView.setImageResource(R.drawable.ic_anon_person_36dp);
        } else {
            Glide.with(imageView.getContext())
                    .load(url)
                    .error(R.drawable.ic_anon_person_36dp)
                    .crossFade()
                    .into(imageView);
        }
    }

    @BindingAdapter("givenLikes")
    public static void setGivenLikes(TextView textView, int likes) {
        if (likes <= 1)
            textView.setText(textView.getContext().getString(R.string.gave_like_singular));
        else
            textView.setText(textView.getContext().getString(R.string.gave_likes_plural,(""+likes)));
    }

    @BindingAdapter("likes")
    public static void setLikes(TextView textView, int likes) {
        if (likes <= 1)
            textView.setText(textView.getContext().getString(R.string.like_singular));
        else
            textView.setText(textView.getContext().getString(R.string.likes_plural,(""+likes)));
    }

    @BindingAdapter("pendingRequests")
    public static void setPendingRequests(TextView textView, int pendingRequsts) {
        if (pendingRequsts == 1) {
            textView.setText(R.string.left_drawer_pending_request_singular);
        } else if (pendingRequsts > 1) {
            textView.setText(textView.getContext().getString(R.string.left_drawer_pending_requests_plural, "" + pendingRequsts));
        }
    }

    public static void setPartnerProfilePic(ImageView imageView, final PrivateChatViewModel viewModel) {
        Glide.with(imageView.getContext()).load(viewModel.getPartner().getProfilePicUrl()).error(R.drawable
                .ic_anon_person_36dp)
                //.crossFade()
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                               boolean isFirstResource) {
                        viewModel.collapseAppbarAfterDelay();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model,
                                                   Target<GlideDrawable> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        viewModel.collapseAppbarAfterDelay();
                        return false;
                    }
                }).into(imageView);
    }

}