package com.instachat.android.app.activity;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.BuildConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.TheApp;
import com.instachat.android.app.adapter.MessageViewHolder;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.bans.BanHelper;
import com.instachat.android.app.blocks.BlockedUserListener;
import com.instachat.android.app.ui.base.BaseViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.api.BasicExistenceResult;
import com.instachat.android.data.api.BasicResponse;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.api.UserResponse;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.SimpleRxWrapper;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.databinding.ObservableField;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

public abstract class AbstractChatViewModel<Navigator extends AbstractChatNavigator> extends BaseViewModel<Navigator> implements Executor{

    private static final String TAG = "AbstractChatViewModel";

    public ObservableField<String> profilePicUrl = new ObservableField<>("");
    public ObservableField<String> username = new ObservableField<>("");
    public ObservableField<String> bio = new ObservableField<>("");
    public ObservableField<Integer> likes = new ObservableField<>(0);
    public ObservableField<Integer> pendingRequests = new ObservableField<>(0);

    protected final FirebaseRemoteConfig firebaseRemoteConfig;
    protected final FirebaseDatabase firebaseDatabase;
    protected final BanHelper banHelper;
    protected FirebaseAnalytics firebaseAnalytics;

    private String databaseRoot;
    private DatabaseReference databaseReference;
    private MessagesRecyclerAdapter messagesAdapter;
    public static boolean isCaptchaVerified;

    public AbstractChatViewModel(DataManager dataManager,
                                 SchedulerProvider schedulerProvider,
                                 FirebaseRemoteConfig firebaseRemoteConfig,
                                 FirebaseDatabase firebaseDatabase,
                                 BanHelper banHelper) {
        super(dataManager, schedulerProvider);
        this.firebaseRemoteConfig = firebaseRemoteConfig;
        this.firebaseDatabase = firebaseDatabase;
        this.banHelper = banHelper;
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

    public DatabaseReference getDatabaseReference() {
        if (databaseReference == null) {
            databaseReference = firebaseDatabase.getReference(getDatabaseRoot());
        }
        return databaseReference;
    }

    private void applyRetrievedLengthLimit(FirebaseRemoteConfig firebaseRemoteConfig) {
        long maxMessageLength = firebaseRemoteConfig.getLong(Constants.KEY_MAX_MESSAGE_LENGTH);
        getNavigator().setMaxMessageLength((int) maxMessageLength);
    }

    public MessagesRecyclerAdapter createMessagesAdapter(FirebaseRemoteConfig firebaseRemoteConfig,
                                                         MessagesRecyclerAdapterHelper map) {
        messagesAdapter = new MessagesRecyclerAdapter<>(FriendlyMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                FirebaseDatabase.getInstance().getReference(getDatabaseRoot()).
                        limitToLast((int) firebaseRemoteConfig.getLong(Constants.KEY_MAX_MESSAGE_HISTORY)),
                map);
        messagesAdapter.setDatabaseRoot(getDatabaseRoot());
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

    public BlockedUserListener getBlockedUserListener() {
        return blockedUserListener;
    }

    private BlockedUserListener blockedUserListener = new BlockedUserListener() {
        @Override
        public void onUserBlocked(int userid) {
            Bundle payload = new Bundle();
            payload.putString("by", myUsername());
            payload.putInt("userid", userid);
            firebaseAnalytics.logEvent(Events.USER_BLOCKED, payload);
            messagesAdapter.blockUser(userid);
        }

        @Override
        public void onUserUnblocked(int userid) {
            Bundle payload = new Bundle();
            payload.putString("by", myUsername());
            payload.putInt("userid", userid);
            firebaseAnalytics.logEvent(Events.USER_UNBLOCKED, payload);
        }
    };


    private boolean canSendText(final String text, final String imageUrl) {

        if (!BuildConfig.DEBUG) {
            if (System.currentTimeMillis() - lastMessageSentTime < Constants.SPAM_BURST_DURATION) {
                if (messageCount >= Constants.SPAM_MAX_BURST_COMMENTS) {
                    getNavigator().showSlowDown();
                    if (stupidSpamAttempts > 2) {
                        lastMessageSentTime = System.currentTimeMillis();
                    } else {
                        stupidSpamAttempts++;
                    }
                    return false;
                }
            } else {
                //reset counter since it's been more than SPAM_BURST_DURATION since last sent
                messageCount = 0;
                stupidSpamAttempts = 0;
            }
        }

        if (imageUrl == null && StringUtil.isEmpty(text)) {
            return false;
        }
        if (isBanned()) {
            getNavigator().showYouHaveBeenBanned();
            return false;
        }
        if (StringUtil.isEmpty(myDpid())) {
            getNavigator().showNeedPhotoDialog();
            return false;
        }
        return true;
    }

    @Override
    public void execute(Runnable command) {
        SimpleRxWrapper.executeInUiThread(command);
    }

    private void _sendText(FriendlyMessage friendlyMessage) {
        lastMessageSentTime = System.currentTimeMillis();
        messageCount++;
        messagesAdapter.sendFriendlyMessage(friendlyMessage);
        getNavigator().clearTextField();
    }

    public void sendText(FriendlyMessage friendlyMessage) {

        if (!canSendText(friendlyMessage.getText(), friendlyMessage.getImageUrl())) {
            return;
        }

        if (isCaptchaVerified) {
            _sendText(friendlyMessage);
            return;
        }

        SafetyNet.getClient(TheApp.getInstance()).verifyWithRecaptcha("6LdO48cUAAAAAMKvpGgmjSlY7zzFaTZpuLFnl-Ab")
                .addOnSuccessListener((Executor) this,
                        new OnSuccessListener<SafetyNetApi.RecaptchaTokenResponse>() {
                            @Override
                            public void onSuccess(SafetyNetApi.RecaptchaTokenResponse response) {
                                // Indicates communication with reCAPTCHA service was
                                // successful.
                                String userResponseToken = response.getTokenResult();
                                if (!userResponseToken.isEmpty()) {
                                    // Validate the user response token using the
                                    // reCAPTCHA siteverify API.
                                    MLog.w(TAG,"safety token: " +userResponseToken);
                                    _sendText(friendlyMessage);
                                    isCaptchaVerified = true;
                                } else {
                                    getNavigator().showErrorToast("");
                                }
                            }
                        })
                .addOnFailureListener((Executor) this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        getNavigator().showErrorToast("");
                        if (e instanceof ApiException) {
                            // An error occurred when communicating with the
                            // reCAPTCHA service. Refer to the status code to
                            // handle the error appropriately.
                            ApiException apiException = (ApiException) e;
                            int statusCode = apiException.getStatusCode();
                            MLog.w(TAG, "Error: " + CommonStatusCodes
                                    .getStatusCodeString(statusCode));
                        } else {
                            // A different, unknown type of error occurred.
                            MLog.w(TAG, "Error: " + e.getMessage());
                        }
                    }
                });

    }

