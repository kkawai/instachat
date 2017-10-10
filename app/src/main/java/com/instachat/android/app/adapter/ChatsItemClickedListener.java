package com.instachat.android.app.adapter;

import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.data.model.PrivateChatSummary;

/**
 * Created by kevin on 9/26/2016.
 */
public interface ChatsItemClickedListener {
    void onPrivateChatClicked(PrivateChatSummary privateChatSummary);

    void onGroupChatClicked(GroupChatSummary groupChatSummary);
}
