package com.instachat.android.fullscreen;

import com.instachat.android.model.FriendlyMessage;

/**
 * Created by kevin on 8/22/2016.
 */
public interface FriendlyMessageContainer {
    FriendlyMessage getFriendlyMessage(int position);
    int getFriendlyMessageCount();
    void setCurrentFriendlyMessage(int position);
}