    private long lastMessageSentTime;
    private int messageCount;
    private int stupidSpamAttempts;
    public void sendText(final String text, boolean showOptions) {
        final FriendlyMessage friendlyMessage = new FriendlyMessage(text,
                myUsername(),
                myUserid(),
                myDpid(), null,
                false, false, null, System.currentTimeMillis());

        if (!showOptions) {
            sendText(friendlyMessage);
            return;
        }
        getNavigator().showSendOptions(friendlyMessage);
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
        inputContentInfo.getContentUri();
        Uri linkUri = inputContentInfo.getLinkUri();
        if (linkUri == null)
            linkUri = inputContentInfo.getContentUri();
        if (linkUri != null && inputContentInfo != null && inputContentInfo.getDescription() != null) {
            if (inputContentInfo.getDescription().toString().contains("image/gif")) {
                final FriendlyMessage friendlyMessage = new FriendlyMessage("",
                        myUsername(),
                        myUserid(),
                        myDpid(),
                        linkUri.toString(), false, false, null, System.currentTimeMillis());
                friendlyMessage.setMT(FriendlyMessage.MESSAGE_TYPE_NORMAL);
                sendText(friendlyMessage);
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

    public boolean isBanned() {
        boolean isBanned = banHelper.amIBanned();
        if (isBanned) {
            MLog.w(TAG, "You are banned. Cannot post anything.");
        }
        return isBanned;
    }

    /**
     * Check if the messages are out of sort order.  If so, then sort them.
     * Delay some time before doing the actual check.
     * Do not do simultaneous checks within 2 seconds.
     *
     */
    private long lastCheckTime;
    public void checkMessageSortOrder() {
        synchronized (this) {
            if ((lastCheckTime + 2100) > System.currentTimeMillis()) {
                MLog.d(TAG, "sort_tag check already in progress...");
                return;
            }
            lastCheckTime = System.currentTimeMillis();
        }
        add(Observable.timer(2100, TimeUnit.MILLISECONDS)
                .subscribeOn(getSchedulerProvider().io())
                .observeOn(getSchedulerProvider().ui())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        if (messagesAdapter.needsSorting()) {
                            messagesAdapter.sort();
                        }
                    }
                })
                .subscribe());
    }

    /**
     * Remove all messages sent by the user of the given message.
     *
     * @param friendlyMessage
     */
    public void removeMessages(FriendlyMessage friendlyMessage) {
        MLog.d(TAG,"sort_tag removeMessages check sort order");
        messagesAdapter.removeMessages(friendlyMessage);
        checkMessageSortOrder();
    }

    /**
     * Remove a single message.
     *
     * @param friendlyMessage
     * @return - the Task associated to the database remove operation
     */
    public Task<Void> removeMessage(final FriendlyMessage friendlyMessage) {
        return messagesAdapter.removeMessage(friendlyMessage);
    }
}
