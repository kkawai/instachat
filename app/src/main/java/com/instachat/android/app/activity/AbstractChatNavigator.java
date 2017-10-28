package com.instachat.android.app.activity;

import android.net.Uri;

import com.instachat.android.data.model.FriendlyMessage;

/**
 * Created by kevin on 10/23/2017.
 */

public interface AbstractChatNavigator {
    void showSignIn();
    void setMaxMessageLength(int maxMessageLength);
    void hideSmallProgressCircle();
    void showSendOptions(FriendlyMessage friendlyMessage);
    void sendText(FriendlyMessage friendlyMessage);
    void showNeedPhotoDialog();
    void showTypingDots();
    void showErrorToast(String message);
    void showGroupChatActivity(long groupId, String groupName, Uri sharePhotoUri,
                               String shareMessage);
}
