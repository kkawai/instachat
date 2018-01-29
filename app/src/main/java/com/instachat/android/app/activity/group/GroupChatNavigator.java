package com.instachat.android.app.activity.group;

import com.instachat.android.app.activity.AbstractChatNavigator;

public interface GroupChatNavigator extends AbstractChatNavigator {
    void showUserTyping(String username);
    void showSubtitle();
    void removeUserFromAllGroups(int userid, long exceptionGroupId);
    void showManageBlocks();
    void toggleGroupChatAppBar();
    void showBannedUsers();
}
