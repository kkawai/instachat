package com.instachat.android.adapter;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static final class Pair {
        DatabaseReference ref;
        ChildEventListener listener;
    }

    private List<User> data = new ArrayList<>(40);
    private long groupid;
    private UserClickedListener userClickedListener;
    private WeakReference<Activity> mActivity;
    private ActivityState mActivityState;
    private Map<Long, Pair> publicGroupChatPresenceReferences = new HashMap<>();

    public GroupChatUsersRecyclerAdapter(@NotNull Activity activity,
                                         @NotNull ActivityState activityState,
                                         @NonNull UserClickedListener userClickedListener,
                                         long groupid) {
        mActivity = new WeakReference<>(activity);
        mActivityState = activityState;
        this.userClickedListener = userClickedListener;
        this.groupid = groupid;
    }

    public void populateData() {
        addPublicGroupChatPresenceReference(groupid);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        final GroupChatUserViewHolder holder = new GroupChatUserViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                User user = data.get(holder.getAdapterPosition());
                userClickedListener.onUserClicked(user.getId());
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
        Constants.DP_URL(user.getId(), user.getProfilePicUrl(), new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (mActivityState.isActivityDestroyed())
                    return;
                try {
                    if (!task.isSuccessful()) {
                        groupChatUserViewHolder.userPic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                        return;
                    }
                    Glide.with(mActivity.get()).
                            load(task.getResult().toString()).
                            error(R.drawable.ic_account_circle_black_36dp).
                            into(groupChatUserViewHolder.userPic);
                } catch (final Exception e) {
                    MLog.e(TAG, "Constants.DP_URL user dp doesn't exist in google cloud storage.  task: " + task.isSuccessful());
                    groupChatUserViewHolder.userPic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void cleanup() {
        for (long groupid : publicGroupChatPresenceReferences.keySet()) {
            Pair pair = publicGroupChatPresenceReferences.get(groupid);
            pair.ref.removeEventListener(pair.listener);
        }
        publicGroupChatPresenceReferences.clear();
        if (mActivity != null)
            mActivity.clear();
        mActivity = null;
    }

    private synchronized void addPublicGroupChatPresenceReference(final long groupid) {
        if (publicGroupChatPresenceReferences.containsKey(groupid))
            return;
        /*
onChildAdded() dataSnapshot: DataSnapshot { key = 3733523, value = {username=kevintrevor, id=3733523, profilePicUrl=ea34ff82-066a-413f-9efe-a816d59863a7.jpg} }
onChildAdded() dataSnapshot: DataSnapshot { key = 234fakeUserid, value = {username=CoolistUserInWorld, id=234fakeUserid, profilePicUrl=blahblahblah} }
onChildRemoved() dataSnapshot: DataSnapshot { key = 234fakeUserid, value = {username=CoolistUserInWorld, id=234fakeUserid, profilePicUrl=blahblahblah} }
         */
        Pair pair = new Pair();
        pair.ref = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_REF(groupid));
        pair.listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildAdded() dataSnapshot: " + dataSnapshot);
                User user = dataSnapshot.getValue(User.class);
                user.setId(Integer.parseInt(dataSnapshot.getKey()));
                synchronized (this) {
                    data.add(user);
                    notifyItemInserted(data.size() - 1);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildChanged() dataSnapshot: " + dataSnapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildRemoved() dataSnapshot: " + dataSnapshot);
                final int userid = Integer.parseInt(dataSnapshot.getKey());
                synchronized (this) {
                    for (int i = 0; i < data.size(); i++) {
                        User user = data.get(i);
                        if (user.getId() == userid) {
                            data.remove(i);
                            notifyItemRemoved(i);
                            break;
                        }
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
        pair.ref.addChildEventListener(pair.listener);
        publicGroupChatPresenceReferences.put(groupid, pair);
    }
}
