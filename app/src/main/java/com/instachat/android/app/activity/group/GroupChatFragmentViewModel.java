package com.instachat.android.app.activity.group;

import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.instachat.android.Constants;
import com.instachat.android.app.activity.AbstractChatViewModel;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.bans.BanHelper;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.data.model.User;
import com.instachat.android.util.AdminUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Action;

public class GroupChatFragmentViewModel extends AbstractChatViewModel<GroupChatNavigator> {

    private static final String TAG = "GroupChatViewModel";

    public ObservableField<String> usernameTyping = new ObservableField<>("");
    private long groupId;
    private long mLastTypingTime;
    private int roomCommentCount;
    private DatabaseReference mTypingInRoomReference;
    private ChildEventListener mTypingInRoomEventListener;
    private DatabaseReference mMeTypingRef;
    private Map<String, Object> mMeTypingMap = new HashMap<>(3);
    private DatabaseReference mGroupSummaryRef;
    private ValueEventListener mGroupSummaryListener;
    private DatabaseReference mRightRef;
    private ValueEventListener mRightListener;

    public GroupChatFragmentViewModel(DataManager dataManager,
                                      SchedulerProvider schedulerProvider,
                                      FirebaseRemoteConfig firebaseRemoteConfig,
                                      FirebaseDatabase firebaseDatabase,
                                      BanHelper banHelper) {
        super(dataManager, schedulerProvider, firebaseRemoteConfig, firebaseDatabase, banHelper);
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public void listenForTyping() {

        mMeTypingRef = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_TYPING_REF(getGroupId(),
                myUserid())).
                child(Constants.CHILD_TYPING);
        mMeTypingRef.setValue(false);

        mTypingInRoomReference = FirebaseDatabase.getInstance().getReference(Constants
                .GROUP_CHAT_USERS_TYPING_PARENT_REF(getGroupId()));
        mTypingInRoomEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "isTyping: onChildAdded() dataSnapshot ", dataSnapshot, " s: ", s);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "isTyping: onChildChanged() dataSnapshot ", dataSnapshot, " s: ", s);
                MLog.d(TAG, "isTyping: onDataChange() dataSnapshot ", dataSnapshot);
                if (dataSnapshot.exists() && dataSnapshot.hasChild(Constants.CHILD_TYPING)) {
                    boolean isTyping = dataSnapshot.child(Constants.CHILD_TYPING).getValue(Boolean.class);
                    String username = dataSnapshot.child(Constants.CHILD_USERNAME).getValue(String.class);
                    //String dpid = dataSnapshot.child(Constants.CHILD_DPID).getValue(String.class);
                    int userid = Integer.parseInt(dataSnapshot.getKey());
                    //MLog.d(TAG, "isTyping: ", isTyping, " dpid: ", dpid, " userid ", userid);
                    if (isTyping) {
                        getNavigator().showUserTyping(username);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "isTyping: onChildRemoved() dataSnapshot ", dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mTypingInRoomReference.addChildEventListener(mTypingInRoomEventListener);
    }

    public void onMeTyping() {
        try {
            if (System.currentTimeMillis() - mLastTypingTime < 3000)
                return;
            mLastTypingTime = System.currentTimeMillis();
            if (mMeTypingMap.size() == 0) {
                mMeTypingMap.put(Constants.CHILD_TYPING, true);
                mMeTypingMap.put(Constants.CHILD_USERNAME, myUsername());
            }
            FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_TYPING_REF(getGroupId(), myUserid()))
                    .setValue(mMeTypingMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    //immediately flip the value back to false in order
                    //to pick up further typing by my person
                    mMeTypingRef.setValue(false);
                }
            });
        } catch (Exception e) {
            MLog.e(TAG, "onMeTyping() failed", e);
        }
    }

    @Override
    public void cleanup() {
        if (mTypingInRoomReference != null && mTypingInRoomEventListener != null)
            mTypingInRoomReference.removeEventListener(mTypingInRoomEventListener);
        if (mRightRef != null && mRightListener != null)
            mRightRef.removeEventListener(mRightListener);
        getDatabaseReference().removeEventListener(roomCommentCountValueEventListener);
    }

    public void removeGroupInfoListener() {
        if (mGroupSummaryRef != null && mGroupSummaryListener != null) {
            mGroupSummaryRef.removeEventListener(mGroupSummaryListener);
        }
    }

    public void addUserPresenceToGroup() {

        mGroupSummaryRef = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_ROOMS).
                child(getGroupId() + "");
        mGroupSummaryListener = mGroupSummaryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    final GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                    getNavigator().showSubtitle();
                    /**
                     * run this delayed, if the user re-enters
                     * the same room (for a variety of reasons)
                     * give them some time to remove themself
                     * before immediately adding them back again.
                     */
                    add(Observable.timer(2000, TimeUnit.MILLISECONDS)
                            .subscribeOn(getSchedulerProvider().io())
                            .observeOn(getSchedulerProvider().ui())
                            .doOnComplete(new Action() {
                                @Override
                                public void run() throws Exception {
                                    MLog.d(TAG, "addUserPresenceToGroup() groupChatViewModel.getGroupId(): ", getGroupId(), " username: ", myUsername());
                                    if (!FirebaseAuth.getInstance().getCurrentUser().getEmail().equals(UserPreferences.getInstance().getEmail())) {
                                        getNavigator().enterChat();
                                        return;
                                    }
                                    User me = UserPreferences.getInstance().getUser();
                                    final DatabaseReference ref = firebaseDatabase.getReference(Constants
                                            .GROUP_CHAT_USERS_REF(getGroupId())).
                                            child(myUserid() + "");
                                    ref.updateChildren(me.toMap()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            getNavigator().removeUserFromAllGroups(myUserid(), getGroupId());
                                        }
                                    });
                                    me.setCurrentGroupId(groupChatSummary.getId());
                                    me.setCurrentGroupName(groupChatSummary.getName());
                                    firebaseDatabase.getReference(Constants.USER_INFO_REF(myUserid()))
                                            .updateChildren(me.toMap());
                                }
                            }).subscribe());

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void removeUserPresenceFromGroup() {
        removeGroupInfoListener();
        MLog.d(TAG, "removeUserPresenceFromGroup() groupChatViewModel.getGroupId(): ", getGroupId(), " username: ", myUsername());
        FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_REF(getGroupId())).child(myUserid() + "")
                .removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(myUserid())).child(Constants
                .FIELD_CURRENT_GROUP_ID).removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(myUserid())).child(Constants
                .FIELD_CURRENT_GROUP_NAME).removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_TYPING_REF(getGroupId(), myUserid()))
                .removeValue();
    }

    public void fetchGroupName() {
        mRightRef = firebaseDatabase.getReference(Constants.GROUP_CHAT_ROOMS).child
                (getGroupId() + "");
        mRightListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "setupRightDrawerContent() dataSnapshot: ", dataSnapshot);
                GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                if (groupChatSummary == null || groupChatSummary.getName() == null) {
                    /*
                     * group was deleted; go to the global default public room
                     */
                    getNavigator().showGroupChatActivity(Constants.DEFAULT_PUBLIC_GROUP_ID, "Main", null,
                            null);
                    return;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mRightRef.addValueEventListener(mRightListener);

        if (AdminUtil.isMeAdmin())
            fetchRoomCommentsCount();
    }

    public void onToggleGroupChatAppbar() {
        getNavigator().toggleGroupChatAppBar();
    }

    /**
     * For analytics purposes.
     *
     * @param friendlyMessage
     */
    public void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage) {
        Bundle payload = new Bundle();
        payload.putString("from", myUsername());
        payload.putString("type", friendlyMessage.getImageUrl() != null ? "photo" : "text");
        payload.putLong("group", getGroupId());
        payload.putBoolean("one-time", friendlyMessage.getMT() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME);
        firebaseAnalytics.logEvent(Events.MESSAGE_GROUP_SENT_EVENT, payload);
    }

    public void clearRoomComments() {
        firebaseDatabase.getReference(getDatabaseRoot()).removeValue();
        FirebaseStorage.getInstance().getReference(getDatabaseRoot()).delete();
    }

    private ValueEventListener roomCommentCountValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            roomCommentCount = (int)dataSnapshot.getChildrenCount();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private void fetchRoomCommentsCount() {
        getDatabaseReference().removeEventListener(roomCommentCountValueEventListener);
        getDatabaseReference().addListenerForSingleValueEvent(roomCommentCountValueEventListener);
    }

    public int getRoomCommentCount() {
        return roomCommentCount;
    }

    public void checkEmailVerified() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (!user.isEmailVerified()) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                MLog.d(TAG, "Email sent.");
                            }
                        }
                    });
            getNavigator().showVerificationEmailSent();
        }
    }

}
