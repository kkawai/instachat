package com.instachat.android.app.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.R;
import com.instachat.android.databinding.DrawerListItemBinding;

/**
 * Created by kevin on 9/26/2016.
 */
public class PrivateChatSummaryViewHolder extends RecyclerView.ViewHolder {

    DrawerListItemBinding binding;

    public PrivateChatSummaryViewHolder(DrawerListItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
