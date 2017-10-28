package com.instachat.android.app.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.TheApp;
import com.instachat.android.app.adapter.MessageViewHolder;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.blocks.BlockedUserListener;
import com.instachat.android.app.ui.base.BaseViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.util.MLog;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Predicate;

public abstract class AbstractChatViewModel<Navigator extends AbstractChatNavigator> extends BaseViewModel<Navigator> {

    private static final String TAG = "AbstractChatViewModel";

    private String databaseRoot;

    private MessagesRecyclerAdapter messagesAdapter;

    protected final FirebaseRemoteConfig firebaseRemoteConfig;
    protected final FirebaseDatabase firebaseDatabase;

    public AbstractChatViewModel(DataManager dataManager,
                                 SchedulerProvider schedulerProvider,
                                 FirebaseRemoteConfig firebaseRemoteConfig,
                                 FirebaseDatabase firebaseDatabase) {
        super(dataManager, schedulerProvider);
        this.firebaseRemoteConfig = firebaseRemoteConfig;
        this.firebaseDatabase = firebaseDatabase;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanup();
    }

    @Override
    public void setNavigator(Navigator navigator) {
        super.setNavigator(navigator);
        if (!UserPreferences.getInstance().isLoggedIn()) {
            navigator.showSignIn();
            return;
        }
        try {
            firebaseDatabase.setPersistenceEnabled(true);
            MLog.w(TAG, "FirebaseDatabase.getInstance().setPersistenceEnabled(true) succeeded");
        } catch (Exception e) {
            //MLog.e(TAG, "FirebaseDatabase.getInstance().setPersistenceEnabled(true) failed: " + e);
        }
    }

    public void fetchConfig(final FirebaseRemoteConfig firebaseRemoteConfig) {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        firebaseRemoteConfig.fetch(cacheExpiration).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
                firebaseRemoteConfig.activateFetched();
                applyRetrievedLengthLimit(firebaseRemoteConfig);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // There has been an error fetching the config
                MLog.w(TAG, "Error fetching config: " + e.getMessage());
                applyRetrievedLengthLimit(firebaseRemoteConfig);
            }
        });
    }

    public void setDatabaseRoot(String databaseRoot) {
        this.databaseRoot = databaseRoot;
    }

    public String getDatabaseRoot() {
        return databaseRoot;
    }

    private void applyRetrievedLengthLimit(FirebaseRemoteConfig firebaseRemoteConfig) {
        long maxMessageLength = firebaseRemoteConfig.getLong(Constants.KEY_MAX_MESSAGE_LENGTH);
        getNavigator().setMaxMessageLength((int) maxMessageLength);
    }

    public MessagesRecyclerAdapter getMessagesAdapter(FirebaseRemoteConfig firebaseRemoteConfig,
                                                      MessagesRecyclerAdapterHelper map) {
        messagesAdapter = new MessagesRecyclerAdapter<>(FriendlyMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                FirebaseDatabase.getInstance().getReference(getDatabaseRoot()).
                        limitToLast((int) firebaseRemoteConfig.getLong(Constants.KEY_MAX_MESSAGE_HISTORY)),
                map);
        messagesAdapter.setIsPrivateChat(isPrivateChat());
        messagesAdapter.setDatabaseRoot(getDatabaseRoot());
        messagesAdapter.setBlockedUserListener(blockedUserListener);
        return messagesAdapter;
    }

    public abstract boolean isPrivateChat();

    public void add(Disposable disposable) {
        getCompositeDisposable().add(disposable);
    }

    /**
     * Check every second for up to 5 seconds if messagesAdapter
     * has messages.  Hide the small progress circle if any messages
     * are found.  If there is still nothing after 5 seconds, then
     * just hide.
     */
    public void smallProgressCheck() {

        //keep checking every half second if message adapter has messages
        //for up to 5 seconds.  close the smallProgressCircle as soon as
        //messages are detected or 5 seconds has elapsed, whichever comes
        //first.
        add(Observable.interval(500,500, TimeUnit.MILLISECONDS)
                .subscribeOn(getSchedulerProvider().io())
                .observeOn(getSchedulerProvider().ui())
                .take(10).takeUntil(new Predicate<Long>() {
                    @Override
                    public boolean test(Long aLong) throws Exception {
                        return (messagesAdapter.getItemCount() > 0);
                    }
                })
                .doOnComplete(new Action() {

                    @Override
                    public void run() throws Exception {
                        getNavigator().hideSmallProgressCircle();
                    }
                })
                .subscribe());

    }

    public Integer myUserid() {
        return UserPreferences.getInstance().getUserId();
    }

    public String myDpid() {
        return UserPreferences.getInstance().getUser().getProfilePicUrl();
    }

    public String myUsername() {
        return UserPreferences.getInstance().getUsername() + "";
    }

    public abstract void cleanup();

    public BlockedUserListener getBlockedUserListener() {
        return blockedUserListener;
    }

    private BlockedUserListener blockedUserListener = new BlockedUserListener() {
        @Override
        public void onUserBlocked(int userid) {
            Bundle payload = new Bundle();
            payload.putString("by", myUsername());
            payload.putInt("userid", userid);
            FirebaseAnalytics.getInstance(TheApp.getInstance()).logEvent(Events.USER_BLOCKED, payload);
        }

        @Override
        public void onUserUnblocked(int userid) {
            Bundle payload = new Bundle();
            payload.putString("by", myUsername());
            payload.putInt("userid", userid);
            FirebaseAnalytics.getInstance(TheApp.getInstance()).logEvent(Events.USER_UNBLOCKED, payload);
        }
    };

    public boolean validateMessage(final String text, boolean showOptions) {
        if (StringUtil.isEmpty(text)) {
            return false;
        }
        if (StringUtil.isEmpty(myDpid())) {
            getNavigator().showNeedPhotoDialog();
            return false;
        }
        final FriendlyMessage friendlyMessage = new FriendlyMessage(text,
                myUsername(),
                myUserid(),
                myDpid(), null,
                false, false, null, System.currentTimeMillis());
        if (!showOptions) {
            getNavigator().sendText(friendlyMessage);
            return true;
        }
        getNavigator().showSendOptions(friendlyMessage);
        return true;
    }

}
