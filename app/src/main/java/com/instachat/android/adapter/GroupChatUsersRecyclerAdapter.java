package com.instachat.android.adapter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;

import java.lang.ref.WeakReference;
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
public class GroupChatUsersRecyclerAdapter extends RecyclerView.Adapter {

    public static final String TAG = "ChatSummariesRecyclerAdapter";

    private List<User> data = new ArrayList<>(40);
    private long groupid;
    private UserClickedListener userClickedListener;
    private WeakReference<Activity> mActivity;
    private ActivityState mActivityState;
    private DatabaseReference ref;
    private ChildEventListener listener;
    private List<Map.Entry<DatabaseReference, ValueEventListener>> userInfoChangeListeners = new ArrayList<>(128);

    public GroupChatUsersRecyclerAdapter(@NonNull Activity activity,
                                         @NonNull ActivityState activityState,
                                         @NonNull UserClickedListener userClickedListener,
                                         long groupid) {
        mActivity = new WeakReference<>(activity);
        mActivityState = activityState;
        this.userClickedListener = userClickedListener;
        this.groupid = groupid;
    }

    public void populateData() {
        ref = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_REF(groupid));
        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildAdded() dataSnapshot: " + dataSnapshot);
                User user = dataSnapshot.getValue(User.class);
                user.setId(Integer.parseInt(dataSnapshot.getKey()));
                synchronized (GroupChatUsersRecyclerAdapter.this) {
                    data.add(user);
                    notifyItemInserted(data.size() - 1);
                    listenForUserUpdates(user);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildChanged() dataSnapshot: " + dataSnapshot);
                User user = dataSnapshot.getValue(User.class);
                user.setId(Integer.parseInt(dataSnapshot.getKey()));
                synchronized (GroupChatUsersRecyclerAdapter.this) {
                    int index = data.indexOf(user);
                    if (index != -1) {
                        data.set(index, user);
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
                    int index = data.indexOf(user);
                    if (index != -1) {
                        data.remove(index);
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

        Map.Entry<DatabaseReference, ChildEventListener> entry = new Map.Entry<DatabaseReference, ChildEventListener>() {
            @Override
            public DatabaseReference getKey() {
                return ref;
            }

            @Override
            public ChildEventListener getValue() {
                return listener;
            }

            @Override
            public ChildEventListener setValue(ChildEventListener childEventListener) {
                return null;
            }
        };
        ref.addChildEventListener(listener);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        final GroupChatUserViewHolder holder = new GroupChatUserViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                User user = data.get(holder.getAdapterPosition());
                userClickedListener.onUserClicked(user.getId(), user.getUsername(), user.getProfilePicUrl(), holder.userPic);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        //int viewType = getItemViewType(position);
        User user = data.get(position);
        final GroupChatUserViewHolder groupChatUserViewHolder = (GroupChatUserViewHolder) holder;
        groupChatUserViewHolder.username.setText(user.getUsername());
        try {
            Glide.with(mActivity.get()).
                    load(user.getProfilePicUrl()).
                    error(R.drawable.ic_anon_person_36dp).
                    into(groupChatUserViewHolder.userPic);
        } catch (final Exception e) {
            MLog.e(TAG, "", e);
            groupChatUserViewHolder.userPic.setImageResource(R.drawable.ic_anon_person_36dp);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void cleanup() {
        if (ref != null && listener != null)
            ref.removeEventListener(listener);
        if (mActivity != null)
            mActivity.clear();
        mActivity = null;
        mActivityState = null;
        for (Map.Entry<DatabaseReference, ValueEventListener> entry : userInfoChangeListeners) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        userInfoChangeListeners = null;
    }

    private void listenForUserUpdates(final User user) {

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(Integer.parseInt(user.getId() + "")));
        final ValueEventListener eventListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;
                try {
                    if (dataSnapshot.getValue() != null) {
                        final User user = dataSnapshot.getValue(User.class);
                        synchronized (GroupChatUsersRecyclerAdapter.this) {
                            int i = data.indexOf(user);
                            if (i != -1) {
                                data.set(i, user);
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
}
