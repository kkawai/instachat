package com.instachat.android.app.requests;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.instachat.android.R;
import com.instachat.android.app.adapter.MessageViewHolder;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.util.MLog;
import com.instachat.android.util.TimeUtil;

public final class RequestsAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<PrivateChatSummary, MessageViewHolder> {

    private static final String TAG = "RequestsAdapter";

    private UserClickedListener mUserClickedListener;

    public RequestsAdapter(Class<PrivateChatSummary> modelClass, int modelLayout, Class<MessageViewHolder> viewHolderClass, DatabaseReference ref) {
        super(modelClass, modelLayout, viewHolderClass, ref);
    }

    public void setUserClickedListener(UserClickedListener userClickedListener) {
        mUserClickedListener = userClickedListener;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final MessageViewHolder holder = super.onCreateViewHolder(parent, viewType);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PrivateChatSummary privateChatSummary = getItem(holder.getAdapterPosition());
                mUserClickedListener.onUserClicked(Integer.parseInt(privateChatSummary.getId()), privateChatSummary.getName(), privateChatSummary.getDpid(), holder.messengerImageView);
            }
        };
        holder.itemView.setOnClickListener(onClickListener);
        holder.messageTextParent.setOnClickListener(onClickListener);
        return holder;
    }

    @Override
    protected void populateViewHolder(final MessageViewHolder viewHolder, PrivateChatSummary model, int position) {
        viewHolder.messengerTextView.setText(model.getName());
        viewHolder.messageTimeTextView.setText(TimeUtil.getTimeAgo(model.getLastMessageSentTimestamp()));
        try {
            Glide.with(viewHolder.messengerImageView.getContext()).load(model.getDpid()).error(R.drawable.ic_anon_person_36dp).into(viewHolder.messengerImageView);
        } catch (final Exception e) {
            MLog.e(TAG, "", e);
            viewHolder.messengerImageView.setImageResource(R.drawable.ic_anon_person_36dp);
        }
        viewHolder.messageTextView.setText(model.getLastMessage());
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