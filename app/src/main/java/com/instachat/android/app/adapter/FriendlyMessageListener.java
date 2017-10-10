package com.instachat.android.app.adapter;

import com.instachat.android.data.model.FriendlyMessage;

/**
 * Created by kevin on 9/30/2016.
 */
public interface FriendlyMessageListener {

    void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage);

    void onFriendlyMessageFail(FriendlyMessage friendlyMessage);
}
