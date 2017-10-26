package com.instachat.android.app.activity.group;

import com.instachat.android.app.activity.AbstractChatNavigator;
import com.instachat.android.data.model.FriendlyMessage;

public interface GroupChatNavigator extends AbstractChatNavigator {
    void showUserTyping(String username);
    void showSubtitle();
    void removeUserFromAllGroups(int userid, long exceptionGroupId);
}
