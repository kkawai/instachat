package com.instachat.android.likes;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.R;

public final class UserLikedUserViewHolder extends RecyclerView.ViewHolder {
    TextView username;
    ImageView userPic;
    TextView likedPersonsPosts;

    public UserLikedUserViewHolder(View view) {
        super(view);
        username = (TextView) view.findViewById(R.id.username);
        userPic = (ImageView) view.findViewById(R.id.userPic);
        likedPersonsPosts = (TextView)view.findViewById(R.id.likedPersonsPosts);
    }
}
