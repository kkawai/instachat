package com.instachat.android.app.activity;

/**
 * Created by kevin on 10/23/2017.
 */

public interface AbstractChatNavigator {
    void showSignIn();
    void setMaxMessageLength(int maxMessageLength);
    void hideSmallProgressCircle();
}
