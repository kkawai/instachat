package com.instachat.android.blocks;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.R;

public final class BlocksViewHolder extends RecyclerView.ViewHolder {
    TextView username;
    ImageView userPic;

    public BlocksViewHolder(View view) {
        super(view);
        username = (TextView) view.findViewById(R.id.username);
        userPic = (ImageView) view.findViewById(R.id.userPic);
    }
}
