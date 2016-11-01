package com.instachat.android.adapter;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.MyApp;
import com.instachat.android.R;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.model.GroupChatHeader;
import com.instachat.android.model.GroupChatSummary;
import com.instachat.android.model.PrivateChatHeader;
import com.instachat.android.model.PrivateChatSummary;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ThreadWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by kevin on 9/26/2016.
 * <p/>
 * Encapsulates a combination of PrivateChatSummary,
 * GroupChatSummary, PrivateChatHeader, and GroupChatHeader
 * objects in an array list;
 */
public class ChatSummariesRecyclerAdapter extends RecyclerView.Adapter implements StickyHeaderHandler {

    public static final String TAG = "ChatSummariesRecyclerAdapter";

    private static final int TYPE_GROUP_SUMMARY = 0;
    private static final int TYPE_PRIVATE_SUMMARY = 1;
    private static final int TYPE_PRIVATE_HEADER = 2;
    private static final int TYPE_GROUP_HEADER = 3;

    private List<Object> data = new ArrayList<>(128);
    private ChildEventListener privateChatsSummaryListener, publicGroupChatsSummaryListener;
    private DatabaseReference privateChatsSummaryReference, publicGroupChatsSummaryReference;
    private ChatsItemClickedListener chatsItemClickedListener;
    private Map<Long, Map.Entry<DatabaseReference, ChildEventListener>> publicGroupChatPresenceReferences = new HashMap<>();
    private ActivityState activityState;
    private List<Map.Entry<DatabaseReference, ValueEventListener>> userInfoRefs = new Vector<>(128);

    public ChatSummariesRecyclerAdapter(@NonNull ChatsItemClickedListener chatsItemClickedListener,
                                        @NonNull ActivityState activityState) {
        this.chatsItemClickedListener = chatsItemClickedListener;
        this.activityState = activityState;
    }

