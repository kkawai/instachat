package com.instachat.android.app.activity;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.adapter.MessageViewHolder;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.ui.base.BaseViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public abstract class AbstractChatViewModel<Navigator extends AbstractChatNavigator> extends BaseViewModel<Navigator> {

    public static final String TAG = "AbstractChatViewModel";

    private String databaseRoot;
    private MessagesRecyclerAdapter messagesAdapter;

    public AbstractChatViewModel(DataManager dataManager, SchedulerProvider schedulerProvider) {
        super(dataManager, schedulerProvider);
    }

    //invoke after calling setNavigator
    public void onCreate() {
        if (!UserPreferences.getInstance().isLoggedIn()) {
            getNavigator().showSignIn();
            return;
        }
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
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
        getNavigator().setMaxMessageLength((int)maxMessageLength);
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
        return messagesAdapter;
    }

    public abstract boolean isPrivateChat();

    /**
     * Check every second for up to 5 seconds if messagesAdapter
     * has messages.  Hide the small progress circle if any messages
     * are found.  If there is still nothing after 5 seconds, then
     * just hide.
     */
    public void smallProgressCheck() {
        getCompositeDisposable().add(Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                .take(5)
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        getNavigator().hideSmallProgressCircle();
                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long value) throws Exception {
                        if (messagesAdapter.getItemCount() > 0) {
                            getNavigator().hideSmallProgressCircle();
                        }
                    }
                }));
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
}
