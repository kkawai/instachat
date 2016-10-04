package com.instachat.android.adapter;

import com.instachat.android.model.GroupChatSummary;
import com.instachat.android.model.PrivateChatSummary;

/**
 * Created by kevin on 9/26/2016.
 */
public interface ChatsItemClickedListener {
    void onPrivateChatClicked(PrivateChatSummary privateChatSummary);
    void onGroupChatClicked(GroupChatSummary groupChatSummary);
}
