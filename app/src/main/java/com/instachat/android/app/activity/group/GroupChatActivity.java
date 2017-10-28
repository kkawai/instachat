package com.instachat.android.app.activity.group;

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
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.AbstractChatActivity;
import com.instachat.android.app.activity.AbstractChatNavigator;
import com.instachat.android.app.activity.AttachPhotoOptionsDialogHelper;
import com.instachat.android.app.activity.LeftDrawerEventListener;
import com.instachat.android.app.activity.LeftDrawerHelper;
import com.instachat.android.app.activity.PhotoUploadHelper;
import com.instachat.android.app.activity.UsersInGroupListener;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.app.adapter.ChatsItemClickedListener;
import com.instachat.android.app.adapter.FriendlyMessageListener;
import com.instachat.android.app.adapter.GroupChatUsersRecyclerAdapter;
import com.instachat.android.app.adapter.MessageTextClickedListener;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.blocks.BlocksFragment;
import com.instachat.android.app.fullscreen.FriendlyMessageContainer;
import com.instachat.android.app.fullscreen.FullScreenTextFragment;
import com.instachat.android.app.likes.UserLikedUserFragment;
import com.instachat.android.app.likes.UserLikedUserListener;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.app.requests.RequestsFragment;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.databinding.ActivityMainBinding;
import com.instachat.android.font.FontUtil;
import com.instachat.android.messaging.NotificationHelper;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.view.ThemedAlertDialog;
import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.ErrorCode;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.exception.AdReceiveFailed;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import pub.devrel.easypermissions.EasyPermissions;

public class GroupChatActivity extends AbstractChatActivity<ActivityMainBinding, GroupChatViewModel> implements GoogleApiClient.OnConnectionFailedListener,
        FriendlyMessageContainer, EasyPermissions.PermissionCallbacks, UploadListener, UserClickedListener,
        ChatsItemClickedListener, FriendlyMessageListener, AttachPhotoOptionsDialogHelper.PhotoOptionsListener,
        AdListenerInterface, GroupChatNavigator {

    private static final String TAG = "GroupChatActivity";

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    LinearLayoutManager linearLayoutManager;

    private GroupChatUsersRecyclerAdapter mGroupChatUsersRecyclerAdapter;

    private ActivityMainBinding binding;
    private GroupChatViewModel groupChatViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupChatViewModel.setNavigator(this);
        initDatabaseRef();
        binding = getViewDataBinding();

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

        initMessageEditText(binding.sendButton, binding.messageEditTextParent);
        groupChatViewModel.fetchConfig(firebaseRemoteConfig);
        initButtons(binding.sendButton, binding.attachButton);

        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_GROUP_NAME)) {
            getSupportActionBar().setTitle(getIntent().getStringExtra(Constants.KEY_GROUP_NAME));
        }

        initExternalSendIntentConsumer(binding.drawerLayout);
        checkIncomingShareIntent();
        groupChatViewModel.listenForTyping();
        cancelNotificationsDueToEntry();
        groupChatViewModel.smallProgressCheck();
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
            groupChatViewModel.addUserPresenceToGroup();
        } else {
            finish();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        groupChatViewModel.removeUserPresenceFromGroup();
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

    /**
     * {@link FriendlyMessageListener}
     *
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
     *
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
                showFullScreenTextView(-1);
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
        drawerLayout = binding.drawerLayout;
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

        RecyclerView rightDrawerRecyclerView = (RecyclerView) getLayoutInflater().inflate(R.layout.right_drawer_layout, binding.rightNavView, false);
        final View rightHeaderView = getLayoutInflater().inflate(R.layout.right_nav_header, binding.rightNavView, false);

        mRightRef = firebaseDatabase.getReference(Constants.GROUP_CHAT_ROOMS).child
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
                ((TextView) rightHeaderView.findViewById(R.id.groupname)).setText(groupChatSummary.getName());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mRightRef.addValueEventListener(mRightListener);

        binding.rightNavView.addHeaderView(rightHeaderView);
        binding.rightNavView.addHeaderView(rightDrawerRecyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rightDrawerRecyclerView.setLayoutManager(linearLayoutManager);

        mGroupChatUsersRecyclerAdapter = new GroupChatUsersRecyclerAdapter(this, this, this, groupChatViewModel.getGroupId());
        rightDrawerRecyclerView.setAdapter(mGroupChatUsersRecyclerAdapter);
        mGroupChatUsersRecyclerAdapter.populateData();
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

    private void showFullScreenTextView(final int startingPos) {
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
                showFullScreenTextView(position);
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
            add(Observable.timer(175, TimeUnit.MILLISECONDS)
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            showPhotoOptionsDialog();
                        }
                    }).subscribe());

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
        groupChatViewModel.removeUserPresenceFromGroup();
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

    @Override
    public void showSubtitle() {
        getSupportActionBar().setSubtitle(R.string.app_name);
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
                groupChatViewModel.removeUserPresenceFromGroup();
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

    @Override
    public void removeUserFromAllGroups(int userid, long exceptionGroupId) {
        if (chatsRecyclerViewAdapter != null)
            chatsRecyclerViewAdapter.removeUserFromAllGroups(userid, exceptionGroupId);
    }

    public void showSendOptions(FriendlyMessage friendlyMessage) {
        showSendOptions(friendlyMessage, binding.sendButton);
    }
}
