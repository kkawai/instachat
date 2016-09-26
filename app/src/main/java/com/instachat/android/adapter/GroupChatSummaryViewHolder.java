package com.instachat.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.instachat.android.R;

/**
 * Created by kevin on 9/26/2016.
 */
public class GroupChatSummaryViewHolder extends RecyclerView.ViewHolder {

    public TextView name, unreadMessageCount;
    public GroupChatSummaryViewHolder(View itemView) {
        super(itemView);
        name = (TextView)itemView.findViewById(R.id.name);
        unreadMessageCount = (TextView)itemView.findViewById(R.id.unread_message_count);
    }
}
