package com.instachat.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.ath.fuel.FuelInjector;
import com.ath.fuel.Lazy;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.instachat.android.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.MyApp;
import com.instachat.android.R;
import com.instachat.android.blocks.BlockUserDialogHelper;
import com.instachat.android.blocks.BlockedUser;
import com.instachat.android.blocks.BlockedUserListener;
import com.instachat.android.likes.LikesHelper;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.options.MessageOptionsDialogHelper;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.TimeUtil;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private BlockedUserListener mBlockedUserListener;
    private String mDatabaseRef;
    private FrameLayout mEntireScreenFrameLayout;
    private int mMaxPeriscopesPerItem;

    public MessagesRecyclerAdapter(Class modelClass, int modelLayout, Class viewHolderClass, Query ref) {
        super(modelClass, modelLayout, viewHolderClass, ref);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        listenForBlockedUsers();
        mMaxPeriscopesPerItem = (int) FirebaseRemoteConfig.getInstance().getLong(Constants.KEY_MAX_PERISCOPABLE_LIKES_PER_ITEM);
    }

    public void setDatabaseRoot(String root) {
        mDatabaseRef = root;
    }

    public void setActivity(@NonNull Activity activity,
                            @NonNull ActivityState activityState,
                            @NonNull FrameLayout entireScreenLayout) {
        FuelInjector.ignite(activity, this);
        mActivity = new WeakReference<>(activity);
        mActivityState = activityState;
        mEntireScreenFrameLayout = entireScreenLayout;
        mEntireScreenFrameLayout.setOnTouchListener(mOnTouchListener);
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

    public void setBlockedUserListener(BlockedUserListener listener) {
        mBlockedUserListener = listener;
    }

    private BlockedUserListener mInternalBlockedUserListener = new BlockedUserListener() {
        @Override
        public void onUserBlocked(int userid) {
            blockUser(userid);
            mBlockedUserListener.onUserBlocked(userid);
        }
    };

    @Override
    public int getItemViewType(int position) {
        FriendlyMessage friendlyMessage = getItem(position);
        String text = friendlyMessage.getText() + "";

        if (URLUtil.isHttpUrl(text) || URLUtil.isHttpsUrl(text)) {
            return ITEM_VIEW_TYPE_WEB_LINK;
        }
        return ITEM_VIEW_TYPE_STANDARD_MESSAGE;
    }

    private void blockUser(int userid) {
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
        if (viewType == ITEM_VIEW_TYPE_STANDARD_MESSAGE) {
            return super.onCreateViewHolder(parent, viewType);
        } else if (viewType == ITEM_VIEW_TYPE_WEB_LINK) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_web_clipping, parent, false);
            return new MessageViewHolder(view);
        }
        throw new IllegalArgumentException("unknown viewType");
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        final MessageViewHolder holder = createMessageViewHolder(parent, viewType);

        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());
                if (TextUtils.isEmpty(friendlyMessage.getName())) {
                    return true;
                }
                final View tempAnchorView = new View(mActivity.get());
                tempAnchorView.setBackgroundColor(Color.TRANSPARENT);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(10, 10);
                params.leftMargin = (int) x;
                params.topMargin = (int) y;
                mEntireScreenFrameLayout.addView(tempAnchorView, params);
                mEntireScreenFrameLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        showMessageOptions(tempAnchorView, friendlyMessage);
                    }
                });
                return true;
            }
        };

        holder.messengerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());
                if (TextUtils.isEmpty(friendlyMessage.getName())) {
                    return;
                }
                mUserClickedListener.onUserClicked(friendlyMessage.getUserid(), friendlyMessage.getName(), friendlyMessage.getDpid());
            }
        });

        if (viewType == ITEM_VIEW_TYPE_STANDARD_MESSAGE) {
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
            holder.messageTextParent.setOnClickListener(onClickListener);
            holder.messageTextParent.setOnLongClickListener(onLongClickListener);
        } else if (viewType == ITEM_VIEW_TYPE_WEB_LINK) {
            View.OnClickListener webLinkClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final FriendlyMessage msg = getItem(holder.getAdapterPosition());
                    final Uri uri = Uri.parse(msg.getText());
                    mActivity.get().startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            };
            holder.webLinkParent.setOnClickListener(webLinkClickListener);
            holder.webLinkParent.setOnLongClickListener(onLongClickListener);
        } else {
            throw new IllegalArgumentException("unknown viewType");
        }

        holder.likesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());
                //friendlyMessage.incrementLikesCount();
                DatabaseReference ref = mFirebaseDatabaseReference.child(mDatabaseRef).child(friendlyMessage.getId()).child(Constants.CHILD_LIKES);
                LikesHelper.getInstance().likeFriendlyMessage(friendlyMessage, ref);
            }
        });

        return holder;
    }

    @Override
    protected void populateViewHolder(final MessageViewHolder viewHolder, final FriendlyMessage friendlyMessage,
                                      int position, List<Object> payloads) {

        if (friendlyMessage.getLikes() > 0) {
            viewHolder.periscopeParent.setVisibility(View.VISIBLE);
            final int consumedLikes = MyApp.getInstance().getMap().getConsumedLikesMap().containsKey(friendlyMessage.getId()) ?
                    MyApp.getInstance().getMap().getConsumedLikesMap().get(friendlyMessage.getId()) : 0;
            final int likesToDisplay = friendlyMessage.getLikes() - consumedLikes;
            final int count = likesToDisplay > mMaxPeriscopesPerItem ? mMaxPeriscopesPerItem : likesToDisplay;
            if (count > 0) { //only show likes periscope for likes that have not been consumed yet
                viewHolder.periscopeLayout.setVisibility(View.VISIBLE);
                viewHolder.periscopeParent.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mActivityState == null || mActivityState.isActivityDestroyed())
                            return;
                        for (int i = 0; i < count; i++) {
                            viewHolder.periscopeLayout.addHeart();
                        }
                        MyApp.getInstance().getMap().getConsumedLikesMap().put(friendlyMessage.getId(), friendlyMessage.getLikes());
                    }
                }, 500);
            } else {
                viewHolder.periscopeLayout.setVisibility(View.INVISIBLE);
            }
            viewHolder.likesButton.setChecked(false);
            viewHolder.likesButton.setBtnColor(viewHolder.likesButton.getContext().getResources().getColor(R.color.chat_like_button_active_state));
            viewHolder.likesCount.setText(" " + friendlyMessage.getLikes() + " ");

        } else {
            viewHolder.periscopeParent.setVisibility(View.GONE);
            viewHolder.likesButton.setBtnColor(viewHolder.likesButton.getContext().getResources().getColor(R.color.chat_like_button_inactive_state));
            viewHolder.likesButton.setChecked(false);
        }

        /**
         * Optimization hack!!
         * If all I did, as a user, was click on the like button.
         * If that's the case, then update just that part and get out
         * because nothing else in the view has been altered.
         * See where notifyItemChanged(position,0) is called to see what
         * is going on there.
         */
        if (payloads != null && payloads.size() != 0) {
            return;
        }

        mAdapterPopulateHolderListener.onViewHolderPopulated();

        if (getItemViewType(position) == ITEM_VIEW_TYPE_WEB_LINK) {
            new WebLinkHelper().populateWebLinkPost(viewHolder, friendlyMessage, position);
        } else {
            if (viewHolder.messageTextView != null) {
                if (StringUtil.isNotEmpty(friendlyMessage.getText()))
                    viewHolder.messageTextView.setText(friendlyMessage.getText());
                else
                    viewHolder.messageTextView.setText("");
            }
        }

        viewHolder.messengerTextView.setText(friendlyMessage.getName());
        if (friendlyMessage.getTime() != 0) {
            viewHolder.messageTimeTextView.setVisibility(View.VISIBLE);
            viewHolder.messageTimeTextView.setText(TimeUtil.getTimeAgo(friendlyMessage.getTime()));
        } else {
            viewHolder.messageTimeTextView.setVisibility(View.INVISIBLE);
        }

        if (viewHolder.messagePhotoViewParent != null) {
            if (friendlyMessage.getImageUrl() != null) {
                Glide.with(mActivity.get()).load(friendlyMessage.getImageUrl()).crossFade().into(viewHolder.messagePhotoView);
                viewHolder.messagePhotoViewParent.setVisibility(View.VISIBLE);
                viewHolder.messagePhotoView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.messagePhotoViewParent.setVisibility(View.GONE);
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
                    if (mActivityState == null || mActivityState.isActivityDestroyed())
                        return;
                    try {
                        if (!task.isSuccessful()) {
                            //viewHolder.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp);
                            return;
                        }
                        Glide.with(mActivity.get()).load(task.getResult().toString()).error(R.drawable.ic_anon_person_36dp).into(viewHolder.messengerImageView);
                    } catch (final Exception e) {
                        MLog.e(TAG, "Constants.DP_URL user dp doesn't exist in google cloud storage.  task: " + task.isSuccessful());
                        viewHolder.messengerImageView.setImageResource(R.drawable.ic_anon_person_36dp);
                    }
                }
            });
        }
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
        mBlockedUserListener = null;
        mFirebaseDatabaseReference.child(Constants.MY_BLOCKS_REF()).removeEventListener(mBlockedUsersListener);
    }

    public void sendFriendlyMessage(final FriendlyMessage friendlyMessage) {

        if (Constants.IS_SUPPORT_MESSAGE_APPENDING) {
            final int itemCount = getItemCount();
            if (itemCount > 0) {
                final FriendlyMessage lastFriendlyMessage = getItem(itemCount - 1);
                if (friendlyMessage.getUserid() == lastFriendlyMessage.getUserid()) {

                    if (lastFriendlyMessage.append(friendlyMessage)) {
                        friendlyMessage.setId(lastFriendlyMessage.getId());

                        /**
                         * now check if the user sending this message has changed their name
                         * or user profile pic.  if so, change the last friendly message
                         * so that it gets updated.
                         */
                        if (!friendlyMessage.getName().equals(lastFriendlyMessage.getName())) {
                            lastFriendlyMessage.setName(friendlyMessage.getName());
                        }
                        String lastdpid = lastFriendlyMessage.getDpid() + "";
                        String currdpid = friendlyMessage.getDpid() + "";
                        if (!currdpid.equals(lastdpid)) {
                            lastFriendlyMessage.setDpid(currdpid);
                        }
                        mFirebaseDatabaseReference.child(mDatabaseRef).child(lastFriendlyMessage.getId()).updateChildren(FriendlyMessage.toMap(lastFriendlyMessage)).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    /**
                                     * since we are appending to an existing friendly message, we need
                                     * to give this friendly message a fake unique id in order for it
                                     * to be stored correctly as a potential 'unread' message
                                     */
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

        mFirebaseDatabaseReference.child(mDatabaseRef).push().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;
                friendlyMessage.setId(dataSnapshot.getKey());
                mFirebaseDatabaseReference.child(mDatabaseRef).child(friendlyMessage.getId()).setValue(friendlyMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
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
            public void onCancelled(DatabaseError databaseError) {
                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;
                mFriendlyMessageListener.onFriendlyMessageFail(friendlyMessage);
                MLog.e(TAG, "could not send message. ", databaseError);
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
        mFirebaseDatabaseReference.child(Constants.MY_BLOCKS_REF()).addChildEventListener(mBlockedUsersListener);
    }

    private void showMessageOptions(final View tempAnchorView, FriendlyMessage friendlyMessage) {
        new MessageOptionsDialogHelper().showMessageOptions(mActivity.get(), tempAnchorView, friendlyMessage, new MessageOptionsDialogHelper.MessageOptionsListener() {

            @Override
            public void onMessageOptionsDismissed() {
                mEntireScreenFrameLayout.removeView(tempAnchorView);
            }

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
                new MessagesDialogHelper().showDeleteMessageDialog(mActivity.get(), friendlyMessage, mStorageRef, mDatabaseRef);
            }

            @Override
            public void onBlockPersonRequested(final FriendlyMessage friendlyMessage) {
                new BlockUserDialogHelper().showBlockUserQuestionDialog(mActivity.get(),
                        friendlyMessage.getUserid(),
                        friendlyMessage.getName(),
                        friendlyMessage.getDpid(),
                        mInternalBlockedUserListener);

            }

            @Override
            public void onReportPersonRequested(FriendlyMessage friendlyMessage) {
                if (Preferences.getInstance().getUserId() == friendlyMessage.getUserid()) {
                    //todo
                    return; //cannot report yourself dummy!
                }
            }
        });
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            x = motionEvent.getX();
            y = motionEvent.getY();
            return false;
        }
    };
    private float x, y;

    /**
     * Check the data at the index to see if the
     * only thing that changed was the like count
     *
     * @param index
     * @param newItem
     */
    @Override
    protected void checkItemBeforeChanging(int index, FriendlyMessage newItem) {
        FriendlyMessage current = getItem(index);
        if (current.getLikes() != newItem.getLikes()) {
            replaceItem(index, newItem);
            /**
             * Optimization HACK!
             * Pass in 0 (any value will do really)
             * to tell the recycler view that the only
             * thing that changed here was the like count!
             *
             * You can pass in any object you want really.
             * Even a list of objects.  It causes populateViewHolder
             * to pass in List<Object> with a size > 0.
             */
            notifyItemChanged(index, 0);
        } else {
            replaceItem(index, newItem);
            notifyItemChanged(index);
        }
    }
}
