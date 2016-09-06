package com.google.firebase.codelab.friendlychat.fullscreen;

import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;

/**
 * Created by kevin on 8/22/2016.
 */
public interface FriendlyMessageContainer {
    FriendlyMessage getFriendlyMessage(int position);
    int getFriendlyMessageCount();
    void setCurrentFriendlyMessage(int position);
}
