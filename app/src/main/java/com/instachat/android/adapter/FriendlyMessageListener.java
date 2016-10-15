package com.instachat.android.adapter;

import com.instachat.android.model.FriendlyMessage;

/**
 * Created by kevin on 9/30/2016.
 */
public interface FriendlyMessageListener {

    void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage);

    void onFriendlyMessageFail(FriendlyMessage friendlyMessage);
}
