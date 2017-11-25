package com.instachat.android.app.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.instachat.android.R;
import com.instachat.android.databinding.DrawerHeaderItemBinding;

/**
 * Created by kevin on 9/26/2016.
 */
public class ChatHeaderViewHolder extends RecyclerView.ViewHolder {

    DrawerHeaderItemBinding binding;

    public ChatHeaderViewHolder(DrawerHeaderItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
