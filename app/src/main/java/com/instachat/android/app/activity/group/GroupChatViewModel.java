package com.instachat.android.app.activity.group;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.app.activity.AbstractChatViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.data.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class GroupChatViewModel extends AbstractChatViewModel<GroupChatNavigator> {

    private static final String TAG = "GroupChatViewModel";

    //public ObservableArrayList<ItemViewModel> list = new ObservableArrayList<>();
    private long groupId;
    private long mLastTypingTime;
    private DatabaseReference mTypingInRoomReference;
    private ChildEventListener mTypingInRoomEventListener;
    private DatabaseReference mMeTypingRef;
    private Map<String, Object> mMeTypingMap = new HashMap<>(3);
    private DatabaseReference mGroupSummaryRef;
    private ValueEventListener mGroupSummaryListener;

    public GroupChatViewModel(DataManager dataManager,
                              SchedulerProvider schedulerProvider,
                              FirebaseRemoteConfig firebaseRemoteConfig,
                              FirebaseDatabase firebaseDatabase) {
        super(dataManager, schedulerProvider, firebaseRemoteConfig, firebaseDatabase);
    }

    @Override
    public boolean isPrivateChat() {
        return false;
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
                                    User me = UserPreferences.getInstance().getUser();
                                    final DatabaseReference ref = firebaseDatabase.getReference(Constants
                                            .GROUP_CHAT_USERS_REF(getGroupId())).
                                            child(myUserid() + "");
                                    ref.updateChildren(me.toMap(true)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            getNavigator().removeUserFromAllGroups(myUserid(), getGroupId());
                                        }
                                    });
                                    me.setCurrentGroupId(groupChatSummary.getId());
                                    me.setCurrentGroupName(groupChatSummary.getName());
                                    firebaseDatabase.getReference(Constants.USER_INFO_REF(myUserid()))
                                            .updateChildren(me.toMap(true));
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

    /*public void fetchHomeData(List<Item> cache) {

        if (cache != null) {
            getNavigator().updateItems(cache);
            return;
        }
        setIsLoading(true);
        getCompositeDisposable()
                .add(getDataManager()
                        .getHomeData()
                        .subscribeOn(getSchedulerProvider().io())
                        .observeOn(getSchedulerProvider().ui())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                setIsLoading(false);
                            }
                        })
                        .subscribe(new Consumer<HomeResponse>() {
                            @Override
                            public void accept(@NonNull HomeResponse homeResponse)
                                    throws Exception {
                                getNavigator().updateItems(homeResponse.getItems());
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable)
                                    throws Exception {
                                getNavigator().handleError(throwable);
                            }
                        }));
    }*/

    /*public void populateViewModel(List<Item> items) {

        if (Build.VERSION.SDK_INT >= 24) {
            list.addAll(items.stream().map(item -> new ItemViewModel(item)).collect(Collectors.toList()));
        } else {
            for (Item item : items) {
                list.add(new ItemViewModel(item));
            }
        }
    }*/

    /**
     * For caching purposes.  Returns a new array list of Item backed by
     * the original list of ItemViewModel.  Could use java8 for this later.
     * Useful for saving the array list into saved instance state when
     * screen is rotated.
     *
     * @return ArrayList<Item>
     */
    /*public ArrayList<Item> convert() {

        if (Build.VERSION.SDK_INT >= 24) {
            return new ArrayList<>(list.stream().map(ItemViewModel::getItem).collect(Collectors.toList()));
        } else {
            ArrayList<Item> arrayList = new ArrayList<>(list.size());
            int size = list.size();
            for (int i = 0; i < size; i++) {
                arrayList.add(list.get(i).getItem());
            }
            return arrayList;
        }
    }*/

}
