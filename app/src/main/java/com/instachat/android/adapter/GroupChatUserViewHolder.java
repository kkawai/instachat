package com.instachat.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.R;

/**
 * Created by kevin on 9/26/2016.
 */
public class GroupChatUserViewHolder extends RecyclerView.ViewHolder {

    public TextView username;
    public ImageView userPic;
    public GroupChatUserViewHolder(View view) {
        super(view);
        username = (TextView)view.findViewById(R.id.username);
        userPic = (ImageView)view.findViewById(R.id.userPic);
    }
}
