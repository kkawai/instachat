package com.instachat.android.app.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.R;

/**
 * Created by kevin on 9/26/2016.
 */
public class PrivateChatSummaryViewHolder extends RecyclerView.ViewHolder {

    public TextView name, unreadMessageCount;
    public ImageView status;

    public PrivateChatSummaryViewHolder(View itemView) {
        super(itemView);
        name = (TextView) itemView.findViewById(R.id.name);
        unreadMessageCount = (TextView) itemView.findViewById(R.id.unread_message_count);
        status = (ImageView) itemView.findViewById(R.id.status);
    }
}
