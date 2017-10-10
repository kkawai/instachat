package com.instachat.android.app.fullscreen;

import com.instachat.android.data.model.FriendlyMessage;

/**
 * Created by kevin on 8/22/2016.
 */
public interface FriendlyMessageContainer {
    FriendlyMessage getFriendlyMessage(int position);

    int getFriendlyMessageCount();

    void setCurrentFriendlyMessage(int position);

    String getFriendlyMessageDatabase();
}
