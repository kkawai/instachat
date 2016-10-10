package com.instachat.android.adapter;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.Constants;
import com.instachat.android.MyApp;
import com.instachat.android.R;
import com.instachat.android.model.GroupChatHeader;
import com.instachat.android.model.GroupChatSummary;
import com.instachat.android.model.PrivateChatHeader;
import com.instachat.android.model.PrivateChatSummary;
import com.instachat.android.util.MLog;

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
public class ChatSummariesRecyclerAdapter extends RecyclerView.Adapter {

    public static final String TAG = "ChatSummariesRecyclerAdapter";

    private static final int TYPE_GROUP_SUMMARY = 0;
    private static final int TYPE_PRIVATE_SUMMARY = 1;
    private static final int TYPE_PRIVATE_HEADER = 2;
    private static final int TYPE_GROUP_HEADER = 3;

    private static final class Pair {
        DatabaseReference ref;
        ChildEventListener listener;
    }

    private List<Object> data = new ArrayList<>(40);
    private ChildEventListener privateChatsSummaryListener, publicGroupChatsSummaryListener;
    private DatabaseReference privateChatsSummaryReference, publicGroupChatsSummaryReference;
    private ChatsItemClickedListener chatsItemClickedListener;
    private Map<Long, Pair> publicGroupChatPresenceReferences = new HashMap<>();

    public ChatSummariesRecyclerAdapter(ChatsItemClickedListener chatsItemClickedListener) {
        this.chatsItemClickedListener = chatsItemClickedListener;
    }

