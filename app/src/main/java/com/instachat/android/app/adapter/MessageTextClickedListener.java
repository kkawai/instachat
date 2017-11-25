package com.instachat.android.app.adapter;

import com.instachat.android.data.model.FriendlyMessage;

/**
 * Created by kevin on 9/3/2016.
 */
public interface MessageTextClickedListener {
    void onMessageClicked(int position);
    void onMessageLongClicked(FriendlyMessage friendlyMessage);
}
