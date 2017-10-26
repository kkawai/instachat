package com.instachat.android.app.activity.pm;

import com.instachat.android.app.activity.AbstractChatNavigator;

public interface PrivateChatNavigator extends AbstractChatNavigator {

    void collapseAppBar();
    void expandAppBar();
    void showErrorToast(String message);
    void showCannotChatWithBlockedUser(String username);
}
