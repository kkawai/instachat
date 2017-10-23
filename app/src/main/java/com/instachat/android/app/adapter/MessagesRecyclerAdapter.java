package com.instachat.android.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
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
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.MessageOptionsDialogHelper;
import com.instachat.android.app.activity.ActivityState;
import com.instachat.android.app.blocks.BlockUserDialogHelper;
import com.instachat.android.app.blocks.BlockedUser;
import com.instachat.android.app.blocks.BlockedUserListener;
import com.instachat.android.app.blocks.ReportUserDialogHelper;
import com.instachat.android.app.likes.LikesHelper;
import com.instachat.android.data.db.OneTimeMessageDb;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.TimeUtil;
import com.tooltip.Tooltip;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.instachat.android.view.TextViewUtil.blurText;

/**
 * Created by kevin on 8/23/2016.
 */
public class MessagesRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends BaseMessagesAdapter<FriendlyMessage, MessageViewHolder> {

    public static final String TAG = "MessagesRecyclerAdapter";

    private static final int ITEM_VIEW_TYPE_STANDARD_MESSAGE = 0;
    private static final int ITEM_VIEW_TYPE_WEB_LINK = 1;
    private static final int ITEM_VIEW_TYPE_STANDARD_MESSAGE_ME = 2;
    private static final int ITEM_VIEW_TYPE_WEB_LINK_ME = 3;

    private static final int PAYLOAD_PERISCOPE_CHANGE = 0;
    private static final int PAYLOAD_IMAGE_REVEAL = 1;

    private StorageReference mStorageRef;
    private DatabaseReference mMessagesRef;
    private WeakReference<Activity> mActivity;
    private ActivityState mActivityState;
    private MessageTextClickedListener mMessageTextClickedListener;
    private UserClickedListener mUserClickedListener;
    private FriendlyMessageListener mFriendlyMessageListener;
    private BlockedUserListener mBlockedUserListener;
    private String mDatabaseRef;
    private FrameLayout mEntireScreenFrameLayout;
    private int mMaxPeriscopesPerItem;
    private boolean mIsPrivateChat;
    private int mMyUserid;
    private final MessagesRecyclerAdapterHelper map;

    public MessagesRecyclerAdapter(Class modelClass, int modelLayout, Class viewHolderClass, Query ref, MessagesRecyclerAdapterHelper map) {
        super(modelClass, modelLayout, viewHolderClass, ref);
        mMessagesRef = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mMaxPeriscopesPerItem = (int) FirebaseRemoteConfig.getInstance().getLong(Constants.KEY_MAX_PERISCOPABLE_LIKES_PER_ITEM);
        mMyUserid = UserPreferences.getInstance().getUserId();
        this.map = map;
    }

    public void setIsPrivateChat(boolean isPrivateChat) {
        mIsPrivateChat = isPrivateChat;
    }

    public void setDatabaseRoot(String root) {
        mDatabaseRef = root;
    }

    public void setActivity(@NonNull Activity activity,
                            @NonNull ActivityState activityState,
                            @NonNull FrameLayout entireScreenLayout) {
        mActivity = new WeakReference<>(activity);
        mActivityState = activityState;
        mEntireScreenFrameLayout = entireScreenLayout;
        mEntireScreenFrameLayout.setOnTouchListener(mOnTouchListener);
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

        @Override
        public void onUserUnblocked(int userid) {

        }
    };

