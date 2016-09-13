package com.google.firebase.codelab.friendlychat.adapter;

import android.widget.ImageView;

import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;

/**
 * Created by kevin on 9/13/2016.
 */
public interface UserThumbClickedListener {
    void onUserThumbClicked(ImageView imageView, FriendlyMessage message);
}
