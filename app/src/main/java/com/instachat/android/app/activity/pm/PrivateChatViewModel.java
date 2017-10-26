package com.instachat.android.app.activity.pm;

import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.AbstractChatViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class PrivateChatViewModel extends AbstractChatViewModel<PrivateChatNavigator> {

    private static final String TAG = "PrivateChatViewModel";

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

    @Override
    public void cleanup() {

    }
}
