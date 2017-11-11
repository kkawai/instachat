package com.instachat.android.app.activity;

import android.databinding.ObservableField;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.text.TextUtils;
import android.util.Log;

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
import com.instachat.android.data.api.BasicExistenceResult;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.api.UserResponse;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

public abstract class AbstractChatViewModel<Navigator extends AbstractChatNavigator> extends BaseViewModel<Navigator> {

    private static final String TAG = "AbstractChatViewModel";

    public ObservableField<String> profilePicUrl = new ObservableField<>("");
    public ObservableField<String> username = new ObservableField<>("");
    public ObservableField<String> bio = new ObservableField<>("");
    public ObservableField<Integer> likes = new ObservableField<>(0);
    public ObservableField<Integer> pendingRequests = new ObservableField<>(0);

    private String databaseRoot;

    private MessagesRecyclerAdapter messagesAdapter;

    protected final FirebaseRemoteConfig firebaseRemoteConfig;
    protected final FirebaseDatabase firebaseDatabase;
    protected FirebaseAnalytics firebaseAnalytics;

    public AbstractChatViewModel(DataManager dataManager,
                                 SchedulerProvider schedulerProvider,
                                 FirebaseRemoteConfig firebaseRemoteConfig,
                                 FirebaseDatabase firebaseDatabase) {
        super(dataManager, schedulerProvider);
        this.firebaseRemoteConfig = firebaseRemoteConfig;
        this.firebaseDatabase = firebaseDatabase;
    }

    public void setFirebaseAnalytics(FirebaseAnalytics firebaseAnalytics) {
        this.firebaseAnalytics = firebaseAnalytics;
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
            //firebaseDatabase.setPersistenceEnabled(true);
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
        messagesAdapter.setDatabaseRoot(getDatabaseRoot());
        messagesAdapter.setBlockedUserListener(blockedUserListener);
        return messagesAdapter;
    }