    @Override
    public int getItemViewType(int position) {
        FriendlyMessage friendlyMessage = getItem(position);
        String text = friendlyMessage.getText() + "";

        if (URLUtil.isHttpUrl(text) || URLUtil.isHttpsUrl(text)) {
            if (friendlyMessage.getUserid() == mMyUserid)
                return ITEM_VIEW_TYPE_WEB_LINK_ME;
            else
                return ITEM_VIEW_TYPE_WEB_LINK;
        }
        if (friendlyMessage.getUserid() == mMyUserid)
            return ITEM_VIEW_TYPE_STANDARD_MESSAGE_ME;
        else
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
        } else if (viewType == ITEM_VIEW_TYPE_STANDARD_MESSAGE_ME) {
            return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_me, parent, false));
        } else if (viewType == ITEM_VIEW_TYPE_WEB_LINK) {
            return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_web_clipping, parent, false));
        } else if (viewType == ITEM_VIEW_TYPE_WEB_LINK_ME) {
            return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_me_web_clipping, parent, false));
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
                mUserClickedListener.onUserClicked(friendlyMessage.getUserid(),
                        friendlyMessage.getName(),
                        friendlyMessage.getDpid(), holder.messengerImageView);
            }
        });

        if (viewType == ITEM_VIEW_TYPE_STANDARD_MESSAGE || viewType == ITEM_VIEW_TYPE_STANDARD_MESSAGE_ME) {
            final View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());

                    if (friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME) {
                        if (!OneTimeMessageDb.getInstance().messageExists(friendlyMessage.getId())) {
                            mMessageTextClickedListener.onMessageClicked(holder.getAdapterPosition());
                        } else {
                            new Tooltip.Builder(view, R.style.drawer_tooltip).setText(mActivity.get().getString(R.string.one_time_message_has_been_read_already)).show();
                        }
                    } else if (!TextUtils.isEmpty(friendlyMessage.getImageUrl())) {
                        mMessageTextClickedListener.onMessageClicked(holder.getAdapterPosition());
                    }
                }
            };

            holder.messageTextParent.setOnClickListener(onClickListener);
            holder.messagePhotoView.setOnClickListener(onClickListener);

            holder.itemView.setOnLongClickListener(onLongClickListener);
            holder.messageTextParent.setOnLongClickListener(onLongClickListener);
            holder.messagePhotoView.setOnLongClickListener(onLongClickListener);
            holder.messagePhotoWarningView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    notifyItemChanged(holder.getAdapterPosition(), PAYLOAD_IMAGE_REVEAL);
                }
            });
        } else if (viewType == ITEM_VIEW_TYPE_WEB_LINK || viewType == ITEM_VIEW_TYPE_WEB_LINK_ME) {
            View.OnClickListener webLinkClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());
                    final Uri uri = Uri.parse(StringUtil.correctUrl(friendlyMessage.getText()));
                    try {
                        mActivity.get().startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception e) {
                        MLog.e(TAG, "", e);
                        Toast.makeText(mActivity.get(), R.string.could_not_open_url, Toast.LENGTH_SHORT).show();
                    }
                }
            };
            holder.webLinkParent.setOnClickListener(webLinkClickListener);
            holder.webLinkParent.setOnLongClickListener(onLongClickListener);
        } else {
            throw new IllegalArgumentException("unknown viewType");
        }

        View.OnClickListener likesButtonClicked = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MLog.d(TAG, "likesButtonClicked likes debug ", holder.getAdapterPosition());
                likeFriendlyMessage(holder);
            }
        };
        View.OnClickListener likesRelatedButtonsClicked = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getTag() != null) {
                    ((View) view.getTag()).callOnClick();
                }
                MLog.d(TAG, "likesRelatedButtonsClicked likes debug ", holder.getAdapterPosition());
            }
        };
        holder.likesButton.setOnClickListener(likesButtonClicked);
        holder.likesButtonParent.setOnClickListener(likesRelatedButtonsClicked);
        holder.likesButtonParent.setTag(holder.likesButton);
        holder.periscopeLayout.setOnClickListener(likesRelatedButtonsClicked);
        holder.periscopeLayout.setTag(holder.likesButton);
        return holder;
    }

    private void likeFriendlyMessage(MessageViewHolder holder) {
        final FriendlyMessage friendlyMessage = getItem(holder.getAdapterPosition());
        DatabaseReference ref = mMessagesRef.child(mDatabaseRef).child(friendlyMessage.getId()).child(Constants.CHILD_LIKES);
        LikesHelper.getInstance().likeFriendlyMessage(friendlyMessage, ref);
    }

    @Override
    protected void populateViewHolder(final MessageViewHolder viewHolder, final FriendlyMessage friendlyMessage,
                                      int position, List<Object> payloads) {

        if (friendlyMessage.getLikes() > 0) {
            viewHolder.periscopeParent.setVisibility(View.VISIBLE);
            final int consumedLikes = map.getConsumedLikesMap().containsKey(friendlyMessage.getId()) ?
                    map.getConsumedLikesMap().get(friendlyMessage.getId()) : 0;
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
                            try {
                                viewHolder.periscopeLayout.addHeart();
                            } catch (final IllegalArgumentException e) {
                                MLog.e(TAG, " failed to periscope ", e.getMessage());
                            }
                        }
                        map.getConsumedLikesMap().put(friendlyMessage.getId(), friendlyMessage.getLikes());
                    }
                }, 500);
            } else {
                viewHolder.periscopeLayout.setVisibility(View.INVISIBLE);
            }
            viewHolder.likesButton.setChecked(false);
            viewHolder.likesButton.setBtnColor(viewHolder.likesButton.getContext().getResources().getColor(R.color.chat_like_button_active_state));
            viewHolder.likesCount.setText(" " + friendlyMessage.getLikes() + " ");

        } else {
            viewHolder.periscopeParent.setVisibility(View.INVISIBLE);
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
        if (payloads != null && payloads.contains(PAYLOAD_PERISCOPE_CHANGE)) {
            return;
        }

        if (viewHolder.messagePhotoViewParent != null) {
            if (friendlyMessage.getImageUrl() != null) {
                viewHolder.messagePhotoViewParent.setVisibility(View.VISIBLE);
                if (friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME) {
                    Glide.with(mActivity.get()).
                            load(friendlyMessage.getImageUrl()).
                            bitmapTransform(
                                    //no need to center crop, image will be blurred anyways!
                                    //new CenterCrop(mActivity.get()),
                                    new BlurTransformation(mActivity.get(), 75),
                                    new RoundedCornersTransformation(mActivity.get(), 30, 0, RoundedCornersTransformation.CornerType.ALL)).
                            crossFade().
                            into(viewHolder.messagePhotoView);
                } else {
                    Glide.with(mActivity.get()).
                            load(friendlyMessage.getImageUrl()).
                            bitmapTransform(
                                    new CenterCrop(mActivity.get()),
                                    new RoundedCornersTransformation(mActivity.get(), 30, 0, RoundedCornersTransformation.CornerType.ALL)).
                            crossFade().
                            into(viewHolder.messagePhotoView);
                }
                if (payloads != null && payloads.contains(PAYLOAD_IMAGE_REVEAL)) {
                    MLog.d(TAG, "populate messagePhotoViewParent got reveal payload");
                    viewHolder.messagePhotoView.setVisibility(View.VISIBLE);
                    viewHolder.messagePhotoWarningView.setVisibility(View.GONE);
                    AnimationUtil.scaleInFromCenter(viewHolder.messagePhotoViewParent);
                } else if (friendlyMessage.isPossibleAdultImage() || friendlyMessage.isPossibleViolentImage() &&
                        (friendlyMessage.getMessageType() != FriendlyMessage.MESSAGE_TYPE_ONE_TIME)) {
                    MLog.d(TAG, "populate messagePhotoViewParent did not get reveal payload");
                    viewHolder.messagePhotoView.setVisibility(View.INVISIBLE);
                    viewHolder.messagePhotoWarningView.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.messagePhotoView.setVisibility(View.VISIBLE);
                    viewHolder.messagePhotoWarningView.setVisibility(View.GONE);
                }
            } else {
                viewHolder.messagePhotoViewParent.setVisibility(View.GONE);
                viewHolder.messagePhotoView.setVisibility(View.GONE);
                viewHolder.messagePhotoWarningView.setVisibility(View.GONE);
            }
        }

        if (payloads != null && payloads.contains(PAYLOAD_IMAGE_REVEAL)) {
            return;
        }

        listenForBlockedUsers();

        int type = getItemViewType(position);
        if (type == ITEM_VIEW_TYPE_WEB_LINK || type == ITEM_VIEW_TYPE_WEB_LINK_ME) {
            new WebLinkHelper().populateWebLinkPost(viewHolder, friendlyMessage, position);
        } else {
            if (viewHolder.messageTextView != null) {
                if (StringUtil.isNotEmpty(friendlyMessage.getText())) {
                    viewHolder.messageTextView.setText(friendlyMessage.getText());
                    if (friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME) {
                        blurText(viewHolder.messageTextView, true);
                    } else {
                        blurText(viewHolder.messageTextView, false);
                    }
                } else
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

        if (mIsPrivateChat) {

            if (friendlyMessage.isConsumedByPartner()) {
                viewHolder.messageReadConfirmationView.setImageResource(R.drawable.ic_done_all_black_18dp);
            } else {
                if (mMyUserid != friendlyMessage.getUserid() && friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_NORMAL) {
                    //my partner reading the message for the first time
                    friendlyMessage.setConsumedByPartner(true);//set locally for optimization purposes
                    mMessagesRef.child(mDatabaseRef).child(friendlyMessage.getId()).child(Constants.CHILD_MESSAGE_CONSUMED_BY_PARTNER).setValue(true); //save remotely
                    viewHolder.messageReadConfirmationView.setImageResource(R.drawable.ic_done_all_black_18dp);
                } else {
                    //just me reading my own message again
                    viewHolder.messageReadConfirmationView.setImageResource(R.drawable.ic_done_black_18dp);
                }
            }

            /**
             * lastly, I only care about messages that I have sent showing read confirmation
             */
            viewHolder.messageReadConfirmationView.setVisibility(mMyUserid != friendlyMessage.getUserid() ? View.GONE : View.VISIBLE);
        }

        viewHolder.messengerImageView.setImageDrawable(null);
        if (TextUtils.isEmpty(friendlyMessage.getName())) {
            viewHolder.messengerImageView.setVisibility(View.GONE);
        } else {
            viewHolder.messengerImageView.setVisibility(View.VISIBLE);
            try {
                Glide.with(mActivity.get()).load(friendlyMessage.getDpid()).error(R.drawable.ic_anon_person_36dp).into(viewHolder.messengerImageView);
            } catch (final Exception e) {
                MLog.e(TAG, "", e);
                viewHolder.messengerImageView.setImageResource(R.drawable.ic_anon_person_36dp);
            }
        }
    }

    public void cleanup() {
        super.cleanup();
        if (mActivity != null)
            mActivity.clear();
        mActivity = null;
        mActivityState = null;
        mUserClickedListener = null;
        mMessageTextClickedListener = null;
        mBlockedUserListener = null;
        if (mBlockedUsersRef != null && mBlockedUsersListener != null)
            mBlockedUsersRef.removeEventListener(mBlockedUsersListener);
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
                        mMessagesRef.child(mDatabaseRef).child(lastFriendlyMessage.getId()).updateChildren(lastFriendlyMessage.toMap()).addOnCompleteListener(new OnCompleteListener<Void>() {
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

        mMessagesRef.child(mDatabaseRef).push().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;
                friendlyMessage.setId(dataSnapshot.getKey());
                MLog.d(TAG, "pushed message. debug possibleAdult content ", friendlyMessage.isPossibleAdultImage());
                mMessagesRef.child(mDatabaseRef).child(friendlyMessage.getId()).setValue(friendlyMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
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
    private DatabaseReference mBlockedUsersRef;
    private Map<Integer, Boolean> mBlockedUsers = new Hashtable<>(20);
    private boolean mIsListeningForBlockedUsers;

    public int getNumBlockedUsers() {
        return mBlockedUsers.size();
    }

    private void listenForBlockedUsers() {
        if (mIsListeningForBlockedUsers)
            return;
        mIsListeningForBlockedUsers = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;
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
                        mBlockedUserListener.onUserUnblocked(blockedUser.id);
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };
                mBlockedUsersRef = FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF());
                mBlockedUsersRef.addChildEventListener(mBlockedUsersListener);
            }
        }, 2000);
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
                new ReportUserDialogHelper().showReportUserQuestionDialog(mActivity.get(),
                        friendlyMessage.getUserid(),
                        friendlyMessage.getName(),
                        friendlyMessage.getDpid());
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
            notifyItemChanged(index, PAYLOAD_PERISCOPE_CHANGE);
        } else {
            replaceItem(index, newItem);
            notifyItemChanged(index);
        }
    }

    private String lastMessage;

    /**
     * For debugging those hard bugs
     *
     * @return
     */
    public String peekLastMessage() {
        return lastMessage;
    }

    @Override
    protected void onAddItem(FriendlyMessage newFriendlyMessage) {
        lastMessage = newFriendlyMessage.getText();
        if (mItemWasRemoved && getItemCount() > 0) {
            mItemWasRemoved = false;
            FriendlyMessage lastFriendlyMessage = getItem(getItemCount() - 1);
            if (newFriendlyMessage.getTime() > lastFriendlyMessage.getTime() || isSmallDifferenceInTime(newFriendlyMessage.getTime(),lastFriendlyMessage.getTime()))
                super.onAddItem(newFriendlyMessage);
        } else {
            super.onAddItem(newFriendlyMessage);
        }

    }

    private boolean mItemWasRemoved;

    @Override
    protected void onRemoveItem(int index) {
        mItemWasRemoved = true;
        super.onRemoveItem(index);
    }

    public static final long ONE_HOUR = 60*1000*60L;
    private boolean isSmallDifferenceInTime(long t1, long t2) {
        return Math.abs(t1 - t2) < ONE_HOUR;
    }
}
