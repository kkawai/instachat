package com.instachat.android.app.activity.group;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.AbstractChatActivity;
import com.instachat.android.app.activity.AttachPhotoOptionsDialogHelper;
import com.instachat.android.app.activity.UsersInGroupListener;
import com.instachat.android.app.adapter.FriendlyMessageListener;
import com.instachat.android.app.adapter.GroupChatUsersRecyclerAdapter;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.blocks.BlocksFragment;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.databinding.ActivityMainBinding;
import com.instachat.android.util.FontUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.view.ThemedAlertDialog;
import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.ErrorCode;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.exception.AdReceiveFailed;

import javax.inject.Inject;

public class GroupChatActivity extends AbstractChatActivity<ActivityMainBinding, GroupChatViewModel>
        implements GoogleApiClient.OnConnectionFailedListener,
        UploadListener, FriendlyMessageListener,
        AttachPhotoOptionsDialogHelper.PhotoOptionsListener,
        AdListenerInterface, GroupChatNavigator {

    private static final String TAG = "GroupChatActivity";

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private GroupChatUsersRecyclerAdapter mGroupChatUsersRecyclerAdapter;

    private ActivityMainBinding binding;
    private GroupChatViewModel groupChatViewModel;
    private TextView rightHeaderGroupName;

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

        initFirebaseAdapter(binding.fragmentContent, binding.messageRecyclerView,this, linearLayoutManager);
        binding.messageRecyclerView.setLayoutManager(linearLayoutManager);
        binding.messageRecyclerView.setAdapter(messagesAdapter);

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
        if (UserPreferences.getInstance().isLoggedIn()) {
            gcmHelper.onResume(this);
            showFirstMessageDialog(GroupChatActivity.this);
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
        getViewModel().setDatabaseRoot(databaseRef);
        getViewModel().setGroupId(groupId);
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
                showManageBlocks();
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
                showAppInviteActivity();
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
                showPendingRequests();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        MLog.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void setupLeftDrawerContent() {
        super.setupLeftDrawerContent();
        UsersInGroupListener usersInGroupListener = new UsersInGroupListener() {
            @Override
            public void onNumUsersUpdated(long groupId, String groupName, int numUsers) {
                if (getSupportActionBar() != null && groupId == groupChatViewModel.getGroupId())
                    getSupportActionBar().setTitle(groupName + getCount(numUsers));
            }
        };
        chatsRecyclerViewAdapter.setUsersInGroupListener(usersInGroupListener);
    }

    @Override
    public void setupRightDrawerContent() {

        RecyclerView rightDrawerRecyclerView = (RecyclerView) getLayoutInflater().inflate(R.layout.right_drawer_layout, binding.rightNavView, false);
        final View rightHeaderView = getLayoutInflater().inflate(R.layout.right_nav_header, binding.rightNavView, false);
        rightHeaderGroupName = rightHeaderView.findViewById(R.id.groupname);

        binding.rightNavView.addHeaderView(rightHeaderView);
        binding.rightNavView.addHeaderView(rightDrawerRecyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rightDrawerRecyclerView.setLayoutManager(linearLayoutManager);

        mGroupChatUsersRecyclerAdapter = new GroupChatUsersRecyclerAdapter(this, this, this, groupChatViewModel.getGroupId());
        rightDrawerRecyclerView.setAdapter(mGroupChatUsersRecyclerAdapter);
        mGroupChatUsersRecyclerAdapter.populateData();

        groupChatViewModel.fetchGroupName();
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

    @Override
    public void showUserTyping(String username) {
        if (isActivityDestroyed()) {
            return;
        }
        binding.usernameTyping.setText(username);
        showTypingDots();
    }

    @Override
    public void showSubtitle() {
        getSupportActionBar().setSubtitle(R.string.app_name);
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
        if (!groupChatViewModel.isPrivateChat()) {
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
            getViewModel().isAdReady.set(true);
            adsHelper.loadAd(this);
        } else {
            getViewModel().isAdReady.set(false);
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

    @Override
    public void removeUserFromAllGroups(int userid, long exceptionGroupId) {
        if (chatsRecyclerViewAdapter != null)
            chatsRecyclerViewAdapter.removeUserFromAllGroups(userid, exceptionGroupId);
    }

    public void showSendOptions(FriendlyMessage friendlyMessage) {
        showSendOptions(friendlyMessage, binding.sendButton);
    }

    @Override
    public void onGroupChatClicked(GroupChatSummary groupChatSummary) {
        groupChatViewModel.removeUserPresenceFromGroup();
        super.onGroupChatClicked(groupChatSummary);
    }

    @Override
    public void showManageBlocks() {
        if (isLeftDrawerOpen()) {
            closeLeftDrawer();
        }
        Fragment fragment = new BlocksFragment();
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim
                .slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment,
                BlocksFragment.TAG).addToBackStack(null).commit();
    }

    @Override
    public void showGroupName(String groupName) {
        rightHeaderGroupName.setText(groupName);
    }
}
