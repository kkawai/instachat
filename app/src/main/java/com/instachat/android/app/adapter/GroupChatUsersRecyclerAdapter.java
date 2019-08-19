package com.instachat.android.app.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.data.model.User;
import com.instachat.android.databinding.ItemPersonBinding;
import com.instachat.android.util.MLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by kevin on 9/26/2016.
 * <p/>
 * Encapsulates a combination of PrivateChatSummary,
 * GroupChatSummary, PrivateChatHeader, and GroupChatHeader
 * objects in an array list;
 */
public class GroupChatUsersRecyclerAdapter extends RecyclerView.Adapter<GroupChatUsersRecyclerAdapter.GroupChatUserViewHolder> {

    public static final String TAG = "ChatSummariesRecyclerAdapter";

    private List<User> users = new ArrayList<>(40);
    private long groupid;
    private UserClickedListener userClickedListener;
    private DatabaseReference ref;
    private ChildEventListener listener;
    private List<Map.Entry<DatabaseReference, ValueEventListener>> userInfoChangeListeners = new ArrayList<>(128);

    public GroupChatUsersRecyclerAdapter(@NonNull UserClickedListener userClickedListener,
                                         long groupid) {
        this.userClickedListener = userClickedListener;
        this.groupid = groupid;
    }

    public void populateData() {
        ref = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_REF(groupid)).orderByChild("lastOnline").limitToLast(50).getRef();
        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildAdded() dataSnapshot: " + dataSnapshot);
                User user = dataSnapshot.getValue(User.class);
                user.setId(Integer.parseInt(dataSnapshot.getKey()));
                synchronized (GroupChatUsersRecyclerAdapter.this) {
                    if (users.size() > Constants.MAX_USERS_IN_ROOM)
                        return;
                    users.add(user);
                    notifyItemInserted(users.size() - 1);
                    /**
                     * this is stupid; don't listen for individual updates
                     */
                    //listenForUserUpdates(user);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildChanged() dataSnapshot: " + dataSnapshot);
                User user = dataSnapshot.getValue(User.class);
                user.setId(Integer.parseInt(dataSnapshot.getKey()));
                synchronized (GroupChatUsersRecyclerAdapter.this) {
                    int index = users.indexOf(user);
                    if (index != -1) {
                        users.set(index, user);
                        notifyItemChanged(index);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildRemoved() dataSnapshot: " + dataSnapshot);
                final int userid = Integer.parseInt(dataSnapshot.getKey());
                User user = new User();
                user.setId(userid);
                synchronized (GroupChatUsersRecyclerAdapter.this) {
                    int index = users.indexOf(user);
                    if (index != -1) {
                        users.remove(index);
                        notifyItemRemoved(index);
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildMoved() dataSnapshot: " + dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        ref.addChildEventListener(listener);
    }

    @Override
    public GroupChatUserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemPersonBinding binding = ItemPersonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        final GroupChatUserViewHolder holder = new GroupChatUserViewHolder(binding);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                User user = users.get(holder.getAdapterPosition());
                userClickedListener.onUserClicked(user.getId(), user.getUsername(), user.getProfilePicUrl(), holder.binding.userPic);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(GroupChatUserViewHolder holder, int position) {
        User user = users.get(position);
        holder.binding.setUser(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void cleanup() {
        if (ref != null && listener != null)
            ref.removeEventListener(listener);
        for (Map.Entry<DatabaseReference, ValueEventListener> entry : userInfoChangeListeners) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        userInfoChangeListeners = null;
    }

    @Deprecated
    private void listenForUserUpdates(final User user) {

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(Integer.parseInt(user.getId() + "")));
        final ValueEventListener eventListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.getValue() != null) {
                        final User user = dataSnapshot.getValue(User.class);
                        synchronized (GroupChatUsersRecyclerAdapter.this) {
                            int i = users.indexOf(user);
                            if (i != -1) {
                                users.set(i, user);
                                notifyItemChanged(i);
                            }
                        }
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
                return valueEventListener;
            }
        };
        ref.addValueEventListener(eventListener);
        userInfoChangeListeners.add(entry);
    }

    final static class GroupChatUserViewHolder extends RecyclerView.ViewHolder {
        private final ItemPersonBinding binding;
        GroupChatUserViewHolder(ItemPersonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
