package com.instachat.android.app.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.R;
import com.instachat.android.databinding.DrawerListItemBinding;

/**
 * Created by kevin on 9/26/2016.
 */
public class GroupChatSummaryViewHolder extends RecyclerView.ViewHolder {

    DrawerListItemBinding binding;

    public GroupChatSummaryViewHolder(DrawerListItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
