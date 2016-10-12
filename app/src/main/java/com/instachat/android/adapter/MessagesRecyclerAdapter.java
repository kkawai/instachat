package com.instachat.android.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.instachat.android.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.MyApp;
import com.instachat.android.R;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.util.MLog;
import com.instachat.android.util.StringUtil;
import com.instachat.android.view.ThemedAlertDialog;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Created by kevin on 8/23/2016.
 */
public class MessagesRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> {

    public static final String TAG = "MessagesRecyclerAdapter";

    private static final int ITEM_VIEW_TYPE_STANDARD_MESSAGE = 0;
    private static final int ITEM_VIEW_TYPE_WEB_LINK = 1;

    private StorageReference mStorageRef;
    private DatabaseReference mFirebaseDatabaseReference;
    private WeakReference<Activity> mActivity;
    private ActivityState mActivityState;
    private AdapterPopulateHolderListener mAdapterPopulateHolderListener;
    private MessageTextClickedListener mMessageTextClickedListener;
    private UserClickedListener mUserClickedListener;
    private FriendlyMessageListener mFriendlyMessageListener;
    private String mDatabaseRoot;

    public MessagesRecyclerAdapter(Class modelClass, int modelLayout, Class viewHolderClass, Query ref) {
        super(modelClass, modelLayout, viewHolderClass, ref);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public void setDatabaseRoot(String root) {
        mDatabaseRoot = root;
    }

    public void setActivity(@NonNull Activity activity, @NonNull ActivityState activityState) {
        mActivity = new WeakReference<>(activity);
        mActivityState = activityState;
    }

    public void setAdapterPopulateHolderListener(AdapterPopulateHolderListener listener) {
        mAdapterPopulateHolderListener = listener;
    }

    public void setMessageTextClickedListener(MessageTextClickedListener listener) {
        mMessageTextClickedListener = listener;
    }

    public void setUserThumbClickedListener(UserClickedListener listener) {
        mUserClickedListener = listener;
    }

    public void setFriendlyMessageListener(FriendlyMessageListener listener) {
        mFriendlyMessageListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        FriendlyMessage friendlyMessage = getItem(position);
        String text = friendlyMessage.getText() + "";
        if (URLUtil.isHttpUrl(text) || URLUtil.isHttpsUrl(text)) {
            return ITEM_VIEW_TYPE_WEB_LINK;
        }
        return ITEM_VIEW_TYPE_STANDARD_MESSAGE;
    }

    private MessageViewHolder createMessageViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_WEB_LINK) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_content, parent, false);
            final MessageViewHolder holder = new MessageViewHolder(view);
            View.OnClickListener webLinkClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final FriendlyMessage msg = getItem(holder.getAdapterPosition());
                    final Uri uri = Uri.parse(msg.getText());
                    mActivity.get().startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            };
            holder.webLinkContent.setOnClickListener(webLinkClickListener);
            holder.webLinkUrl.setOnClickListener(webLinkClickListener);
            holder.webLinkTitle.setOnClickListener(webLinkClickListener);
            holder.webLinkDescription.setOnClickListener(webLinkClickListener);
            holder.webLinkImageView.setOnClickListener(webLinkClickListener);
            return holder;
        } else {
            MessageViewHolder holder = super.onCreateViewHolder(parent, viewType);
            return holder;
        }
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        final MessageViewHolder holder = createMessageViewHolder(parent, viewType);
        holder.messengerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());
                if (TextUtils.isEmpty(friendlyMessage.getName())) {
                    return;
                }
                mUserClickedListener.onUserClicked(friendlyMessage.getUserid());
            }
        });
        final View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());
                if (TextUtils.isEmpty(friendlyMessage.getName())) {
                    return;
                }
                mMessageTextClickedListener.onMessageClicked(holder.getAdapterPosition());
            }
        };
        holder.messageTimeTextView.setOnClickListener(onClickListener);
        if (holder.messageTextView != null)
            holder.messageTextView.setOnClickListener(onClickListener);
        holder.messengerTextView.setOnClickListener(onClickListener);

        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());
                if (TextUtils.isEmpty(friendlyMessage.getName())) {
                    return true;
                }
                new ThemedAlertDialog.Builder(view.getContext()).setMessage(MyApp.getInstance().getString(R.string.delete_message_question) + limitString(getItem(holder.getAdapterPosition()).getText(), 15)).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final FriendlyMessage msg = getItem(holder.getAdapterPosition());
                        MLog.d(TAG, " msg.getImageUrl(): " + msg.getImageUrl() + " " + msg.getImageId());
                        if (msg.getImageUrl() != null && msg.getImageId() != null) {
                            final StorageReference photoRef = mStorageRef.child(mDatabaseRoot).child(msg.getImageId());
                            photoRef.delete();
                            MLog.d(TAG, "deleted photo " + msg.getImageId());
                        }
                        DatabaseReference ref = mFirebaseDatabaseReference.child(mDatabaseRoot + "/" + msg.getId());
                        if (ref != null)
                            ref.removeValue();
                    }
                }).setNegativeButton(android.R.string.no, null).setCancelable(true).setOnCancelListener(null).create().show();
                return true;
            }
        };

        holder.messageTimeTextView.setOnLongClickListener(onLongClickListener);
        if (holder.messageTextView != null)
            holder.messageTextView.setOnLongClickListener(onLongClickListener);
        holder.messengerTextView.setOnLongClickListener(onLongClickListener);
        if (holder.webLinkContent != null) {
            holder.webLinkContent.setOnLongClickListener(onLongClickListener);
            holder.webLinkUrl.setOnLongClickListener(onLongClickListener);
            holder.webLinkTitle.setOnLongClickListener(onLongClickListener);
            holder.webLinkDescription.setOnLongClickListener(onLongClickListener);
            holder.webLinkImageView.setOnLongClickListener(onLongClickListener);
        }
        if (holder.messagePhotoView != null) {
            holder.messagePhotoView.setOnClickListener(onClickListener);
            holder.messagePhotoView.setOnLongClickListener(onLongClickListener);
        }
        return holder;
    }

    @Override
    protected void populateViewHolder(final MessageViewHolder viewHolder, final FriendlyMessage friendlyMessage, int position) {

        mAdapterPopulateHolderListener.onViewHolderPopulated();

        if (getItemViewType(position) == ITEM_VIEW_TYPE_WEB_LINK) {
            new WebLinkHelper().populateWebLinkPost(viewHolder, friendlyMessage, position);
        }

        if (viewHolder.messageTextView != null) {
            if (StringUtil.isNotEmpty(friendlyMessage.getText()))
                viewHolder.messageTextView.setText(friendlyMessage.getText());
            else
                viewHolder.messageTextView.setText("");
        }

        viewHolder.messengerTextView.setText(friendlyMessage.getName());
        if (friendlyMessage.getTime() != 0) {
            viewHolder.messageTimeTextView.setVisibility(View.VISIBLE);
            viewHolder.messageTimeTextView.setText(StringUtil.getHour(friendlyMessage.getTime()));
        } else {
            viewHolder.messageTimeTextView.setVisibility(View.INVISIBLE);
        }

        if (viewHolder.messagePhotoView != null) {
            if (friendlyMessage.getImageUrl() != null) {
                Glide.with(mActivity.get()).load(friendlyMessage.getImageUrl()).crossFade().into(viewHolder.messagePhotoView);
                viewHolder.messagePhotoView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.messagePhotoView.setVisibility(View.GONE);
            }
        }

        if (TextUtils.isEmpty(friendlyMessage.getName())) {
            viewHolder.messengerImageView.setVisibility(View.GONE);
        } else {
            viewHolder.messengerImageView.setVisibility(View.VISIBLE);
            Constants.DP_URL(friendlyMessage.getUserid(), friendlyMessage.getDpid(), new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (mActivityState.isActivityDestroyed())
                        return;
                    try {
                        if (!task.isSuccessful()) {
                            viewHolder.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp);
                            return;
                        }
                        Glide.with(mActivity.get()).load(task.getResult().toString()).error(R.drawable.ic_account_circle_black_36dp).into(viewHolder.messengerImageView);
                    } catch (final Exception e) {
                        MLog.e(TAG, "Constants.DP_URL user dp doesn't exist in google cloud storage.  task: " + task.isSuccessful());
                        viewHolder.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp);
                    }
                }
            });
        }

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
        super.cleanup();
        if (mActivity != null)
            mActivity.clear();
        mActivity = null;
        mUserClickedListener = null;
        mAdapterPopulateHolderListener = null;
        mMessageTextClickedListener = null;
    }

    public void sendFriendlyMessage(final FriendlyMessage friendlyMessage) {

        if (Constants.IS_SUPPORT_MESSAGE_APPENDING) {
            final int itemCount = getItemCount();
            if (itemCount > 0) {
                final FriendlyMessage lastFriendlyMessage = getItem(itemCount - 1);
                if (friendlyMessage.getUserid() == lastFriendlyMessage.getUserid()) {

                    if (lastFriendlyMessage.append(friendlyMessage)) {
                        friendlyMessage.setId(lastFriendlyMessage.getId());
                        mFirebaseDatabaseReference.child(mDatabaseRoot).child(lastFriendlyMessage.getId()).updateChildren(FriendlyMessage.toMap(lastFriendlyMessage)).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    friendlyMessage.setId(UUID.randomUUID().toString());
                                    mFriendlyMessageListener.onFriendlyMessageSuccess(friendlyMessage);
                                } else {
                                    mFriendlyMessageListener.onFriendlyMessageFail(friendlyMessage);
                                }
                            }
                        });
                        return;
                    }
                }
            }
        }

        final String key = mFirebaseDatabaseReference.child(mDatabaseRoot).push().getKey();
        friendlyMessage.setId(key);

        mFirebaseDatabaseReference.child(mDatabaseRoot).child(friendlyMessage.getId()).setValue(friendlyMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    mFriendlyMessageListener.onFriendlyMessageSuccess(friendlyMessage);
                } else {
                    mFriendlyMessageListener.onFriendlyMessageFail(friendlyMessage);
                }
            }
        });
    }
}
