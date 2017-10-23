package com.instachat.android.app.activity.group;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v13.view.inputmethod.EditorInfoCompat;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.MessageOptionsDialogHelper;
import com.instachat.android.app.activity.AbstractChatNavigator;
import com.instachat.android.app.activity.AdHelper;
import com.instachat.android.app.activity.AttachPhotoOptionsDialogHelper;
import com.instachat.android.app.activity.ExternalSendIntentConsumer;
import com.instachat.android.app.activity.LeftDrawerEventListener;
import com.instachat.android.app.activity.LeftDrawerHelper;
import com.instachat.android.app.activity.PhotoUploadHelper;
import com.instachat.android.app.activity.PresenceHelper;
import com.instachat.android.app.activity.UsersInGroupListener;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.app.adapter.ChatSummariesRecyclerAdapter;
import com.instachat.android.app.adapter.ChatsItemClickedListener;
import com.instachat.android.app.adapter.FriendlyMessageListener;
import com.instachat.android.app.adapter.GroupChatUsersRecyclerAdapter;
import com.instachat.android.app.adapter.MessageTextClickedListener;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.blocks.BlocksFragment;
import com.instachat.android.app.fullscreen.FriendlyMessageContainer;
import com.instachat.android.app.fullscreen.FullScreenTextFragment;
import com.instachat.android.app.likes.UserLikedUserFragment;
import com.instachat.android.app.likes.UserLikedUserListener;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.app.requests.RequestsFragment;
import com.instachat.android.app.ui.base.BaseActivity;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.data.model.User;
import com.instachat.android.databinding.ActivityMainBinding;
import com.instachat.android.font.FontUtil;
import com.instachat.android.gcm.GCMHelper;
import com.instachat.android.messaging.InstachatMessagingService;
import com.instachat.android.messaging.NotificationHelper;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;
import com.instachat.android.view.ThemedAlertDialog;
import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.ErrorCode;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.exception.AdReceiveFailed;
import com.tooltip.Tooltip;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.Observable;
import pub.devrel.easypermissions.EasyPermissions;