    public abstract void onMeTyping();

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
        add(Observable.interval(500, 500, TimeUnit.MILLISECONDS)
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

    private BlockedUserListener blockedUserListener = new BlockedUserListener() {
        @Override
        public void onUserBlocked(int userid) {
            Bundle payload = new Bundle();
            payload.putString("by", myUsername());
            payload.putInt("userid", userid);
            firebaseAnalytics.logEvent(Events.USER_BLOCKED, payload);
        }

        @Override
        public void onUserUnblocked(int userid) {
            Bundle payload = new Bundle();
            payload.putString("by", myUsername());
            payload.putInt("userid", userid);
            firebaseAnalytics.logEvent(Events.USER_UNBLOCKED, payload);
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

    public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags,
                                   Bundle opts, String[] contentMimeTypes) {

        boolean supported = false;
        for (final String mimeType : contentMimeTypes) {
            if (inputContentInfo.getDescription().hasMimeType(mimeType)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            return false;
        }

        return onCommitContentInternal(inputContentInfo, flags);
    }

    private boolean onCommitContentInternal(InputContentInfoCompat inputContentInfo, int flags) {
        if ((flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
            try {
                inputContentInfo.requestPermission();
            } catch (Exception e) {
                Log.e(TAG, "InputContentInfoCompat#requestPermission() failed.", e);
                return false;
            }
        }
        Uri linkUri = inputContentInfo.getLinkUri();
        if (inputContentInfo != null && inputContentInfo.getDescription() != null) {
            if (inputContentInfo.getDescription().toString().contains("image/gif")) {
                final FriendlyMessage friendlyMessage = new FriendlyMessage("",
                        myUsername(),
                        myUserid(),
                        myDpid(),
                        linkUri.toString(), false, false, null, System.currentTimeMillis());
                friendlyMessage.setMessageType(FriendlyMessage.MESSAGE_TYPE_NORMAL);
                messagesAdapter.sendFriendlyMessage(friendlyMessage);
            }
        }
        return true;
    }

    public void saveUserPhoto(@NonNull String photoUrl) {
        final User user = UserPreferences.getInstance().getUser();
        user.setProfilePicUrl(photoUrl);
        UserPreferences.getInstance().saveUser(user);
        add(getDataManager().saveUser3((long) user.getId(), user.getUsername(), user.getPassword(),
                user.getEmail(), user.getProfilePicUrl(), user.getBio())
                .subscribe());
        profilePicUrl.set(photoUrl);
    }

    public void checkForRemoteUpdatesToMyDP() {
        add(getDataManager().getUserById(myUserid())
                .subscribe(new Consumer<UserResponse>() {
                    @Override
                    public void accept(UserResponse userResponse) throws Exception {
                        if (userResponse.status.equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                            final User remote = userResponse.user;
                            if (!TextUtils.isEmpty(remote.getProfilePicUrl())) {
                                String localProfilePic = UserPreferences.getInstance().getUser().getProfilePicUrl() + "";
                                String remoteProfilePic = remote.getProfilePicUrl() + "";
                                if (StringUtil.isNotEmpty(remoteProfilePic) && !localProfilePic.equals(remoteProfilePic)) {
                                    User user = UserPreferences.getInstance().getUser();
                                    user.setProfilePicUrl(remoteProfilePic);
                                    UserPreferences.getInstance().saveUser(user);
                                    profilePicUrl.set(remote.getProfilePicUrl());
                                    MLog.i(TAG, "checkForRemoteUpdatesToMyDP() my pic changed remotely. attempt to update");
                                } else {
                                    MLog.i(TAG, "checkForRemoteUpdatesToMyDP() my pic did not change remotely. do not update.");
                                }
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        MLog.e(TAG, "checkForRemoteUpdatesToMyDP() failed", throwable);
                    }
                }));
    }

    public void saveUser(final User user, final String newUsername, final String newBio, final boolean needToSaveBio, final boolean needToSaveUsername) {
        if (needToSaveBio && !needToSaveUsername) {
            user.setBio(newBio);
            add(getDataManager().saveUser3((long) user.getId(),
                    user.getUsername(),
                    user.getPassword(),
                    user.getEmail(),
                    user.getProfilePicUrl(),
                    user.getBio())
                    .subscribe(new Consumer<UserResponse>() {
                        @Override
                        public void accept(UserResponse userResponse) throws Exception {
                            if (userResponse.status.equals(NetworkApi.RESPONSE_OK)) {
                                UserPreferences.getInstance().saveUser(user);
                                getNavigator().showProfileUpdatedDialog();
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            getNavigator().showErrorToast("");
                            Bundle payload = new Bundle();
                            payload.putString("why", throwable.toString());
                            payload.putString("username", UserPreferences.getInstance().getUsername() + "");
                            firebaseAnalytics.logEvent(Events.SAVED_PROFILE_FAILED, payload);
                        }
                    }));

        } else if (needToSaveUsername) {
            if (needToSaveBio)
                user.setBio(newBio);

            add(getDataManager().userNameExists(newUsername)
                    .subscribe(new Consumer<BasicExistenceResult>() {
                        @Override
                        public void accept(BasicExistenceResult basicExistenceResult) throws Exception {
                            if (basicExistenceResult.data.exists) {
                                getNavigator().showUsernameExistsDialog(newUsername);
                                username.set(UserPreferences.getInstance().getUsername());
                            } else {
                                user.setUsername(newUsername);
                                saveUser(user);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            getNavigator().showErrorToast("");
                            username.set(UserPreferences.getInstance().getUsername());
                            Bundle payload = new Bundle();
                            payload.putString("why", throwable.toString());
                            payload.putString("username", UserPreferences.getInstance().getUsername() + "");
                            firebaseAnalytics.getInstance(TheApp.getInstance()).logEvent(Events.SAVED_PROFILE_FAILED, payload);
                        }
                    }));

        }

    }

    private void saveUser(@NonNull final User user) {
        getDataManager().saveUser3((long) user.getId(), user.getUsername(), user.getPassword(), user.getEmail(), user.getProfilePicUrl(), user.getBio())
                .subscribe(new Consumer<UserResponse>() {
                    @Override
                    public void accept(UserResponse userResponse) throws Exception {
                        UserPreferences.getInstance().saveUser(user);
                        UserPreferences.getInstance().saveLastSignIn(user.getUsername());
                        Bundle payload = new Bundle();
                        payload.putString("username", UserPreferences.getInstance().getUsername() + "");
                        firebaseAnalytics.logEvent(Events.SAVED_PROFILE, payload);
                    }
                });
    }

    public void onGroupChatClicked(long groupId, String groupName) {
        if (groupId == 0 || StringUtil.isEmpty(groupName)) {
            return;
        }
        getNavigator().showGroupChatActivity(groupId, groupName, null, null);
    }

}
