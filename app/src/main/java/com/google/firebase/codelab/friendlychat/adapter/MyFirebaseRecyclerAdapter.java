package com.google.firebase.codelab.friendlychat.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.codelab.friendlychat.R;
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.initech.Constants;
import com.initech.MyApp;
import com.initech.util.MLog;
import com.initech.util.StringUtil;
import com.initech.view.ThemedAlertDialog;

import java.lang.ref.WeakReference;

/**
 * Created by kevin on 8/23/2016.
 */
public class MyFirebaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> {

    public static final String TAG = "MyFirebaseRecyclerAdapter";

    private StorageReference mStorageRef;
    private DatabaseReference mFirebaseDatabaseReference;
    private WeakReference<Activity> mActivity;
    private AdapterPopulateHolderListener mAdapterPopulateHolderListener;
    private MessageTextClickedListener mMessageTextClickedListener;
    private UserThumbClickedListener mUserThumbClickedListener;
    private String mDatabaseChild;

    public MyFirebaseRecyclerAdapter(Class modelClass, int modelLayout, Class viewHolderClass, DatabaseReference ref) {
        super(modelClass, modelLayout, viewHolderClass, ref);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public void setDatabaseChild(String child) {
        mDatabaseChild = child;
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

    public void setUserThumbClickedListener(UserThumbClickedListener listener) {
        mUserThumbClickedListener = listener;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final MessageViewHolder holder = super.onCreateViewHolder(parent, viewType);
        holder.messengerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final FriendlyMessage msg = getItem(holder.getAdapterPosition());
                mUserThumbClickedListener.onUserThumbClicked(holder.messengerImageView, msg);
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
                        .setMessage(MyApp.getInstance().getString(R.string.delete_message_question) + limitString(getItem(holder.getAdapterPosition()).getText(), 15))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                final FriendlyMessage msg = getItem(holder.getAdapterPosition());
                                MLog.d(TAG, " msg.getImageUrl(): " + msg.getImageUrl() + " " + msg.getImageId());
                                if (msg.getImageUrl() != null && msg.getImageId() != null) {
                                    final StorageReference photoRef = mStorageRef.child(Constants.PHOTOS_CHILD)
                                            .child(msg.getImageId());
                                    photoRef.delete();
                                    MLog.d(TAG, "deleted photo " + msg.getImageId());
                                }
                                DatabaseReference ref = mFirebaseDatabaseReference.child(mDatabaseChild + "/" + msg.getId());
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
    protected void populateViewHolder(final MessageViewHolder viewHolder, final FriendlyMessage friendlyMessage, int position) {

        mAdapterPopulateHolderListener.onViewHolderPopulated();
        if (StringUtil.isNotEmpty(friendlyMessage.getText()))
            viewHolder.messageTextView.setText(friendlyMessage.getText());
        else
            viewHolder.messageTextView.setText("");
        viewHolder.messengerTextView.setText(friendlyMessage.getName());
        if (friendlyMessage.getTime() != 0) {
            viewHolder.messageTimeTextView.setVisibility(View.VISIBLE);
            viewHolder.messageTimeTextView.setText(StringUtil.getHour(friendlyMessage.getTime()));
        } else {
            viewHolder.messageTimeTextView.setVisibility(View.INVISIBLE);
        }
//                if (friendlyMessage.getPhotoUrl() == null) {
//                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(GroupChatActivity.this,
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

        Constants.DP_URL(friendlyMessage.getUserid(), friendlyMessage.getDpid(), new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (isActivityDestroyed())
                    return;
                try {
                    if (!task.isSuccessful()) {
                        viewHolder.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp);
                        return;
                    }
                    Glide.with(mActivity.get())
                            .load(task.getResult().toString())
                            .error(R.drawable.ic_account_circle_black_36dp)
                            .into(viewHolder.messengerImageView);
                } catch (final Exception e) {
                    MLog.e(TAG, "Constants.DP_URL user dp doesn't exist in google cloud storage.  task: " + task.isSuccessful());
                    viewHolder.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp);
                }
            }
        });

    }

    @Override
    protected FriendlyMessage parseSnapshot(DataSnapshot snapshot) {
        FriendlyMessage friendlyMessage = super.parseSnapshot(snapshot);
        friendlyMessage.setId(snapshot.getKey());
        return friendlyMessage;
    }

    private String limitString(final String s, final int limit) {
        if (StringUtil.isEmpty(s)) {
            return "";
        }
        if (s.length() <= limit) {
            return " (" + s + ")";
        }
        return " (" + s.substring(0, limit) + "...)";
    }

    public void cleanup() {
        if (mActivity != null)
            mActivity.clear();
        mActivity = null;
        mUserThumbClickedListener = null;
        mAdapterPopulateHolderListener = null;
        mMessageTextClickedListener = null;
    }

    private boolean isActivityDestroyed() {
        return (mActivity == null || mActivity.get() == null || mActivity.get().isFinishing());
    }
}