public class GroupChatActivity extends BaseActivity<ActivityMainBinding, GroupChatViewModel> implements GoogleApiClient.OnConnectionFailedListener,
        FriendlyMessageContainer, EasyPermissions.PermissionCallbacks, UploadListener, UserClickedListener,
        ChatsItemClickedListener, FriendlyMessageListener, AttachPhotoOptionsDialogHelper.PhotoOptionsListener,
        AdListenerInterface, GroupChatNavigator {

    private static final String TAG = "GroupChatActivity";

    private static final int REQUEST_INVITE = 1;

    @Inject
    SchedulerProvider schedulerProvider;

    @Inject
    FirebaseRemoteConfig firebaseRemoteConfig;

    @Inject
    MessagesRecyclerAdapterHelper map;

    @Inject
    PresenceHelper presenceHelper;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    LinearLayoutManager linearLayoutManager;

    MessagesRecyclerAdapter messagesAdapter;

    @Inject
    NetworkApi networkApi;

    @Inject
    GCMHelper gcmHelper;

    @Inject
    LogoutDialogHelper logoutDialogHelper;

    @Inject
    FirebaseAuth firebaseAuth;

    @Inject
    ChatSummariesRecyclerAdapter chatsRecyclerViewAdapter;

    @Inject
    AdHelper adsHelper;

    private Toolbar mToolbar;

    private EditText mMessageEditText;

    private ProgressDialog mProgressDialog;

    private PhotoUploadHelper mPhotoUploadHelper;
    private LeftDrawerHelper mLeftDrawerHelper;
    private GroupChatUsersRecyclerAdapter mGroupChatUsersRecyclerAdapter;
    private ExternalSendIntentConsumer mExternalSendIntentConsumer;
    private Uri mSharePhotoUri;
    private String mShareText;
    private boolean mIsPendingRequestsAvailable;
    private ActivityMainBinding binding;
    private GroupChatViewModel groupChatViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupChatViewModel.setNavigator(this);
        groupChatViewModel.onCreate();
        initDatabaseRef();
        binding =  getViewDataBinding();

        initPhotoHelper(savedInstanceState);
        setupDrawers();
        setupToolbar();

        gcmHelper.onCreate(this);

        linearLayoutManager.setStackFromEnd(true);

        NotificationHelper.createNotificationChannels(this);

        initFirebaseAdapter();
        binding.messageRecyclerView.setLayoutManager(linearLayoutManager);
        binding.messageRecyclerView.setAdapter(messagesAdapter);

        binding.setVisibleAd(true);
        adsHelper.loadAd(this);

        mMessageEditText = createEditTextWithContentMimeTypes(
                new String[]{"image/png", "image/gif", "image/jpeg", "image/webp"});
        binding.messageEditTextParent.addView(mMessageEditText);
        FontUtil.setTextViewFont(mMessageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter((int) firebaseRemoteConfig
                .getLong(Constants.KEY_MAX_MESSAGE_LENGTH))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

                int length = mMessageEditText.getText().toString().trim().length();
                //MLog.i(TAG, "input onTextChanged() text [start]: " + start + " [before]: " + before + " [count]: "
                // + count, " last delta: ", lastDelta, " length: ", length);

                if (length > 0) {
                    setEnableSendButton(true);
                    groupChatViewModel.onMeTyping();
                    showSendOptionsTooltip(binding.sendButton);
                } else {
                    setEnableSendButton(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        groupChatViewModel.fetchConfig(firebaseRemoteConfig);
        initButtons();

        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_GROUP_NAME)) {
            getSupportActionBar().setTitle(getIntent().getStringExtra(Constants.KEY_GROUP_NAME));
        }

        mExternalSendIntentConsumer = new ExternalSendIntentConsumer(this);
        mExternalSendIntentConsumer.setListener(new ExternalSendIntentConsumer.ExternalSendIntentListener() {
            @Override
            public void onHandleSendImage(final Uri imageUri) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
                mSharePhotoUri = imageUri;
            }

            @Override
            public void onHandleSendText(final String text) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
                mShareText = text;
            }
        });
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_SHARE_PHOTO_URI)) {
            mPhotoUploadHelper.setStorageRefString(groupChatViewModel.getDatabaseRoot());
            mPhotoUploadHelper.consumeExternallySharedPhoto((Uri) getIntent().getParcelableExtra(Constants
                    .KEY_SHARE_PHOTO_URI));
            getIntent().removeExtra(Constants.KEY_SHARE_PHOTO_URI);
        }
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_SHARE_MESSAGE)) {
            mMessageEditText.setText(getIntent().getStringExtra(Constants.KEY_SHARE_MESSAGE));
            getIntent().removeExtra(Constants.KEY_SHARE_MESSAGE);
        }
        groupChatViewModel.listenForTyping();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(InstachatMessagingService.NOTIFICATION_ID_PENDING_REQUESTS);
        notificationManager.cancel(InstachatMessagingService.NOTIFICATION_ID_FRIEND_JUMPED_IN);
        groupChatViewModel.smallProgressCheck();
    }

    private DatabaseReference mGroupSummaryRef;
    private ValueEventListener mGroupSummaryListener;

    private void removeGroupInfoListener() {
        if (mGroupSummaryRef != null && mGroupSummaryListener != null) {
            mGroupSummaryRef.removeEventListener(mGroupSummaryListener);
        }
    }

    private String getCount(int count) {
        if (count > 0)
            return " (" + count + ")";
        return "";
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.sendButton.setEnabled(mMessageEditText.getText().toString().trim().length() > 0);
        if (UserPreferences.getInstance().isLoggedIn()) {
            gcmHelper.onResume(this);
            showFirstMessageDialog(GroupChatActivity.this);
            if (mExternalSendIntentConsumer != null)
                mExternalSendIntentConsumer.consumeIntent(getIntent());
            addUserPresenceToGroup();
        } else {
            finish();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        removeUserPresenceFromGroup();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mPhotoUploadHelper.getPhotoType() != null) {
            outState.putString(Constants.KEY_PHOTO_TYPE, mPhotoUploadHelper.getPhotoType().name());
            MLog.d(TAG, "onSaveInstanceState() saving photo type: " + mPhotoUploadHelper.getPhotoType().name());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (mRightRef != null)
            mRightRef.removeEventListener(mRightListener);
        if (mPhotoUploadHelper != null)
            mPhotoUploadHelper.cleanup();
        if (messagesAdapter != null)
            messagesAdapter.cleanup();
        if (mLeftDrawerHelper != null)
            mLeftDrawerHelper.cleanup();
        if (chatsRecyclerViewAdapter != null)
            chatsRecyclerViewAdapter.cleanup();
        if (mExternalSendIntentConsumer != null)
            mExternalSendIntentConsumer.cleanup();
        if (mGroupChatUsersRecyclerAdapter != null)
            mGroupChatUsersRecyclerAdapter.cleanup();
        groupChatViewModel.cleanup();
        super.onDestroy();
    }

    protected void initDatabaseRef() {
        String databaseRef;
        long groupId;
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_GROUPID)) {
            groupId = getIntent().getLongExtra(Constants.KEY_GROUPID, Constants.DEFAULT_PUBLIC_GROUP_ID);
            databaseRef = Constants.GROUP_CHAT_REF(groupId);
            UserPreferences.getInstance().setLastGroupChatRoomVisited(groupId);
        } else {
            groupId = UserPreferences.getInstance().getLastGroupChatRoomVisited();
            databaseRef = Constants.GROUP_CHAT_REF(groupId);
        }
        groupChatViewModel.setDatabaseRoot(databaseRef);
        groupChatViewModel.setGroupId(groupId);
    }

    private void setEnableSendButton(final boolean isEnable) {

        if (isEnable && binding.sendButton.isEnabled() || !isEnable && !binding.sendButton.isEnabled())
            return; //already set

        binding.sendButton.setEnabled(isEnable);

        final Animation hideAnimation = AnimationUtils.loadAnimation(GroupChatActivity.this, R.anim.fab_scale_down);
        final Animation showAnimation = AnimationUtils.loadAnimation(GroupChatActivity.this, R.anim.fab_scale_up);

        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.sendButton.startAnimation(showAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        binding.sendButton.startAnimation(hideAnimation);
    }

    private void initPhotoHelper(Bundle savedInstanceState) {
        mPhotoUploadHelper = new PhotoUploadHelper(this, this);
        mPhotoUploadHelper.setStorageRefString(groupChatViewModel.getDatabaseRoot());
        mPhotoUploadHelper.setPhotoUploadListener(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY_PHOTO_TYPE)) {
            PhotoUploadHelper.PhotoType photoType = PhotoUploadHelper.PhotoType.valueOf(savedInstanceState.getString
                    (Constants.KEY_PHOTO_TYPE));
            mPhotoUploadHelper.setPhotoType(photoType);
            MLog.d(TAG, "initPhotoHelper: retrieved from saved instance state: " + photoType);
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgressNumberFormat("%1dk / %2dk");
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private int mAttachPhotoMessageType;

    private void initButtons() {
        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String text = mMessageEditText.getText().toString();
                validateBeforeSendText(text, false);
            }
        });
        binding.sendButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final String text = mMessageEditText.getText().toString();
                validateBeforeSendText(text, true);
                return true;
            }
        });
        binding.attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNeedsDp())
                    return;
                mAttachPhotoMessageType = FriendlyMessage.MESSAGE_TYPE_NORMAL;
                showFileOptions();
            }
        });
        binding.attachButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new MessageOptionsDialogHelper().showSendOptions(GroupChatActivity.this, binding.attachButton, null, new
                        MessageOptionsDialogHelper.SendOptionsListener() {
                            @Override
                            public void onSendNormalRequested(FriendlyMessage friendlyMessage) {
                                mAttachPhotoMessageType = FriendlyMessage.MESSAGE_TYPE_NORMAL;
                                showFileOptions();
                            }

                            @Override
                            public void onSendOneTimeRequested(FriendlyMessage friendlyMessage) {
                                mAttachPhotoMessageType = FriendlyMessage.MESSAGE_TYPE_ONE_TIME;
                                showFileOptions();
                            }
                        });
                return true;
            }
        });
    }

    private void sendText(FriendlyMessage friendlyMessage) {
        try {
            messagesAdapter.sendFriendlyMessage(friendlyMessage);
            mMessageEditText.setText("");//fast double taps on send can cause 2x sends!
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    private boolean validateBeforeSendText(final String text, boolean showOptions) {
        if (StringUtil.isEmpty(text)) {
            return false;
        }
        if (isNeedsDp())
            return false;
        final FriendlyMessage friendlyMessage = new FriendlyMessage(text,
                groupChatViewModel.myUsername(),
                groupChatViewModel.myUserid(),
                groupChatViewModel.myDpid(), null,
                false, false, null, System.currentTimeMillis());
        if (!showOptions) {
            sendText(friendlyMessage);
            return true;
        }
        new MessageOptionsDialogHelper().showSendOptions(GroupChatActivity.this, binding.sendButton, friendlyMessage, new
                MessageOptionsDialogHelper.SendOptionsListener() {
                    @Override
                    public void onSendNormalRequested(FriendlyMessage friendlyMessage) {
                        friendlyMessage.setMessageType(FriendlyMessage.MESSAGE_TYPE_NORMAL);
                        sendText(friendlyMessage);
                    }

                    @Override
                    public void onSendOneTimeRequested(FriendlyMessage friendlyMessage) {
                        friendlyMessage.setMessageType(FriendlyMessage.MESSAGE_TYPE_ONE_TIME);
                        sendText(friendlyMessage);
                    }
                });
        return true;
    }

    /**
     * {@link FriendlyMessageListener}
     * @param friendlyMessage
     */
    @Override
    public void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage) {
        try {
            if (isActivityDestroyed())
                return;
            MLog.d(TAG, "C kevin scroll: " + (messagesAdapter.getItemCount() - 1) + " text: " + messagesAdapter.peekLastMessage());
            binding.messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
            presenceHelper.updateLastActiveTimestamp();
        } catch (final Exception e) {
            MLog.e(TAG, "", e);
        }
        Bundle payload = new Bundle();
        payload.putString("from", groupChatViewModel.myUsername());
        payload.putString("type", friendlyMessage.getImageUrl() != null ? "photo" : "text");
        payload.putLong("group", groupChatViewModel.getGroupId());
        payload.putBoolean("one-time", friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME);
        FirebaseAnalytics.getInstance(this).logEvent(Events.MESSAGE_GROUP_SENT_EVENT, payload);
    }

    /**
     * {@link FriendlyMessageListener}
     * @param friendlyMessage
     */
    @Override
    public void onFriendlyMessageFail(FriendlyMessage friendlyMessage) {
        if (isActivityDestroyed())
            return;
        mMessageEditText.setText(friendlyMessage.getText());
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string
                .could_not_send_message)).show();
        FirebaseAnalytics.getInstance(this).logEvent(Events.MESSAGE_FAILED, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_chat_options_menu, menu);
        return true;
    }

    protected void onHomeClicked() {
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onHomeClicked();
                return true;
            case R.id.menu_manage_blocks:
                if (isLeftDrawerOpen()) {
                    closeLeftDrawer();
                }
                Fragment fragment = new BlocksFragment();
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim
                        .slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment,
                        BlocksFragment.TAG).addToBackStack(null).commit();
                return true;
            case R.id.menu_who_is_online:
                if (isLeftDrawerOpen()) {
                    closeLeftDrawer();
                }
                if (!isRightDrawerOpen()) {
                    openRightDrawer();
                }
                return true;
            case R.id.menu_invite:
                sendInvitation();
                return true;
            case R.id.menu_sign_out:
                signout();
                return true;
            case R.id.fresh_config_menu:
                groupChatViewModel.fetchConfig(firebaseRemoteConfig);
                return true;
            case R.id.full_screen_texts_menu:
                openFullScreenTextView(-1);
                return true;
            case R.id.menu_pending_requests:
                onPendingRequestsClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title)).setMessage
                (getString(R.string.invitation_message)).setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MLog.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        mPhotoUploadHelper.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                MLog.d(TAG, "Invitations sent: " + ids.length);
                payload.putInt("num_inv", ids.length);
                payload.putString("username", groupChatViewModel.myUsername());
                FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SHARE, payload);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SHARE, payload);
                // Sending failed or it was canceled, show failure message to the user
                MLog.d(TAG, "Failed to send invitation.");
            }
        }

    }

    private void showPhotoReduceError() {
        addDisposable(Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Toast.makeText(GroupChatActivity.this, "Could not read photo", Toast.LENGTH_SHORT).show();
                return false;
            }
        }).subscribeOn(schedulerProvider.ui()).subscribe());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        MLog.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void setupToolbar() {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);
        setupToolbarTitle(mToolbar);
        setToolbarOnClickListener(mToolbar);
    }

    protected Toolbar getToolbar() {
        return mToolbar;
    }

    protected void setToolbarOnClickListener(Toolbar toolbar) {
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRightDrawer();
            }
        });
    }

    private void setupDrawers() {
        setupLeftDrawerContent();
        setupRightDrawerContent();
    }

    private LeftDrawerEventListener mLeftDrawerEventListener = new LeftDrawerEventListener() {
        @Override
        public void onProfilePicChangeRequest(boolean isLaunchCamera) {
            GroupChatActivity.this.onProfilePicChangeRequest(isLaunchCamera);
        }

        @Override
        public void onPendingRequestsClicked() {
            GroupChatActivity.this.onPendingRequestsClicked();
        }

        @Override
        public void onPendingRequestsAvailable() {
            mIsPendingRequestsAvailable = true;
            ActionBar ab = getSupportActionBar();
            if (ab != null)
                ab.setHomeAsUpIndicator(R.drawable.ic_menu_new);
        }

        @Override
        public void onPendingRequestsCleared() {
            mIsPendingRequestsAvailable = false;
            ActionBar ab = getSupportActionBar();
            if (ab != null)
                ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        }
    };

    private void onProfilePicChangeRequest(boolean isLaunchCamera) {
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.userProfilePhoto);
        mPhotoUploadHelper.setStorageRefString(Constants.DP_STORAGE_BASE_REF(groupChatViewModel.myUserid()));
        mPhotoUploadHelper.launchCamera(isLaunchCamera);
    }

    private void setupLeftDrawerContent() {
        View headerView = getLayoutInflater().inflate(R.layout.left_nav_header, binding.navView, false);
        View drawerView = getLayoutInflater().inflate(R.layout.left_drawer_layout, binding.navView, false);
        binding.navView.addView(drawerView);
        binding.navView.addHeaderView(headerView);
        mLeftDrawerHelper = new LeftDrawerHelper(networkApi, this, this, binding.drawerLayout, mLeftDrawerEventListener);
        mLeftDrawerHelper.setup(binding.navView);
        mLeftDrawerHelper.setUserLikedUserListener(mUserLikedUserListener);

        chatsRecyclerViewAdapter.setup(this, this, false);
        RecyclerView recyclerView = (RecyclerView) drawerView.findViewById(R.id.drawerRecyclerView);
        recyclerView.setLayoutManager(new StickyLayoutManager(this, chatsRecyclerViewAdapter));
        recyclerView.setAdapter(chatsRecyclerViewAdapter);
        chatsRecyclerViewAdapter.populateData();
        UsersInGroupListener usersInGroupListener = new UsersInGroupListener() {
            @Override
            public void onNumUsersUpdated(long groupId, String groupName, int numUsers) {
                if (getSupportActionBar() != null && groupId == groupChatViewModel.getGroupId())
                    getSupportActionBar().setTitle(groupName + getCount(numUsers));
            }
        };
        chatsRecyclerViewAdapter.setUsersInGroupListener(usersInGroupListener);
    }

    private DatabaseReference mRightRef;
    private ValueEventListener mRightListener;

    protected void setupRightDrawerContent() {

        RecyclerView drawerRecyclerView = (RecyclerView)getLayoutInflater().inflate(R.layout.right_drawer_layout, binding.rightNavView, false);
        final View headerView = getLayoutInflater().inflate(R.layout.right_nav_header, binding.rightNavView, false);

        mRightRef = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_ROOMS).child
                (groupChatViewModel.getGroupId() + "");
        mRightListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "setupRightDrawerContent() dataSnapshot: ", dataSnapshot);
                GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                if (groupChatSummary == null || groupChatSummary.getName() == null) {
                    /*
                     * group was deleted; go to the global default public room
                     */
                    startGroupChatActivity(GroupChatActivity.this, Constants.DEFAULT_PUBLIC_GROUP_ID, "Main", null,
                            null);
                    return;
                }
                ((TextView) headerView.findViewById(R.id.groupname)).setText(groupChatSummary.getName());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mRightRef.addValueEventListener(mRightListener);

        binding.rightNavView.addHeaderView(headerView);
        binding.rightNavView.addHeaderView(drawerRecyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        drawerRecyclerView.setLayoutManager(linearLayoutManager);

        mGroupChatUsersRecyclerAdapter = new GroupChatUsersRecyclerAdapter(this, this, this, groupChatViewModel.getGroupId());
        drawerRecyclerView.setAdapter(mGroupChatUsersRecyclerAdapter);
        mGroupChatUsersRecyclerAdapter.populateData();
    }

    private boolean isLeftDrawerOpen() {
        return binding.drawerLayout != null && binding.drawerLayout.isDrawerOpen(GravityCompat.START);
    }

    private boolean isRightDrawerOpen() {
        return binding.drawerLayout != null && binding.drawerLayout.isDrawerOpen(GravityCompat.END);
    }

    private void closeLeftDrawer() {
        if (binding.drawerLayout != null)
            binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void closeRightDrawer() {
        if (binding.drawerLayout != null)
            binding.drawerLayout.closeDrawer(GravityCompat.END);
    }

    private void openRightDrawer() {
        if (binding.drawerLayout != null)
            binding.drawerLayout.openDrawer(GravityCompat.END);
    }

    private void openLeftDrawer() {
        if (binding.drawerLayout != null)
            binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    private boolean closeBothDrawers() {
        boolean atLeastOneClosed = false;
        if (isRightDrawerOpen()) {
            closeRightDrawer();
            atLeastOneClosed = true;
        }
        if (isLeftDrawerOpen()) {
            closeLeftDrawer();
            atLeastOneClosed = true;
        }
        return atLeastOneClosed;
    }

    @Override
    public void onBackPressed() {
        if (closeBothDrawers()) {
            return;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        super.onBackPressed();
    }

    protected void openFullScreenTextView(final int startingPos) {
        closeBothDrawers();
        ScreenUtil.hideVirtualKeyboard(mMessageEditText);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
        if (fragment != null) {
            return;
        }
        fragment = new FullScreenTextFragment();
        final Bundle args = new Bundle();
        args.putInt(Constants.KEY_STARTING_POS, startingPos);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim
                .slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, FullScreenTextFragment.TAG)
                .addToBackStack(null).commit();
    }

    private void notifyPagerAdapterDataSetChanged() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
        if (fragment == null) {
            return;
        }
        ((FullScreenTextFragment) fragment).notifyDataSetChanged();
    }

    /**
     * Implementation of {@link FriendlyMessageContainer}
     */
    @Override
    public FriendlyMessage getFriendlyMessage(int position) {
        return (FriendlyMessage) messagesAdapter.getItem(position);
    }

    /**
     * Implementation of {@link FriendlyMessageContainer}
     */
    @Override
    public String getFriendlyMessageDatabase() {
        return groupChatViewModel.getDatabaseRoot();
    }

    /**
     * Implementation of {@link FriendlyMessageContainer}
     */
    @Override
    public int getFriendlyMessageCount() {
        return messagesAdapter.getItemCount();
    }

    /**
     * Implementation of {@link FriendlyMessageContainer}
     */
    @Override
    public void setCurrentFriendlyMessage(int position) {
        MLog.d(TAG, "A kevin scroll: " + (position + 1) + " text: " + messagesAdapter.peekLastMessage());
        binding.messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        MLog.i(TAG, "onRequestPermissionsResult() requestCode: " + requestCode);
        for (int i = 0; permissions != null && i < permissions.length; i++) {
            MLog.i(TAG, "onRequestPermissionsResult() requestCode: " + requestCode, " ", "permission ", permissions[i]);
        }
        for (int i = 0; grantResults != null && i < grantResults.length; i++) {
            MLog.i(TAG, "onRequestPermissionsResult() requestCode: " + requestCode, " ", "grant result ",
                    grantResults[i]);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        mPhotoUploadHelper.onPermissionsGranted(requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    private void initFirebaseAdapter() {
        messagesAdapter = groupChatViewModel.getMessagesAdapter(firebaseRemoteConfig, map);
        messagesAdapter.setActivity(this, this, binding.fragmentContent);
        messagesAdapter.setMessageTextClickedListener(new MessageTextClickedListener() {
            @Override
            public void onMessageClicked(final int position) {
                openFullScreenTextView(position);
            }
        });
        messagesAdapter.setFriendlyMessageListener(this);
        messagesAdapter.setUserThumbClickedListener(this);
        messagesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {

                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = messagesAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
                MLog.d(TAG, "scroll debug: lastVisiblePosition: " + lastVisiblePosition + " text: " + messagesAdapter.peekLastMessage()
                        + " positionStart: " + positionStart + " friendlyMessageCount: " + friendlyMessageCount);
                if (lastVisiblePosition == -1 || ((lastVisiblePosition + 4) >= positionStart)) {
                    MLog.d(TAG, "B kevin scroll: " + (positionStart) + " text: " + messagesAdapter.peekLastMessage());
                    binding.messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
                }
                notifyPagerAdapterDataSetChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                notifyPagerAdapterDataSetChanged();
            }
        });
    }

    private void showFileOptions() {

        /**
         * if the keyboard is open, close it first before showing
         * the bottom dialog otherwise there is flicker.
         * The delay is bad, but it works for now.
         */
        if (mMessageEditText.hasFocus()) {
            ScreenUtil.hideVirtualKeyboard(mMessageEditText);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isActivityDestroyed())
                        return;
                    showPhotoOptionsDialog();
                }
            }, 175);
        } else {
            showPhotoOptionsDialog();
        }
    }

    private void showPhotoOptionsDialog() {
        new AttachPhotoOptionsDialogHelper(this, this).showBottomDialog();
    }

    @Override
    public void onPhotoGallery() {
        mPhotoUploadHelper.setStorageRefString(groupChatViewModel.getDatabaseRoot());
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
        mPhotoUploadHelper.launchCamera(false);
    }

    @Override
    public void onPhotoTake() {
        mPhotoUploadHelper.setStorageRefString(groupChatViewModel.getDatabaseRoot());
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
        mPhotoUploadHelper.launchCamera(true);
    }

    @Override
    public void onErrorReducingPhotoSize() {
        MLog.i(TAG, "onErrorReducingPhotoSize()");
        if (isActivityDestroyed())
            return;
        showPhotoReduceError();
    }

    @Override
    public void onPhotoUploadStarted() {
        MLog.i(TAG, "onPhotoUploadStarted()");
        if (isActivityDestroyed())
            return;
        showProgressDialog();
    }

    @Override
    public void onPhotoUploadProgress(int max, int current) {
        MLog.i(TAG, "onPhotoUploadProgress() " + current + " / " + max);
        if (isActivityDestroyed())
            return;
        if (mProgressDialog != null) {
            try {
                mProgressDialog.setMax(max);
                mProgressDialog.setProgress(current);
            } catch (Exception e) {
                MLog.e(TAG, "set photo upload progress failed ", e);
            }
        }
    }

    @Override
    public void onPhotoUploadSuccess(String photoUrl, boolean isPossiblyAdultImage, boolean isPossiblyViolentImage) {
        if (isActivityDestroyed()) {
            return;
        }
        hideProgressDialog();

        if (mPhotoUploadHelper.getPhotoType() == PhotoUploadHelper.PhotoType.chatRoomPhoto) {

            final FriendlyMessage friendlyMessage = new FriendlyMessage("", groupChatViewModel.myUsername(), groupChatViewModel.myUserid(), groupChatViewModel.myDpid(),
                    photoUrl, isPossiblyAdultImage, isPossiblyViolentImage, null, System.currentTimeMillis());
            friendlyMessage.setMessageType(mAttachPhotoMessageType);
            MLog.d(TAG, "uploadFromUri:onSuccess photoUrl: " + photoUrl, " debug possibleAdult: ", friendlyMessage
                    .isPossibleAdultImage(), " parameter: ", isPossiblyAdultImage);
            try {
                messagesAdapter.sendFriendlyMessage(friendlyMessage);
            } catch (final Exception e) {
                MLog.e(TAG, "", e);
            }

        } else if (mPhotoUploadHelper.getPhotoType() == PhotoUploadHelper.PhotoType.userProfilePhoto) {

            final User user = UserPreferences.getInstance().getUser();
            user.setProfilePicUrl(photoUrl);
            UserPreferences.getInstance().saveUser(user);
            networkApi.saveUser(null, user, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    MLog.d(TAG, "saveUser() success via uploadFromUri(): " + response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    MLog.e(TAG, "saveUser() failed via uploadFromUri() ", error);
                }
            });
            mLeftDrawerHelper.updateProfilePic(photoUrl);
        }
    }

    private boolean isNeedsDp() {

        if (!TextUtils.isEmpty(groupChatViewModel.myDpid()))
            return false;
        new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE).setTitleText(this.getString(R.string
                .display_photo_title)).setContentText(this.getString(R.string.display_photo)).setCancelText(this
                .getString(android.R.string.cancel)).setConfirmText(this.getString(android.R.string.ok))
                .showCancelButton(true).setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                sweetAlertDialog.cancel();
            }
        }).setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                sweetAlertDialog.dismiss();
                if (isRightDrawerOpen())
                    closeRightDrawer();
                if (!isLeftDrawerOpen()) {
                    openLeftDrawer();
                }
            }
        }).show();
        return true;
    }

    @Override
    public void onPhotoUploadError(Exception exception) {
        MLog.i(TAG, "onPhotoUploadError() ", exception);
        if (isActivityDestroyed())
            return;
        hideProgressDialog();
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.error_send_photo))
                .show();
    }

    @Override
    public void onUserClicked(int userid, String username, String dpid, View transitionImageView) {
        closeBothDrawers();
        ScreenUtil.hideVirtualKeyboard(mMessageEditText);
        PrivateChatActivity.startPrivateChatActivity(this, userid, username, dpid, false, transitionImageView, null,
                null);
    }

    @Override
    public void showUserTyping(String username) {
        if (isActivityDestroyed()) {
            return;
        }
        binding.usernameTyping.setText(username);
        showTypingDots();
    }

    private Handler mDotsHandler = new Handler();
    private Runnable mDotsHideRunner = new Runnable() {
        @Override
        public void run() {
            if (isActivityDestroyed())
                return;
            hideDotsParent();
        }
    };

    protected void showTypingDots() {
        showDotsParent(true);
        mDotsHandler.removeCallbacks(mDotsHideRunner);
        mDotsHandler.postDelayed(mDotsHideRunner, firebaseRemoteConfig.getLong(Constants
                .KEY_MAX_TYPING_DOTS_DISPLAY_TIME));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        closeBothDrawers();
    }

    @Override
    public void onGroupChatClicked(GroupChatSummary groupChatSummary) {
        removeUserPresenceFromGroup();
        closeBothDrawers();
        startGroupChatActivity(this, groupChatSummary.getId(), groupChatSummary.getName(), mSharePhotoUri, mShareText);
        mSharePhotoUri = null;
        mShareText = null;
    }

    @Override
    public void onPrivateChatClicked(PrivateChatSummary privateChatSummary) {
        closeBothDrawers();
        PrivateChatActivity.startPrivateChatActivity(this, Integer.parseInt(privateChatSummary.getId()),
                privateChatSummary.getName(), privateChatSummary.getDpid(), false, null, mSharePhotoUri, mShareText);
        mSharePhotoUri = null;
        mShareText = null;
    }

    private void hideDotsParent() {
        if (binding.dotsLayout.getVisibility() == View.GONE)
            return;
        binding.dotsLayout.setVisibility(View.GONE);
    }

    private void showDotsParent(boolean isAnimate) {
        if (binding.dotsLayout.getVisibility() == View.VISIBLE)
            return;
        binding.dotsLayout.setVisibility(View.VISIBLE);
        if (isAnimate)
            AnimationUtil.fadeInAnimation(binding.dotsLayout);
    }

    public static void startGroupChatActivity(Context context, long groupId, String groupName, Uri sharePhotoUri,
                                              String shareMessage) {
        Intent intent = newIntent(context, groupId, groupName);
        if (sharePhotoUri != null)
            intent.putExtra(Constants.KEY_SHARE_PHOTO_URI, sharePhotoUri);
        if (shareMessage != null)
            intent.putExtra(Constants.KEY_SHARE_MESSAGE, shareMessage);
        context.startActivity(intent);
    }

    public static Intent newIntent(Context context, long groupId, String groupName) {
        Intent intent = new Intent(context, GroupChatActivity.class);
        intent.putExtra(Constants.KEY_GROUPID, groupId);
        intent.putExtra(Constants.KEY_GROUP_NAME, groupName);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void setupToolbarTitle(Toolbar toolbar) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                FontUtil.setTextViewFont((TextView) view);
                break;
            }
        }
    }

    protected void addUserPresenceToGroup() {

        mGroupSummaryRef = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_ROOMS).
                child(groupChatViewModel.getGroupId() + "");
        mGroupSummaryListener = mGroupSummaryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    final GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                    getSupportActionBar().setSubtitle(R.string.app_name);

                    /**
                     * run this delayed, if the user re-enters
                     * the same room (for a variety of reasons)
                     * give them some time to remove themself
                     * before immediately adding them back again.
                     */
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isActivityDestroyed())
                                return;

                            MLog.d(TAG, "addUserPresenceToGroup() groupChatViewModel.getGroupId(): ", groupChatViewModel.getGroupId(), " username: ", groupChatViewModel.myUsername());
                            User me = UserPreferences.getInstance().getUser();
                            final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants
                                    .GROUP_CHAT_USERS_REF(groupChatViewModel.getGroupId())).
                                    child(groupChatViewModel.myUserid() + "");
                            ref.updateChildren(me.toMap(true)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (chatsRecyclerViewAdapter != null)
                                        chatsRecyclerViewAdapter.removeUserFromAllGroups(groupChatViewModel.myUserid(), groupChatViewModel.getGroupId());
                                }
                            });
                            me.setCurrentGroupId(groupChatSummary.getId());
                            me.setCurrentGroupName(groupChatSummary.getName());
                            FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(groupChatViewModel.myUserid()))
                                    .updateChildren(me.toMap(true));

                        }
                    }, 2000);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    protected void removeUserPresenceFromGroup() {
        removeGroupInfoListener();
        MLog.d(TAG, "removeUserPresenceFromGroup() groupChatViewModel.getGroupId(): ", groupChatViewModel.getGroupId(), " username: ", groupChatViewModel.myUsername());
        FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_REF(groupChatViewModel.getGroupId())).child(groupChatViewModel.myUserid() + "")
                .removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(groupChatViewModel.myUserid())).child(Constants
                .FIELD_CURRENT_GROUP_ID).removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(groupChatViewModel.myUserid())).child(Constants
                .FIELD_CURRENT_GROUP_NAME).removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_TYPING_REF(groupChatViewModel.getGroupId(), groupChatViewModel.myUserid()))
                .removeValue();
    }

    private void toggleRightDrawer() {
        if (isRightDrawerOpen()) {
            closeRightDrawer();
            return;
        } else if (isLeftDrawerOpen()) {
            closeLeftDrawer();
        }
        openRightDrawer();
    }

    private void signout() {

        logoutDialogHelper.showLogoutDialog(this, new LogoutDialogHelper.LogoutListener() {
            @Override
            public void onConfirmLogout() {
                firebaseAuth.signOut();
                removeUserPresenceFromGroup();
                gcmHelper.unregister(UserPreferences.getInstance().getUserId() + "");
                UserPreferences.getInstance().clearUser();
                startActivity(new Intent(GroupChatActivity.this, SignInActivity.class));
                finish();
            }
        });
    }

    private UserLikedUserListener mUserLikedUserListener = new UserLikedUserListener() {
        @Override
        public void onMyLikersClicked() {
            Fragment fragment = new UserLikedUserFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.KEY_USERID, groupChatViewModel.myUserid());
            bundle.putString(Constants.KEY_USERNAME, groupChatViewModel.myUsername());
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R
                    .anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, UserLikedUserFragment
                    .TAG).addToBackStack(null).commit();
        }
    };

    private boolean mShownSendOptionsProtips;

    private void showSendOptionsTooltip(View anchor) {
        //        if (UserPreferences.getInstance().hasShownToolbarProfileTooltip())
        //            return;
        //        UserPreferences.getInstance().setShownToolbarProfileTooltip(true);
        if (mShownSendOptionsProtips) {
            return;
        }
        mShownSendOptionsProtips = true;
        final Tooltip tooltip = new Tooltip.Builder(anchor, R.style.drawer_tooltip_non_cancellable).setText(getString
                (R.string.send_option_protips)).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                if (tooltip.isShowing())
                    tooltip.dismiss();
            }
        }, 2000);

    }

    private void onPendingRequestsClicked() {
        closeLeftDrawer();
        Fragment fragment = new RequestsFragment();
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim
                .slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, RequestsFragment.TAG)
                .addToBackStack(null).commit();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu == null)
            return false;
        if (!mIsPendingRequestsAvailable) {
            if (menu.findItem(R.id.menu_pending_requests) != null)
                menu.removeItem(R.id.menu_pending_requests);
        } else {
            if (menu.findItem(R.id.menu_pending_requests) == null)
                menu.add(0, R.id.menu_pending_requests, 0, getString(R.string.menu_option_pending_requests));
        }
        if (messagesAdapter != null && messagesAdapter.getNumBlockedUsers() > 0) {
            if (menu.findItem(R.id.menu_manage_blocks) == null) {
                menu.add(0, R.id.menu_manage_blocks, 1, getString(R.string.manage_blocks));
            }
        } else {
            if (menu.findItem(R.id.menu_manage_blocks) != null)
                menu.removeItem(R.id.menu_manage_blocks);
        }
        /**
         * very hacky!
         */
        if (!groupChatViewModel.isPrivateChat() && groupChatViewModel.myUserid() > 10) {
            if (menu.findItem(R.id.menu_sign_out) != null) {
                menu.removeItem(R.id.menu_sign_out);
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    private void showFirstMessageDialog(@NonNull final Context context) {
        if (UserPreferences.getInstance().hasShownSendFirstMessageDialog()) {
            return;
        }
        final View view = getLayoutInflater().inflate(R.layout.dialog_input_comment, null);
        final TextView textView = (TextView) view.findViewById(R.id.input_text);
        final TextView textViewTitle = (TextView) view.findViewById(R.id.intro_message_title);
        textViewTitle.setText(getString(R.string.enter_first_comment, groupChatViewModel.myUsername()));
        FontUtil.setTextViewFont(textView);
        final AlertDialog dialog = new ThemedAlertDialog.Builder(context).
                setView(view).
                setCancelable(false).
                setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final String text = textView.getText().toString();
                        if (!TextUtils.isEmpty(text)) {
                            UserPreferences.getInstance().setShownSendFirstMessageDialog(true);
                            final FriendlyMessage friendlyMessage = new FriendlyMessage(text,
                                    groupChatViewModel.myUsername(),
                                    groupChatViewModel.myUserid(),
                                    groupChatViewModel.myDpid(), null, false, false, null, System.currentTimeMillis());
                            sendText(friendlyMessage);
                            FirebaseAnalytics.getInstance(GroupChatActivity.this).logEvent(Events
                                    .WELCOME_MESSAGE_SENT, null);
                        }
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (!UserPreferences.getInstance().hasShownSendFirstMessageDialog()) {
                    showFirstMessageDialog(GroupChatActivity.this);
                }
            }
        }).show();
    }

    private EditText createEditTextWithContentMimeTypes(String[] contentMimeTypes) {
        final CharSequence hintText;
        final String[] mimeTypes;  // our own copy of contentMimeTypes.
        if (contentMimeTypes == null || contentMimeTypes.length == 0) {
            hintText = "MIME: []";
            mimeTypes = new String[0];
        } else {
            hintText = "MIME: " + Arrays.toString(contentMimeTypes);
            mimeTypes = Arrays.copyOf(contentMimeTypes, contentMimeTypes.length);
        }
        AppCompatEditText editText = new AppCompatEditText(this) {
            @Override
            public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
                final InputConnection ic = super.onCreateInputConnection(editorInfo);
                EditorInfoCompat.setContentMimeTypes(editorInfo, mimeTypes);
                final InputConnectionCompat.OnCommitContentListener callback =
                        new InputConnectionCompat.OnCommitContentListener() {
                            @Override
                            public boolean onCommitContent(InputContentInfoCompat inputContentInfo,
                                                           int flags, Bundle opts) {
                                return GroupChatActivity.this.onCommitContent(
                                        inputContentInfo, flags, opts, mimeTypes);
                            }
                        };
                return InputConnectionCompat.createWrapper(ic, editorInfo, callback);
            }
        };
        editText.setHint(R.string.message_hint);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(params);
        return editText;
    }

    private boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags,
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
        //MLog.d(TAG, "linkUri: " + linkUri.toString() + ": " + inputContentInfo.getDescription().toString(), " : ",
        // inputContentInfo);
        if (inputContentInfo != null && inputContentInfo.getDescription() != null) {
            if (inputContentInfo.getDescription().toString().contains("image/gif")) {
                final FriendlyMessage friendlyMessage = new FriendlyMessage("",
                        groupChatViewModel.myUsername(),
                        groupChatViewModel.myUserid(),
                        groupChatViewModel.myDpid(),
                        linkUri.toString(), false, false, null, System.currentTimeMillis());
                friendlyMessage.setMessageType(FriendlyMessage.MESSAGE_TYPE_NORMAL);
                messagesAdapter.sendFriendlyMessage(friendlyMessage);
            }
        }
        return true;
    }

    @Override
    public void onReceiveAd(AdDownloaderInterface adDownloaderInterface, ReceivedBannerInterface receivedBanner) throws AdReceiveFailed {
        if (receivedBanner.getErrorCode() != ErrorCode.NO_ERROR) {
            binding.setVisibleAd(false);
            adsHelper.loadAd(this);
        } else {
            binding.setVisibleAd(true);
        }
    }

    @Override
    public GroupChatViewModel getViewModel() {
        return (groupChatViewModel = ViewModelProviders.of(this, viewModelFactory).get(GroupChatViewModel.class));
    }

    @Override
    public int getBindingVariable() {
        return BR.viewModel;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    /**
     * Implementation of {@link AbstractChatNavigator}
     */
    @Override
    public void showSignIn() {
        startActivity(new Intent(this, SignInActivity.class));
        finish();
    }

    /**
     * Implementation of {@link AbstractChatNavigator}
     */
    @Override
    public void setMaxMessageLength(int maxMessageLength) {
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxMessageLength)});
    }
}
