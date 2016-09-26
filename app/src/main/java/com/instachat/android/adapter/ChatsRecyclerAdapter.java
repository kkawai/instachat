package com.instachat.android.adapter;

import android.app.Activity;
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
import com.instachat.android.PrivateChatActivity;
import com.instachat.android.R;
import com.instachat.android.model.GroupChatHeader;
import com.instachat.android.model.GroupChatSummary;
import com.instachat.android.model.PrivateChatHeader;
import com.instachat.android.model.PrivateChatSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 9/26/2016.
 * <p/>
 * Encapsulates a combination of PrivateChatSummary,
 * GroupChatSummary, PrivateChatHeader, and GroupChatHeader
 * objects in an array list;
 */
public class ChatsRecyclerAdapter extends RecyclerView.Adapter {

    private static final int TYPE_GROUP_SUMMARY = 0;
    private static final int TYPE_PRIVATE_SUMMARY = 1;
    private static final int TYPE_PRIVATE_HEADER = 2;
    private static final int TYPE_GROUP_HEADER = 3;

    private List<Object> data = new ArrayList<>(40);
    private ChildEventListener listener;
    private DatabaseReference databaseReference;
    private Activity activity;
    private ChatsItemClickedListener chatsItemClickedListener;

    public ChatsRecyclerAdapter(Activity activity, ChatsItemClickedListener chatsItemClickedListener) {
        this.activity = activity;
        this.chatsItemClickedListener = chatsItemClickedListener;
    }

    public void populateData() {

        data.add(new GroupChatHeader(MyApp.getInstance().getString(R.string.group_chat_header)));
        data.add(new PrivateChatHeader(MyApp.getInstance().getString(R.string.private_chat_header)));
        databaseReference = FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHATS_SUMMARY_PARENT_REF());
        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                PrivateChatSummary privateChatSummary = PrivateChatSummary.fromDataSnapshot(dataSnapshot);
                insertPrivateChatSummary(privateChatSummary);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //todo to-user might have changed their name
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //todo user can remove this summary
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
        databaseReference.addChildEventListener(listener);
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

    private void insertGroupChatSummary(GroupChatSummary groupChatSummary) {
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (o instanceof GroupChatHeader) {
                data.add(i + 1, groupChatSummary);
                notifyItemInserted(i + 1);
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
            GroupChatSummaryViewHolder holder = new GroupChatSummaryViewHolder(view);
            return holder;
        } else if (viewType == TYPE_PRIVATE_SUMMARY) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_list_item, parent, false);
            final PrivateChatSummaryViewHolder holder = new PrivateChatSummaryViewHolder(view);
            holder.name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PrivateChatSummary privateChatSummary = (PrivateChatSummary) data.get(holder.getAdapterPosition());
                    PrivateChatActivity.startPrivateChatActivity(activity, Integer.parseInt(privateChatSummary.getId()));
                    chatsItemClickedListener.onNameClicked();
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

        } else if (viewType == TYPE_PRIVATE_SUMMARY) {

            PrivateChatSummary privateChatSummary = (PrivateChatSummary) data.get(position);
            ((PrivateChatSummaryViewHolder) holder).name.setText(privateChatSummary.getName());
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void cleanup() {
        databaseReference.removeEventListener(listener);
    }
}
