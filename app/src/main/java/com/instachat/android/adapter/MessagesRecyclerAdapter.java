package com.instachat.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.instachat.android.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.blocks.BlockedUser;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.options.MessageOptionsDialogHelper;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.StringUtil;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 8/23/2016.
 */
public class MessagesRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends BaseMessagesAdapter<FriendlyMessage, MessageViewHolder> {

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
        listenForBlockedUsers();
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

    public void blockUser(int userid) {
        synchronized (this) {
            int count = getData().size() - 1;
            for (int i = count; i >= 0; i--) {
                FriendlyMessage m = getItem(i);
                if (m.getUserid() == userid) {
                    removeItemLocally(i);
                }
            }
        }
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
                mUserClickedListener.onUserClicked(friendlyMessage.getUserid(), friendlyMessage.getName());
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
                new MessageOptionsDialogHelper().showMessageOptions(mActivity.get(), friendlyMessage, new MessageOptionsDialogHelper.MessageOptionsListener() {
                    @Override
                    public void onCopyTextRequested(FriendlyMessage friendlyMessage) {
                        final ClipboardManager cm = (ClipboardManager) mActivity.get()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setText(friendlyMessage.getText());
                        Toast.makeText(mActivity.get(), R.string.message_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDeleteMessageRequested(final FriendlyMessage friendlyMessage) {
                        MLog.d(TAG, " msg.getImageUrl(): " + friendlyMessage.getImageUrl() + " " + friendlyMessage.getImageId());
                        new SweetAlertDialog(mActivity.get(), SweetAlertDialog.WARNING_TYPE)
                                .setTitleText(mActivity.get().getString(R.string.message_delete_title))
                                .setContentText(mActivity.get().getString(R.string.message_delete_question))
                                .setCancelText(mActivity.get().getString(android.R.string.no))
                                .setConfirmText(mActivity.get().getString(android.R.string.yes))
                                .showCancelButton(true)
                                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                        sweetAlertDialog.cancel();
                                    }
                                }).setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {

                                sweetAlertDialog.dismiss();
                                if (friendlyMessage.getImageUrl() != null && friendlyMessage.getImageId() != null) {
                                    final StorageReference photoRef = mStorageRef.child(mDatabaseRoot).child(friendlyMessage.getImageId());
                                    photoRef.delete();
                                    MLog.d(TAG, "deleted photo " + friendlyMessage.getImageId());
                                }
                                removeItemRemotely(mDatabaseRoot + "/" + friendlyMessage.getId(), new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            new SweetAlertDialog(mActivity.get(), SweetAlertDialog.SUCCESS_TYPE)
                                                    .setTitleText(mActivity.get().getString(R.string.success_exclamation))
                                                    .setContentText(mActivity.get().getString(R.string.message_delete_success))
                                                    .show();
                                        } else {
                                            new SweetAlertDialog(mActivity.get(), SweetAlertDialog.ERROR_TYPE)
                                                    .setTitleText(mActivity.get().getString(R.string.oops_exclamation))
                                                    .setContentText(mActivity.get().getString(R.string.message_delete_failed))
                                                    .show();
                                        }
                                    }
                                });
                            }
                        }).show();
                    }

                    @Override
                    public void onBlockPersonRequested(final FriendlyMessage friendlyMessage) {
                        if (Preferences.getInstance().getUserId() == friendlyMessage.getUserid()) {
                            Toast.makeText(mActivity.get(), R.string.block_cannot_block_yourself, Toast.LENGTH_SHORT).show();
                            return; //cannot block yourself dummy!
                        }
                        new SweetAlertDialog(mActivity.get(), SweetAlertDialog.WARNING_TYPE)
                                .setTitleText(mActivity.get().getString(R.string.block_person_title, friendlyMessage.getName()))
                                .setContentText(mActivity.get().getString(R.string.block_person_question, friendlyMessage.getName()))
                                .setCancelText(mActivity.get().getString(android.R.string.no))
                                .setConfirmText(mActivity.get().getString(android.R.string.yes))
                                .showCancelButton(true)
                                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                        sweetAlertDialog.cancel();
                                    }
                                }).setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {

                                sweetAlertDialog.dismiss();
                                Map<String, Object> map = new HashMap<>(2);
                                map.put("name", friendlyMessage.getName());
                                if (!TextUtils.isEmpty(friendlyMessage.getDpid()))
                                    map.put("dpid", friendlyMessage.getDpid());
                                mFirebaseDatabaseReference.child(Constants.BLOCKS_REF()).
                                        child(friendlyMessage.getUserid() + "").updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            new SweetAlertDialog(mActivity.get(), SweetAlertDialog.SUCCESS_TYPE)
                                                    .setTitleText(mActivity.get().getString(R.string.success_exclamation))
                                                    .setContentText(mActivity.get().getString(R.string.block_person_success, friendlyMessage.getName()))
                                                    .show();
                                            blockUser(friendlyMessage.getUserid());
                                        } else {
                                            new SweetAlertDialog(mActivity.get(), SweetAlertDialog.ERROR_TYPE)
                                                    .setTitleText(mActivity.get().getString(R.string.oops_exclamation))
                                                    .setContentText(mActivity.get().getString(R.string.block_person_failed, friendlyMessage.getName()))
                                                    .show();
                                        }
                                    }
                                });
                            }
                        }).show();
                    }

                    @Override
                    public void onReportPersonRequested(FriendlyMessage friendlyMessage) {
                        if (Preferences.getInstance().getUserId() == friendlyMessage.getUserid()) {
                            //todo
                            return; //cannot report yourself dummy!
                        }
                    }
                });
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
        mActivityState = null;
        mUserClickedListener = null;
        mAdapterPopulateHolderListener = null;
        mMessageTextClickedListener = null;
        mFirebaseDatabaseReference.child(Constants.BLOCKS_REF()).removeEventListener(mBlockedUsersListener);
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

    @Override
    protected boolean isNewItemAllowed(FriendlyMessage model) {
        return !mBlockedUsers.containsKey(model.getUserid());
    }

    private ChildEventListener mBlockedUsersListener;
    private Map<Integer, Boolean> mBlockedUsers = new Hashtable<>(20);

    private void listenForBlockedUsers() {
        mBlockedUsersListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                BlockedUser blockedUser = dataSnapshot.getValue(BlockedUser.class);
                blockedUser.id = Integer.parseInt(dataSnapshot.getKey());
                mBlockedUsers.put(blockedUser.id, false);
                blockUser(blockedUser.id);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                BlockedUser blockedUser = dataSnapshot.getValue(BlockedUser.class);
                blockedUser.id = Integer.parseInt(dataSnapshot.getKey());
                mBlockedUsers.remove(blockedUser.id);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mFirebaseDatabaseReference.child(Constants.BLOCKS_REF()).addChildEventListener(mBlockedUsersListener);
    }
}