    public void populateData() {

        data.add(new GroupChatHeader(MyApp.getInstance().getString(R.string.group_chat_header)));
        data.add(new PrivateChatHeader(MyApp.getInstance().getString(R.string.private_chat_header)));
        privateChatsSummaryReference = FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHATS_SUMMARY_PARENT_REF());
        privateChatsSummaryListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "onChildAdded() dataSnapshot: " + dataSnapshot.toString());
                PrivateChatSummary privateChatSummary = getPrivateChatSummary(dataSnapshot);
                insertPrivateChatSummary(privateChatSummary);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //todo to-user might have changed their name or sent a message that is unread by you
                MLog.d(TAG, "onChildChanged() dataSnapshot: " + dataSnapshot.toString());
                PrivateChatSummary privateChatSummary = getPrivateChatSummary(dataSnapshot);
                updatePrivateChatSummary(privateChatSummary);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //todo user can remove this summary
                MLog.d(TAG, "onChildRemoved() dataSnapshot: " + dataSnapshot.toString());
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
                GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                groupChatSummary.setId(Long.parseLong(dataSnapshot.getKey()));
                insertGroupChatSummary(groupChatSummary);
                addPublicGroupChatPresenceReference(groupChatSummary.getId());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                groupChatSummary.setId(Long.parseLong(dataSnapshot.getKey()));
                updateGroupChatSummary(groupChatSummary);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
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
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (o instanceof GroupChatSummary) {
                GroupChatSummary existingGroupChatSummary = (GroupChatSummary) o;
                if (existingGroupChatSummary.getId() == groupChatSummary.getId()) {
                    data.set(i, groupChatSummary);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    private void removeGroupChatSummary(GroupChatSummary groupChatSummary) {
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (o instanceof GroupChatSummary) {
                GroupChatSummary existingGroupChatSummary = (GroupChatSummary) o;
                if (existingGroupChatSummary.getId() == groupChatSummary.getId()) {
                    data.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
        }
    }

    private void insertPrivateChatSummary(PrivateChatSummary privateChatSummary) {
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (o instanceof PrivateChatHeader) {
                data.add(i + 1, privateChatSummary);
                notifyItemInserted(i + 1);
                break;
            }
        }
    }

    private void updatePrivateChatSummary(PrivateChatSummary privateChatSummary) {
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (o instanceof PrivateChatSummary) {
                PrivateChatSummary existingPrivateChatSummary = (PrivateChatSummary) o;
                if (existingPrivateChatSummary.getId().equals(privateChatSummary.getId())) {
                    data.set(i, privateChatSummary);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    private void removePrivateChatSummary(PrivateChatSummary privateChatSummary) {
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (o instanceof PrivateChatSummary) {
                PrivateChatSummary existingPrivateChatSummary = (PrivateChatSummary) o;
                if (existingPrivateChatSummary.getId().equals(privateChatSummary.getId())) {
                    data.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
        }
    }

    private void insertGroupChatSummary(GroupChatSummary groupChatSummary) {
        int groupHeaderIndex = -1;
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
        //to-do add on click listeners
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
        int viewType = getItemViewType(position);
        if (viewType == TYPE_GROUP_HEADER) {

            GroupChatHeader groupChatHeader = (GroupChatHeader) data.get(position);
            ((ChatHeaderViewHolder) holder).name.setText(groupChatHeader.name);

        } else if (viewType == TYPE_PRIVATE_HEADER) {

            PrivateChatHeader privateChatHeader = (PrivateChatHeader) data.get(position);
            ((ChatHeaderViewHolder) holder).name.setText(privateChatHeader.name);

        } else if (viewType == TYPE_GROUP_SUMMARY) {

            GroupChatSummary groupChatSummary = (GroupChatSummary) data.get(position);
            ((GroupChatSummaryViewHolder) holder).name.setText(groupChatSummary.getName());
            ((GroupChatSummaryViewHolder) holder).status.setVisibility(View.INVISIBLE);
            if (groupChatSummary.getUsersInRoomCount() > 0) {
                ((GroupChatSummaryViewHolder) holder).unreadMessageCount.setVisibility(View.VISIBLE);
                ((GroupChatSummaryViewHolder) holder).unreadMessageCount.setText(" " + groupChatSummary.getUsersInRoomCount() + " ");
            } else
                ((GroupChatSummaryViewHolder) holder).unreadMessageCount.setVisibility(View.INVISIBLE);

        } else if (viewType == TYPE_PRIVATE_SUMMARY) {

            PrivateChatSummary privateChatSummary = (PrivateChatSummary) data.get(position);
            ((PrivateChatSummaryViewHolder) holder).name.setText(privateChatSummary.getName());
            ((PrivateChatSummaryViewHolder) holder).status.setVisibility(View.VISIBLE);
            Resources res = ((PrivateChatSummaryViewHolder) holder).name.getContext().getResources();
            if (privateChatSummary.getUnreadMessageCount() > 0) {
                ((PrivateChatSummaryViewHolder) holder).unreadMessageCount.setVisibility(View.VISIBLE);
                ((PrivateChatSummaryViewHolder) holder).unreadMessageCount.setText(" " + privateChatSummary.getUnreadMessageCount() + " ");
                ((PrivateChatSummaryViewHolder) holder).name.setTextColor(res.getColor(android.R.color.white));
            } else {
                ((PrivateChatSummaryViewHolder) holder).unreadMessageCount.setVisibility(View.INVISIBLE);
                ((PrivateChatSummaryViewHolder) holder).name.setTextColor(res.getColor(R.color.defaultChatSummaryNameColor));
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
            Pair pair = publicGroupChatPresenceReferences.get(groupid);
            pair.ref.removeEventListener(pair.listener);
        }
        publicGroupChatPresenceReferences.clear();
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
                addUserToPublicGroupChat(groupid);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildChanged() dataSnapshot: " + dataSnapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "addPublicGroupChatPresenceReference() onChildRemoved() dataSnapshot: " + dataSnapshot);
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
        pair.ref.addChildEventListener(pair.listener);
        publicGroupChatPresenceReferences.put(groupid, pair);
    }

    private void removeUserFromPublicGroupChat(long groupid) {
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (o instanceof GroupChatSummary) {
                GroupChatSummary groupChatSummary = (GroupChatSummary) o;
                if (groupChatSummary.getId() == groupid && groupChatSummary.getUsersInRoomCount() > 0) {
                    groupChatSummary.setUsersInRoomCount(groupChatSummary.getUsersInRoomCount() - 1);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    private void addUserToPublicGroupChat(long groupid) {
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (o instanceof GroupChatSummary) {
                GroupChatSummary groupChatSummary = (GroupChatSummary) o;
                if (groupChatSummary.getId() == groupid) {
                    groupChatSummary.setUsersInRoomCount(groupChatSummary.getUsersInRoomCount() + 1);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    private synchronized void removePublicGroupChatPresenceReference(final long groupid) {
        Pair pair = publicGroupChatPresenceReferences.get(groupid);
        if (pair != null) {
            pair.ref.removeEventListener(pair.listener);
            publicGroupChatPresenceReferences.remove(groupid);
        }
    }
}
