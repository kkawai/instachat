package com.instachat.android.app.activity.pm;

import com.instachat.android.app.activity.AbstractChatNavigator;
import com.instachat.android.data.model.User;

public interface PrivateChatNavigator extends AbstractChatNavigator {

    void collapseAppBar();
    void expandAppBar();
    void showCannotChatWithBlockedUser(String username);
    void showLikesCount(int count);
    void showUserProfile(User user);
    void showCustomTitles(String username, long lastOnline);
}
