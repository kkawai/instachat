package com.instachat.android.app.requests;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.instachat.android.R;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.databinding.ItemRequestBinding;

import androidx.recyclerview.widget.RecyclerView;

public final class RequestsAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<PrivateChatSummary, RequestsAdapter.RequestsViewHolder> {

    private static final String TAG = "RequestsAdapter";

    private UserClickedListener mUserClickedListener;

    public RequestsAdapter(Class<PrivateChatSummary> modelClass, Query ref) {
        super(modelClass, R.layout.item_request, RequestsViewHolder.class, ref);
    }

    public void setUserClickedListener(UserClickedListener userClickedListener) {
        mUserClickedListener = userClickedListener;
    }

    @Override
    public RequestsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemRequestBinding binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        final RequestsViewHolder holder = new RequestsViewHolder(binding);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PrivateChatSummary privateChatSummary = getItem(holder.getAdapterPosition());
                mUserClickedListener.onUserClicked(Integer.parseInt(privateChatSummary.getId()), privateChatSummary.getName(), privateChatSummary.getDpid(), holder.binding.messengerImageView);
            }
        };
        holder.itemView.setOnClickListener(onClickListener);
        holder.binding.messageTextParent.setOnClickListener(onClickListener);
        return holder;
    }

    @Override
    protected void populateViewHolder(final RequestsViewHolder viewHolder, PrivateChatSummary model, int position) {
        viewHolder.binding.setPrivateChatSummary(model);
    }

    static final class RequestsViewHolder extends RecyclerView.ViewHolder {
        private final ItemRequestBinding binding;
        RequestsViewHolder(ItemRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @Override
    protected PrivateChatSummary parseSnapshot(DataSnapshot snapshot) {
        PrivateChatSummary privateChatSummary = snapshot.getValue(PrivateChatSummary.class);
        privateChatSummary.setId(snapshot.getKey());
        return privateChatSummary;
    }

    @Override
    public void cleanup() {
        mUserClickedListener = null;
        super.cleanup();
    }

}