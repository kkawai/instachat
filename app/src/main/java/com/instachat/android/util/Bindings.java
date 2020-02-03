package com.instachat.android.util;

/**
 * Created by kevin on 8/6/2016.
 */

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.instachat.android.R;
import com.instachat.android.app.activity.pm.PrivateChatViewModel;
import com.instachat.android.app.bans.BannedUser;
import com.instachat.android.data.model.PrivateChatSummary;

import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

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
                    .into(imageView);
        }
    }

    @BindingAdapter("givenLikes")
    public static void setGivenLikes(TextView textView, int likes) {
        if (likes <= 1)
            textView.setText(textView.getContext().getString(R.string.gave_like_singular));
        else
            textView.setText(textView.getContext().getString(R.string.gave_likes_plural) + " " + likes);
    }

    @BindingAdapter("likes")
    public static void setLikes(TextView textView, int likes) {
        if (likes <= 1)
            textView.setText(textView.getContext().getString(R.string.like_singular));
        else
            textView.setText(likes + " " + textView.getContext().getString(R.string.likes_plural));
    }

    @BindingAdapter("pendingRequests")
    public static void setPendingRequests(TextView textView, int pendingRequsts) {
        if (pendingRequsts == 1) {
            textView.setText(R.string.left_drawer_pending_request_singular);
        } else if (pendingRequsts > 1) {
            textView.setText(pendingRequsts + " " + textView.getContext().getString(R.string.left_drawer_pending_requests_plural));
        }
    }

    @BindingAdapter("partnerStatus")
    public static void setPartnerStatus(ImageView imageView, int status) {
        if (status == PrivateChatSummary.USER_ONLINE) {
            imageView.setImageResource(R.drawable.presence_green);
        } else if (status == PrivateChatSummary.USER_AWAY) {
            imageView.setImageResource(R.drawable.presence_away);
        } else if (status == PrivateChatSummary.USER_OFFLINE) {
            imageView.setImageResource(R.drawable.presence_gone);
        } else {
            imageView.setImageResource(R.drawable.presence_green);
        }
    }

    public static void setPartnerProfilePic(ImageView imageView, final PrivateChatViewModel viewModel) {
        Glide.with(imageView.getContext()).load(viewModel.getPartner().getProfilePicUrl()).error(R.drawable
                .ic_anon_person_36dp)
                //.crossFade()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        viewModel.collapseAppbarAfterDelay();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        viewModel.collapseAppbarAfterDelay();
                        return false;
                    }
                }).into(imageView);
    }

    @BindingAdapter("bannedUser")
    public static void setBannedUser(TextView textView, BannedUser bannedUser) {
        textView.setText(bannedUser.username + " " + TimeUtil.timeLeft(bannedUser.banExpiration));
    }

    @BindingAdapter("bannedByAdmin")
    public static void setBannedByAdmin(TextView textView, BannedUser bannedUser) {
        textView.setText("Ban issued by "+bannedUser.admin + " (id:"+bannedUser.adminId+")");
    }

}