package com.instachat.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.instachat.android.R;

/**
 * Created by kevin on 9/26/2016.
 */
public class ChatHeaderViewHolder extends RecyclerView.ViewHolder {

    public TextView name;

    public ChatHeaderViewHolder(View view) {
        super(view);
        name = (TextView) view.findViewById(R.id.name);
    }
}
