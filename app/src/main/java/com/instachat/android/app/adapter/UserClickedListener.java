package com.instachat.android.app.adapter;

import android.view.View;

/**
 * Created by kevin on 9/13/2016.
 */
public interface UserClickedListener {
    void onUserClicked(int userid, String username, String dpid, View transitionImageView);
}
