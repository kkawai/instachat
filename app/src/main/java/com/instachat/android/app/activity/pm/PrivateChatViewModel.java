package com.instachat.android.app.activity.pm;

import android.databinding.ObservableField;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.app.activity.AbstractChatViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.api.UserResponse;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.data.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class PrivateChatViewModel extends AbstractChatViewModel<PrivateChatNavigator> {

    private static final String TAG = "PrivateChatViewModel";

    public ObservableField<String> partnerProfilePicUrl = new ObservableField<>("");
    public ObservableField<String> partnerUsername = new ObservableField<>("");
    public ObservableField<String> partnerBio = new ObservableField<>("");
    public ObservableField<String> partnerCurrentGroup = new ObservableField<>("");
    public ObservableField<String> partnerLastActive = new ObservableField<>("");
    public ObservableField<Integer> partnerLikesCount = new ObservableField<>(0);

    private DatabaseReference mTypingReference;
    private ValueEventListener mTypingValueEventListener;
    private ValueEventListener mUserInfoValueEventListener;
    private DatabaseReference mTotalLikesRef;
    private ValueEventListener mTotalLikesEventListener;
    private User toUser;
    private long mLastTypingTime;

    public PrivateChatViewModel(DataManager dataManager,
                                SchedulerProvider schedulerProvider,
                                FirebaseRemoteConfig firebaseRemoteConfig,
                                FirebaseDatabase firebaseDatabase) {
        super(dataManager, schedulerProvider, firebaseRemoteConfig, firebaseDatabase);
    }

    @Override
    public boolean isPrivateChat() {
        return true;
    }

    public void collapseAppbarAfterDelay() {

        add(Observable.timer(firebaseRemoteConfig.getLong(Constants.KEY_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY), TimeUnit.MILLISECONDS)
                .subscribeOn(getSchedulerProvider().io())
                .observeOn(getSchedulerProvider().ui())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        getNavigator().collapseAppBar();
                    }
                }).subscribe());
    }

    public void addUser(String username, String profilePicUrl, int userid) {
        PrivateChatSummary privateChatSummary = new PrivateChatSummary();
        privateChatSummary.setName(username);
        privateChatSummary.setDpid(profilePicUrl);
        privateChatSummary.setAccepted(true);
        privateChatSummary.setLastMessageSentTimestamp(System.currentTimeMillis());
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants
                .MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).child(userid + "");
        ref.updateChildren(privateChatSummary.toMap());

        /**
         * remove the person from your pending requests
         */
        firebaseDatabase.getReference(Constants.PRIVATE_REQUEST_STATUS_PARENT_REF(userid,
                UserPreferences.getInstance().getUserId())).removeValue();
    }

    public void initializePrivateChatSummary(String username, int userid, String profilePicUrl) {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants
                .MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).child(userid + "");
        PrivateChatSummary summary = new PrivateChatSummary();
        summary.setName(username);
        summary.setDpid(profilePicUrl);
        summary.setAccepted(true);
        Map<String, Object> map = summary.toMap();
        map.put(Constants.FIELD_LAST_MESSAGE_SENT_TIMESTAMP, ServerValue.TIMESTAMP);
        ref.updateChildren(map);
    }

    public void checkIfPartnerIsBlocked(final String username, final int userid) {
        final DatabaseReference ref = firebaseDatabase.getReference(Constants.MY_BLOCKS_REF()).child(userid + "");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "onDataChange() snapshot: " + dataSnapshot, " ref: ", ref);
                ref.removeEventListener(this);
                if (dataSnapshot.getValue() != null) {
                    getNavigator().collapseAppBar();
                    getNavigator().showCannotChatWithBlockedUser(username);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ref.removeEventListener(this);
                getNavigator().showErrorToast("p");
            }
        });
    }

    public void fetchUser(int toid) {

        add(getDataManager()
                .getUserById(toid)
                .subscribeOn(getSchedulerProvider().io())
                .observeOn(getSchedulerProvider().ui())
                .subscribe(new Consumer<UserResponse>() {
                    @Override
                    public void accept(@NonNull UserResponse userResponse)
                            throws Exception {

                        toUser = userResponse.user;
                        partnerProfilePicUrl.set(toUser.getProfilePicUrl());
                        MLog.d(TAG, "after grab from server: toUser: ", toUser.getId(), " ", toUser.getUsername(), " ",
                                toUser.getProfilePicUrl());
                        getNavigator().showUserProfile(toUser);
                        getNavigator().showCustomTitles(toUser.getUsername(), 0);
                        listenForPartnerTyping(toUser);
                        checkIfPartnerIsBlocked(toUser.getUsername(), toUser.getId());

                        mUserInfoValueEventListener = firebaseDatabase.getReference(Constants.USER_INFO_REF
                                (toUser.getId())).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                try {
                                    if (dataSnapshot.getValue() != null) {
                                        User user = dataSnapshot.getValue(User.class);

                                        //check if only the last active time changed
                                        boolean onlyUpdateLastActiveTime = true;
                                        if (!strEq(toUser.getUsername(), user.getUsername())) {
                                            onlyUpdateLastActiveTime = false;
                                            toUser.setUsername(user.getUsername());
                                        }
                                        if (!strEq(toUser.getProfilePicUrl(),user.getProfilePicUrl())) {
                                            onlyUpdateLastActiveTime = false;
                                            toUser.setProfilePicUrl(user.getProfilePicUrl());
                                        }
                                        if (!strEq(toUser.getBio(), user.getBio())) {
                                            onlyUpdateLastActiveTime = false;
                                            toUser.setBio(user.getBio());
                                        }
                                        if (toUser.getCurrentGroupId() != user.getCurrentGroupId()) {
                                            onlyUpdateLastActiveTime = false;
                                            toUser.setCurrentGroupName(user.getCurrentGroupName());
                                            toUser.setCurrentGroupId(user.getCurrentGroupId());
                                        }

                                        getNavigator().showCustomTitles(user.getUsername(), user.getLastOnline());

                                        if (!onlyUpdateLastActiveTime) {
                                            getNavigator().showUserProfile(toUser);
                                        }
                                        MLog.d(TAG, "user info changed onlyUpdateLastActiveTime: ",
                                                onlyUpdateLastActiveTime);

                                    }
                                } catch (Exception e) {
                                    MLog.e(TAG, "", e);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable)
                            throws Exception {
                        //getNavigator().handleError(throwable);
                        //todo
                        getNavigator().showErrorToast("getUser()");
                    }
                }));
    }

    private boolean strEq(String s1, String s2) {
        if ((s1 == null && s2 == null) || (s1 == "" && s2 == "")) {
            return true;
        }
        return s1 != null && s2 != null && s1.equals(s2);
    }

    private void listenForPartnerTyping(final User user) {
        mTypingReference = firebaseDatabase.getReference(Constants.PRIVATE_CHAT_TYPING_REF(user.getId())).
                child("" + user.getId()).child(Constants.CHILD_TYPING);
        mTypingReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@android.support.annotation.NonNull Task<Void> task) {
                mTypingValueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        MLog.d(TAG, "isTyping: onDataChange() dataSnapshot ", dataSnapshot);
                        if (dataSnapshot.exists()) {
                            boolean isTyping = dataSnapshot.getValue(Boolean.class);
                            MLog.d(TAG, "isTyping: ", isTyping);
                            if (isTyping) {
                                onRemoteUserTyping(user);
                                mTypingReference.setValue(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };
                mTypingReference.addValueEventListener(mTypingValueEventListener);
            }
        });

    }

    //@Override
    private void onRemoteUserTyping(User user) {
        getNavigator().showTypingDots();
    }

    public void listenForUpdatedLikeCount(int userid) {

        mTotalLikesRef = firebaseDatabase.getReference(Constants.USER_TOTAL_LIKES_RECEIVED_REF(userid));
        mTotalLikesEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    long count = dataSnapshot.getValue(Long.class);
                    getNavigator().showLikesCount((int)count);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mTotalLikesRef.addValueEventListener(mTotalLikesEventListener);
    }

    @Override
    public void cleanup() {
        if (mTotalLikesRef != null)
            mTotalLikesRef.removeEventListener(mTotalLikesEventListener);
        if (mTypingReference != null && mTypingValueEventListener != null)
            mTypingReference.removeEventListener(mTypingValueEventListener);

        if (mUserInfoValueEventListener != null) {
            try {
                firebaseDatabase.getReference(Constants.USER_INFO_REF(toUser.getId())).removeEventListener
                        (mUserInfoValueEventListener);
            } catch (Exception e) {
                MLog.e(TAG, "", e);
            }
        }
    }

    public void clearPrivateUnreadMessages(int toUserid) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference(Constants
                    .MY_PRIVATE_CHATS_SUMMARY_PARENT_REF());
            ref.child(toUserid + "").child(Constants.CHILD_UNREAD_MESSAGES).removeValue();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    @Override
    public void onMeTyping() {
        try {
            if (System.currentTimeMillis() - mLastTypingTime < 3000) {
                return;
            }
            firebaseDatabase
                    .getReference(Constants.PRIVATE_CHAT_TYPING_REF(myUserid()))
                    .child("" + myUserid())
                    .child(Constants.CHILD_TYPING).setValue(true);
            mLastTypingTime = System.currentTimeMillis();
        } catch (Exception e) {
            MLog.e(TAG, "onMeTyping() failed", e);
        }
    }

    public User getPartner() {
        return toUser;
    }

}
