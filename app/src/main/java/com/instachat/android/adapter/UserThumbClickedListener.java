package com.instachat.android.adapter;

import android.widget.ImageView;

import com.instachat.android.model.FriendlyMessage;

/**
 * Created by kevin on 9/13/2016.
 */
public interface UserThumbClickedListener {
    void onUserThumbClicked(ImageView imageView, FriendlyMessage message);
}