    public void populateData() {

        data.add(new GroupChatHeader(MyApp.getInstance().getString(R.string.group_chat_header)));
        data.add(new PrivateChatHeader(MyApp.getInstance().getString(R.string.private_chat_header)));
        privateChatsSummaryReference = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF());
        privateChatsSummaryListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                MLog.d(TAG, "private chat: onChildAdded() dataSnapshot: " + dataSnapshot.toString());
                if (!dataSnapshot.hasChild("name")) {
                    return;
                }
                PrivateChatSummary privateChatSummary = getPrivateChatSummary(dataSnapshot);
                insertPrivateChatSummary(privateChatSummary);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //todo to-user might have changed their name or sent a message that is unread by you
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                MLog.d(TAG, "private chat: onChildChanged() dataSnapshot: " + dataSnapshot.toString());
                if (!dataSnapshot.hasChild("name")) {
                    return;
                }
                PrivateChatSummary privateChatSummary = getPrivateChatSummary(dataSnapshot);
                updatePrivateChatSummary(privateChatSummary);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //todo user can remove this summary
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                MLog.d(TAG, "onChildRemoved() dataSnapshot: " + dataSnapshot.toString());
                if (!dataSnapshot.hasChild("name")) {
                    return;
                }
                PrivateChatSummary privateChatSummary = getPrivateChatSummary(dataSnapshot);
                removePrivateChatSummary(privateChatSummary);

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                //probably not useful right now, but will be later
                //when we prioritize private conversations
                MLog.d(TAG, "onChildMoved() dataSnapshot: " + dataSnapshot.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //??
            }
        };

        publicGroupChatsSummaryReference = FirebaseDatabase.getInstance().getReference(Constants.PUBLIC_CHATS_SUMMARY_PARENT_REF);
        publicGroupChatsSummaryListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                groupChatSummary.setId(Long.parseLong(dataSnapshot.getKey()));
                insertGroupChatSummary(groupChatSummary);
                addPublicGroupChatPresenceReference(groupChatSummary.getId());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                groupChatSummary.setId(Long.parseLong(dataSnapshot.getKey()));
                updateGroupChatSummary(groupChatSummary);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                groupChatSummary.setId(Long.parseLong(dataSnapshot.getKey()));
                removeGroupChatSummary(groupChatSummary);
                removePublicGroupChatPresenceReference(groupChatSummary.getId());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                //probably not useful
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //??
            }
        };
        publicGroupChatsSummaryReference.addChildEventListener(publicGroupChatsSummaryListener);
        privateChatsSummaryReference.addChildEventListener(privateChatsSummaryListener);

    }

    private PrivateChatSummary getPrivateChatSummary(DataSnapshot dataSnapshot) {
        PrivateChatSummary privateChatSummary = dataSnapshot.getValue(PrivateChatSummary.class);
        privateChatSummary.setId(dataSnapshot.getKey());
        if (dataSnapshot.hasChild(Constants.CHILD_UNREAD_MESSAGES)) {
            privateChatSummary.setUnreadMessageCount((int) dataSnapshot.child(Constants.CHILD_UNREAD_MESSAGES).getChildrenCount());
        } else {
            privateChatSummary.setUnreadMessageCount(0);
        }
        return privateChatSummary;
    }

    private void updateGroupChatSummary(GroupChatSummary groupChatSummary) {
        synchronized (this) {
            int index = data.indexOf(groupChatSummary);
            if (index != -1) {
                data.set(index, groupChatSummary);
                notifyItemChanged(index);
            }
        }
    }

    private void removeGroupChatSummary(GroupChatSummary groupChatSummary) {
        synchronized (this) {

            int index = data.indexOf(groupChatSummary);
            if (index != -1) {
                data.remove(index);
                notifyItemRemoved(index);
            }
        }
    }

    private void insertPrivateChatSummary(PrivateChatSummary privateChatSummary) {
        synchronized (this) {
            for (int i = 0; i < data.size(); i++) {
                Object o = data.get(i);
                if (o instanceof PrivateChatHeader) {

                    boolean inserted = false;
                    for (int j = i + 1; j < data.size(); j++) {
                        Object n = data.get(j);
                        if (n instanceof PrivateChatSummary) {
                            PrivateChatSummary curr = (PrivateChatSummary) n;
                            if (privateChatSummary.compareTo(curr) < 0) {
                                data.add(j, privateChatSummary);
                                notifyItemInserted(j);
                                inserted = true;
                                break;
                            }
                        } else {
                            data.add(j, privateChatSummary);
                            notifyItemInserted(j);
                            inserted = true;
                            break;
                        }
                    }
                    if (!inserted) {
                        data.add(privateChatSummary);
                        notifyItemInserted(data.size() - 1);
                    }
                    break;
                }
            }
        }
        listenForUserUpdates(privateChatSummary);
    }

    private void listenForUserUpdates(final PrivateChatSummary privateChatSummary) {

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(Integer.parseInt(privateChatSummary.getId())));
        final ValueEventListener eventListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                try {
                    if (dataSnapshot.getValue() != null) {
                        User user = dataSnapshot.getValue(User.class);
                        privateChatSummary.setLastOnline(user.getLastOnline());
                        privateChatSummary.setName(user.getUsername());
                    }
                    MLog.d(TAG, "debug user info. dataSnapshot: ", dataSnapshot);
                    getGcmStatus(privateChatSummary);
                } catch (Exception e) {
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        Map.Entry<DatabaseReference, ValueEventListener> entry = new Map.Entry<DatabaseReference, ValueEventListener>() {
            @Override
            public ValueEventListener getValue() {
                return eventListener;
            }

            @Override
            public DatabaseReference getKey() {
                return ref;
            }

            @Override
            public ValueEventListener setValue(ValueEventListener valueEventListener) {
                return null;
            }
        };
        ref.addValueEventListener(eventListener);
        userInfoRefs.add(entry);
    }

    private void getGcmStatus(final PrivateChatSummary privateChatSummary) {
        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                try {
                    if (NetworkApi.gcmcount(Integer.parseInt(privateChatSummary.getId())) > 0) {
                        if (System.currentTimeMillis() - privateChatSummary.getLastOnline() > Constants.TWELVE_HOURS) {
                            privateChatSummary.setOnlineStatus(Constants.USER_AWAY);
                        } else {
                            privateChatSummary.setOnlineStatus(Constants.USER_ONLINE);
                        }
                    } else {
                        privateChatSummary.setOnlineStatus(Constants.USER_OFFLINE);
                    }
                    if (activityState == null || activityState.isActivityDestroyed())
                        return;
                    FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).
                            child(privateChatSummary.getId()).
                            updateChildren(PrivateChatSummary.toMap(privateChatSummary));
                } catch (Exception e) {
                    MLog.e(TAG, "", e);
                }
            }
        });
    }

    /*private void sortPrivateChatSummaries() {
        int startIndex = -1;
        int endIndex = -1;
        ArrayList<PrivateChatSummary> list = new ArrayList<>(data.size());
        for (int i = 0; i < data.size(); i++) {
            Object object = data.get(i);
            if (object instanceof PrivateChatSummary) {
                if (startIndex == -1) {
                    startIndex = i;
                }
                list.add((PrivateChatSummary) object);
            } else if (startIndex != -1 && !(object instanceof PrivateChatSummary)) {
                endIndex = i - 1;
                break;
            }
        }
        if (endIndex == -1) {
            endIndex = data.size() - 1;
        }
        if (list.size() <= 1) {
            notifyItemChanged(endIndex);
            return;
        }
        for (int i = endIndex; i >= startIndex; i--) {
            data.remove(i);
        }
        Collections.sort(list);
        for (PrivateChatSummary s : list) {
            MLog.d(TAG, "sorted summary: " + s.getName(), "  ", s.getLastMessageSentTimestamp());
        }
        data.addAll(list);
        notifyItemRangeChanged(startIndex, endIndex);
    }*/

    private void updatePrivateChatSummary(PrivateChatSummary privateChatSummary) {
        synchronized (this) {
            int i = data.indexOf(privateChatSummary);
            if (i != -1) {
                data.set(i, privateChatSummary);
                notifyItemChanged(i);
            }
        }
    }

    private void removePrivateChatSummary(PrivateChatSummary privateChatSummary) {
        synchronized (this) {
            int i = data.indexOf(privateChatSummary);
            if (i != -1) {
                data.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    private void insertGroupChatSummary(GroupChatSummary groupChatSummary) {
        int groupHeaderIndex = -1;
        synchronized (this) {
            for (int i = 0; i < data.size(); i++) {
                Object o = data.get(i);
                if (o instanceof GroupChatHeader) {
                    groupHeaderIndex = i;

                    //handle case where GroupChatHeader is the only
                    //or last item in the data set
                    if (groupHeaderIndex + 1 == data.size()) {
                        data.add(i + 1, groupChatSummary);
                        notifyItemInserted(i + 1);
                        break;
                    } else {
                        continue;
                    }
                }

                if (groupHeaderIndex == -1)
                    continue;//keep iterating thru the data set until we find the GroupChatHeader

                if (o instanceof GroupChatSummary) {
                    GroupChatSummary summary = (GroupChatSummary) o;
                    if (groupChatSummary.compareTo(summary) < 0) {
                        data.add(i, groupChatSummary);
                        notifyItemInserted(i);
                        break;
                    }
                } else {
                    data.add(i, groupChatSummary);
                    notifyItemInserted(i);
                    break;
                }

            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object o = data.get(position);
        if (o instanceof GroupChatHeader) {
            return TYPE_GROUP_HEADER;
        } else if (o instanceof PrivateChatHeader) {
            return TYPE_PRIVATE_HEADER;
        } else if (o instanceof GroupChatSummary) {
            return TYPE_GROUP_SUMMARY;
        } else if (o instanceof PrivateChatSummary) {
            return TYPE_PRIVATE_SUMMARY;
        }
        throw new IllegalStateException("invalid object type in data array");
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_GROUP_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_header_item, parent, false);
            ChatHeaderViewHolder holder = new ChatHeaderViewHolder(view);
            return holder;
        } else if (viewType == TYPE_PRIVATE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_header_item, parent, false);
            ChatHeaderViewHolder holder = new ChatHeaderViewHolder(view);
            return holder;
        } else if (viewType == TYPE_GROUP_SUMMARY) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_list_item, parent, false);
            final GroupChatSummaryViewHolder holder = new GroupChatSummaryViewHolder(view);
            holder.name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    GroupChatSummary groupChatSummary = (GroupChatSummary) data.get(holder.getAdapterPosition());
                    chatsItemClickedListener.onGroupChatClicked(groupChatSummary);
                }
            });
            return holder;
        } else if (viewType == TYPE_PRIVATE_SUMMARY) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_list_item, parent, false);
            final PrivateChatSummaryViewHolder holder = new PrivateChatSummaryViewHolder(view);
            holder.name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PrivateChatSummary privateChatSummary = (PrivateChatSummary) data.get(holder.getAdapterPosition());
                    chatsItemClickedListener.onPrivateChatClicked(privateChatSummary);
                }
            });
            return holder;
        } else {
            throw new IllegalStateException("invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Resources res = holder.itemView.getResources();
        final int viewType = getItemViewType(position);
        if (viewType == TYPE_GROUP_HEADER) {

            GroupChatHeader groupChatHeader = (GroupChatHeader) data.get(position);
            ((ChatHeaderViewHolder) holder).name.setText(groupChatHeader.name);
            ((ChatHeaderViewHolder) holder).name.setTextColor(res.getColor(R.color.left_drawer_groups_label_text_color));

        } else if (viewType == TYPE_PRIVATE_HEADER) {

            PrivateChatHeader privateChatHeader = (PrivateChatHeader) data.get(position);
            ((ChatHeaderViewHolder) holder).name.setText(privateChatHeader.name);
            ((ChatHeaderViewHolder) holder).name.setTextColor(res.getColor(R.color.left_drawer_privates_label_text_color));

        } else if (viewType == TYPE_GROUP_SUMMARY) {
            GroupChatSummary groupChatSummary = (GroupChatSummary) data.get(position);
            ((GroupChatSummaryViewHolder) holder).name.setText(groupChatSummary.getName());
            ((GroupChatSummaryViewHolder) holder).name.setTextColor(res.getColor(R.color.left_drawer_list_room_name_text_color));
            ((GroupChatSummaryViewHolder) holder).status.setVisibility(View.INVISIBLE);
            /**
             * Note!  In the beginning stages of this app, we don't have group notifications
             * working yet as it requires some server work.  Let's use unread message count
             * for the number of users in the room for now.
             */
            if (groupChatSummary.getUsersInRoomCount() > 0) {
                ((GroupChatSummaryViewHolder) holder).unreadMessageCount.setVisibility(View.VISIBLE);
                ((GroupChatSummaryViewHolder) holder).unreadMessageCount.setText("  " + groupChatSummary.getUsersInRoomCount() + "  ");
            } else
                ((GroupChatSummaryViewHolder) holder).unreadMessageCount.setVisibility(View.INVISIBLE);

        } else if (viewType == TYPE_PRIVATE_SUMMARY) {

            PrivateChatSummary privateChatSummary = (PrivateChatSummary) data.get(position);
            ((PrivateChatSummaryViewHolder) holder).name.setText(privateChatSummary.getName());
            ((PrivateChatSummaryViewHolder) holder).status.setVisibility(View.VISIBLE);
            if (privateChatSummary.getUnreadMessageCount() > 0) {
                ((PrivateChatSummaryViewHolder) holder).unreadMessageCount.setVisibility(View.VISIBLE);
                ((PrivateChatSummaryViewHolder) holder).unreadMessageCount.setText("  " + privateChatSummary.getUnreadMessageCount() + "  ");
                ((PrivateChatSummaryViewHolder) holder).name.setTextColor(res.getColor(R.color.left_drawer_list_name_new_messages_text_color));
            } else {
                ((PrivateChatSummaryViewHolder) holder).unreadMessageCount.setVisibility(View.INVISIBLE);
                ((PrivateChatSummaryViewHolder) holder).name.setTextColor(res.getColor(R.color.left_drawer_list_name_text_color));
            }
            if (privateChatSummary.getOnlineStatus() == Constants.USER_ONLINE) {
                ((PrivateChatSummaryViewHolder) holder).status.setImageResource(R.drawable.presence_green);
            } else if (privateChatSummary.getOnlineStatus() == Constants.USER_AWAY) {
                ((PrivateChatSummaryViewHolder) holder).status.setImageResource(R.drawable.presence_away);
            } else if (privateChatSummary.getOnlineStatus() == Constants.USER_OFFLINE) {
                ((PrivateChatSummaryViewHolder) holder).status.setImageResource(R.drawable.presence_gone);
            } else {
                ((PrivateChatSummaryViewHolder) holder).status.setImageResource(R.drawable.presence_green);
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void cleanup() {
        privateChatsSummaryReference.removeEventListener(privateChatsSummaryListener);
        publicGroupChatsSummaryReference.removeEventListener(publicGroupChatsSummaryListener);
        for (long groupid : publicGroupChatPresenceReferences.keySet()) {
            Map.Entry<DatabaseReference, ChildEventListener> entry = publicGroupChatPresenceReferences.get(groupid);
            entry.getKey().removeEventListener(entry.getValue());
        }
        publicGroupChatPresenceReferences = null;
        activityState = null;
        for (Map.Entry<DatabaseReference, ValueEventListener> entry : userInfoRefs) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        userInfoRefs = null;
    }

    private synchronized void addPublicGroupChatPresenceReference(final long groupid) {
        if (publicGroupChatPresenceReferences.containsKey(groupid))
            return;
        /*
onChildAdded() dataSnapshot: DataSnapshot { key = 3733523, value = {username=kevintrevor, id=3733523, profilePicUrl=ea34ff82-066a-413f-9efe-a816d59863a7.jpg} }
onChildAdded() dataSnapshot: DataSnapshot { key = 234fakeUserid, value = {username=CoolistUserInWorld, id=234fakeUserid, profilePicUrl=blahblahblah} }
onChildRemoved() dataSnapshot: DataSnapshot { key = 234fakeUserid, value = {username=CoolistUserInWorld, id=234fakeUserid, profilePicUrl=blahblahblah} }
         */
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_REF(groupid));
        final ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildAdded() dataSnapshot: " + dataSnapshot, " groupid: ", groupid);
                addUserToPublicGroupChat(groupid);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildChanged() dataSnapshot: " + dataSnapshot, " groupid: ", groupid);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (activityState == null || activityState.isActivityDestroyed())
                    return;
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildRemoved() dataSnapshot: " + dataSnapshot, " groupid: ", groupid);
                removeUserFromPublicGroupChat(groupid);
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
        publicGroupChatPresenceReferences.put(groupid, entry);
    }

    private void removeUserFromPublicGroupChat(long groupid) {
        synchronized (this) {
            GroupChatSummary temp = new GroupChatSummary();
            temp.setId(groupid);
            int i = data.indexOf(temp);
            if (i != -1) {
                GroupChatSummary groupChatSummary = (GroupChatSummary) data.get(i);
                groupChatSummary.setUsersInRoomCount(groupChatSummary.getUsersInRoomCount() - 1);
                notifyItemChanged(i);
                MLog.d(TAG, "removeUserFromPublicGroupChat() groupid ", groupid);
            }
        }
    }

    private void addUserToPublicGroupChat(long groupid) {
        synchronized (this) {

            GroupChatSummary temp = new GroupChatSummary();
            temp.setId(groupid);
            int i = data.indexOf(temp);
            if (i != -1) {
                GroupChatSummary groupChatSummary = (GroupChatSummary) data.get(i);
                groupChatSummary.setUsersInRoomCount(groupChatSummary.getUsersInRoomCount() + 1);
                notifyItemChanged(i);
            }
        }
    }

    private synchronized void removePublicGroupChatPresenceReference(final long groupid) {
        Map.Entry<DatabaseReference, ChildEventListener> entry = publicGroupChatPresenceReferences.get(groupid);
        if (entry != null) {
            entry.getKey().removeEventListener(entry.getValue());
            publicGroupChatPresenceReferences.remove(groupid);
        }
    }

    @Override
    public List<?> getAdapterData() {
        return data;
    }

    /**
     * Remove given user from all public group chat rooms EXCEPT
     * for exceptionGroupId.  Call this when the user enters
     * any room, passing in the room they are about to enter
     * as the exceptionGroupId.
     * If the user is not about to enter any room, pass in 0
     * for the exceptionGroupId.
     *
     * @param userid
     * @param exceptionGroupId
     */
    public void removeUserFromAllGroups(int userid, long exceptionGroupId) {
        synchronized (this) {
            for (int i = 0; i < data.size(); i++) {
                Object o = data.get(i);
                if (o instanceof GroupChatHeader)
                    continue;
                if (o instanceof GroupChatSummary) {
                    GroupChatSummary groupChatSummary = (GroupChatSummary) o;
                    if (exceptionGroupId != groupChatSummary.getId() || exceptionGroupId == 0) {
                        FirebaseDatabase.getInstance().
                                getReference(Constants.GROUP_CHAT_USERS_REF(groupChatSummary.getId())).
                                child(userid + "").removeValue();
                    }
                } else {
                    break;
                }
            }
        }
    }
}
