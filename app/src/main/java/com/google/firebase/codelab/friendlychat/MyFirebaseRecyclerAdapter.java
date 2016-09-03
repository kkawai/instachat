package com.google.firebase.codelab.friendlychat;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.initech.Constants;
import com.initech.MyApp;
import com.initech.util.MLog;
import com.initech.util.Preferences;
import com.initech.util.StringUtil;
import com.initech.view.ThemedAlertDialog;

import java.lang.ref.WeakReference;

/**
 * Created by kevin on 8/23/2016.
 */
public class MyFirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> extends FirebaseRecyclerAdapter<com.google.firebase.codelab.friendlychat.FriendlyMessage, com.google.firebase.codelab.friendlychat.MessageViewHolder> {

    public static final String TAG = "MyFirebaseRecyclerAdapter";

    private StorageReference mStorageRef;
    private DatabaseReference mFirebaseDatabaseReference;
    private WeakReference<Activity> mActivity;
    private AdapterPopulateHolderListener mAdapterPopulateHolderListener;
    private MessageTextClickedListener mMessageTextClickedListener;

    public MyFirebaseRecyclerAdapter(Class modelClass, int modelLayout, Class viewHolderClass, DatabaseReference ref) {
        super(modelClass, modelLayout, viewHolderClass, ref);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public void setActivity(Activity activity) {
        mActivity = new WeakReference<>(activity);
    }
    public void setAdapterPopulateHolderListener(AdapterPopulateHolderListener listener) {
        mAdapterPopulateHolderListener = listener;
    }
    public void setMessageTextClickedListener(MessageTextClickedListener listener) {
        mMessageTextClickedListener = listener;
    }

    @Override
    public com.google.firebase.codelab.friendlychat.MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final com.google.firebase.codelab.friendlychat.MessageViewHolder holder = super.onCreateViewHolder(parent, viewType);
        holder.messengerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final com.google.firebase.codelab.friendlychat.FriendlyMessage msg = getItem(holder.getAdapterPosition());
                Toast.makeText(v.getContext(), "clicked on profile: " + msg.getName() + " said: " + msg.getText(), Toast.LENGTH_SHORT).show();
            }
        });
        final View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMessageTextClickedListener.onMessageClicked(holder.getAdapterPosition());
            }
        };
        holder.messageTimeTextView.setOnClickListener(onClickListener);
        holder.messageTextView.setOnClickListener(onClickListener);
        holder.messengerTextView.setOnClickListener(onClickListener);

        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new ThemedAlertDialog.Builder(view.getContext())
                        .setMessage(MyApp.getInstance().getString(R.string.delete_message_question) + " (" + limitString(getItem(holder.getAdapterPosition()).getText(), 15) + ")")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                final com.google.firebase.codelab.friendlychat.FriendlyMessage msg = getItem(holder.getAdapterPosition());
                                MLog.d(TAG, " msg.getImageUrl(): " + msg.getImageUrl() + " " + msg.getImageId());
                                if (msg.getImageUrl() != null && msg.getImageId() != null) {
                                    final StorageReference photoRef = mStorageRef.child("photos")
                                            .child(msg.getImageId());
                                    photoRef.delete();
                                    MLog.d(TAG, "deleted photo " + msg.getImageId());
                                }
                                DatabaseReference ref = mFirebaseDatabaseReference.child(Constants.MESSAGES_CHILD + "/" + msg.getId());
                                if (ref != null)
                                    ref.removeValue();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setCancelable(true).setOnCancelListener(null)
                        .create().show();
                return false;
            }
        };

        holder.messageTimeTextView.setOnLongClickListener(onLongClickListener);
        holder.messageTextView.setOnLongClickListener(onLongClickListener);
        holder.messengerTextView.setOnLongClickListener(onLongClickListener);
        return holder;
    }

    @Override
    protected void populateViewHolder(com.google.firebase.codelab.friendlychat.MessageViewHolder viewHolder, com.google.firebase.codelab.friendlychat.FriendlyMessage friendlyMessage, int position) {

        mAdapterPopulateHolderListener.onViewHolderPopulated();
        if (friendlyMessage.getText() != null)
            viewHolder.messageTextView.setText(friendlyMessage.getText());
        viewHolder.messengerTextView.setText(friendlyMessage.getName());
        if (friendlyMessage.getTime() != 0) {
            viewHolder.messageTimeTextView.setVisibility(View.VISIBLE);
            viewHolder.messageTimeTextView.setText(StringUtil.getHour(friendlyMessage.getTime()));
        } else {
            viewHolder.messageTimeTextView.setVisibility(View.INVISIBLE);
        }
//                if (friendlyMessage.getPhotoUrl() == null) {
//                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
//                            R.drawable.ic_account_circle_black_36dp));
//                } else {
//
//                }
        if (friendlyMessage.getImageUrl() != null) {
            Glide.with(mActivity.get())
                    .load(friendlyMessage.getImageUrl())
                    .crossFade()
                    .into(viewHolder.messagePhotoView);
            viewHolder.messagePhotoView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.messagePhotoView.setVisibility(View.GONE);
        }

        Glide.with(mActivity.get())
                .load(Constants.DP_URL(Preferences.getInstance(mActivity.get()).getUserId()))
                .error(R.drawable.ic_account_circle_black_36dp)
                .into(viewHolder.messengerImageView);

    }

    @Override
    protected com.google.firebase.codelab.friendlychat.FriendlyMessage parseSnapshot(DataSnapshot snapshot) {
        com.google.firebase.codelab.friendlychat.FriendlyMessage friendlyMessage = super.parseSnapshot(snapshot);
        friendlyMessage.setId(snapshot.getKey());
        return friendlyMessage;
    }

    private String limitString(final String s, final int limit) {
        if (s == null || s.length() <= limit) {
            return s;
        }
        return s.substring(0, limit) + "...";
    }
}
