package com.instachat.android.app.blocks;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.data.model.User;
import com.instachat.android.databinding.ItemBlockedPersonBinding;
import com.instachat.android.util.MLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BlocksAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<BlockedUser, BlocksAdapter.BlocksViewHolder> {

    private static final String TAG = "BlocksAdapter";

    private UserClickedListener mUserClickedListener;
    private List<Map.Entry<DatabaseReference, ValueEventListener>> mUserInfoChangeListeners = new ArrayList<>();

    public BlocksAdapter(Class<BlockedUser> modelClass, DatabaseReference ref) {
        super(modelClass, R.layout.item_blocked_person, BlocksViewHolder.class, ref);
    }

    public void setUserClickedListener(UserClickedListener userClickedListener) {
        mUserClickedListener = userClickedListener;
    }

    @Override
    public BlocksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemBlockedPersonBinding binding = ItemBlockedPersonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        final BlocksViewHolder holder = new BlocksViewHolder(binding);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BlockedUser blockedUser = getItem(holder.getAdapterPosition());
                mUserClickedListener.onUserClicked(blockedUser.id, blockedUser.getName(), blockedUser.getDpid(), holder.binding.userPic);
            }
        });
        return holder;
    }

    @Override
    protected void populateViewHolder(final BlocksViewHolder viewHolder, BlockedUser model, int position) {
        viewHolder.binding.setBlockedUser(model);
    }

    @Override
    protected BlockedUser parseSnapshot(DataSnapshot snapshot) {
        MLog.d(TAG, "BlockedUser: " + snapshot);
        BlockedUser blockedUser = snapshot.getValue(BlockedUser.class);
        blockedUser.id = Integer.parseInt(snapshot.getKey());
        listenForUserUpdates(blockedUser);
        return blockedUser;
    }

    @Override
    public void cleanup() {
        mUserClickedListener = null;
        for (Map.Entry<DatabaseReference, ValueEventListener> entry : mUserInfoChangeListeners) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        mUserInfoChangeListeners = null;
        super.cleanup();
    }

    private void listenForUserUpdates(final BlockedUser blockedUser) {

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(Integer.parseInt(blockedUser.id + "")));
        final ValueEventListener eventListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.getValue() != null) {
                        User user = dataSnapshot.getValue(User.class);
                        BlockedUser blockedUser = new BlockedUser(user.getId(), user.getUsername(), user.getProfilePicUrl());
                        updateBlockedUser(blockedUser);
                    }
                } catch (Exception e) {
                    MLog.e(TAG, "", e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Map.Entry<DatabaseReference, ValueEventListener> entry = new Map.Entry<DatabaseReference, ValueEventListener>() {
            @Override
            public DatabaseReference getKey() {
                return ref;
            }

            @Override
            public ValueEventListener getValue() {
                return eventListener;
            }

            @Override
            public ValueEventListener setValue(ValueEventListener valueEventListener) {
                return null;
            }
        };
        mUserInfoChangeListeners.add(entry);
    }

    private void updateBlockedUser(BlockedUser blockedUser) {
        FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF()).child(blockedUser.id + "").updateChildren(blockedUser.getUpdateMap());
    }

    static final class BlocksViewHolder extends RecyclerView.ViewHolder {

        private final ItemBlockedPersonBinding binding;

        public BlocksViewHolder(ItemBlockedPersonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}