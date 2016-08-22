package com.google.firebase.codelab.friendlychat;

/**
 * Created by kevin on 8/22/2016.
 */
public interface FriendlyMessageContainer {
    FriendlyMessage getFriendlyMessage(int position);
    int getFriendlyMessageCount();
    void setCurrentPosition(int position);
}
