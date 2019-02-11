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
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.AbstractChatActivity;
import com.instachat.android.app.activity.AttachPhotoOptionsDialogHelper;
import com.instachat.android.app.activity.UsersInGroupListener;
import com.instachat.android.app.adapter.FriendlyMessageListener;
import com.instachat.android.app.adapter.GroupChatUsersRecyclerAdapter;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.bans.BannedUsersFragment;
import com.instachat.android.app.blocks.BlocksFragment;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.databinding.ActivityMain2Binding;
import com.instachat.android.databinding.ActivityMainBinding;
import com.instachat.android.databinding.DialogInputCommentBinding;
import com.instachat.android.databinding.RightDrawerLayoutBinding;
import com.instachat.android.util.AdminUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.view.ThemedAlertDialog;
import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.ErrorCode;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.exception.AdReceiveFailed;

import javax.inject.Inject;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class GroupChatActivity2 extends AbstractChatActivity<ActivityMain2Binding, GroupChatViewModel>
        implements GoogleApiClient.OnConnectionFailedListener, GroupChatNavigator {

    private static final String TAG = "GroupChatActivity";

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private GroupChatUsersRecyclerAdapter mGroupChatUsersRecyclerAdapter;

    private ActivityMain2Binding binding;
    private GroupChatViewModel groupChatViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupChatViewModel.setNavigator(this);
        initDatabaseRef();
        binding = getViewDataBinding();
        initPhotoHelper(savedInstanceState);
        setupDrawers(binding.navView);
        setupToolbar();

        gcmHelper.onCreate(this);

        adsHelper.loadAd(this, firebaseRemoteConfig);

        groupChatViewModel.fetchConfig(firebaseRemoteConfig);

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

    protected void onHomeClicked() {
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_chat_options_menu, menu);
        return true;
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
        if (!AdminUtil.isMeAdmin()) {
            if (menu.findItem(R.id.menu_clear_room) != null)
                menu.removeItem(R.id.menu_clear_room);
        } else {
            if (menu.findItem(R.id.menu_clear_room) != null) {
                menu.findItem(R.id.menu_clear_room).setTitle("Clear comments: "+getViewModel().getRoomCommentCount());
            }
        }
        return super.onMenuOpened(featureId, menu);
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
            case R.id.menu_banned_users:
                showBannedUsers();
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
            case R.id.menu_pending_requests:
                showPendingRequests();
                return true;
            case R.id.menu_clear_room:
                getViewModel().clearRoomComments();
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
    public void listenForUsersInGroup() {
        UsersInGroupListener usersInGroupListener = new UsersInGroupListener() {
            @Override
            public void onNumUsersUpdated(long groupId, String groupName, int numUsers) {
                if (getSupportActionBar() != null && groupId == groupChatViewModel.getGroupId())
                    getSupportActionBar().setTitle(groupName + getCount(numUsers));
            }
        };
        chatSummariesRecyclerAdapter.setUsersInGroupListener(usersInGroupListener);
    }

    private RightDrawerLayoutBinding rightDrawerLayoutBinding;
    @Override
    public void setupRightDrawerContent() {

        rightDrawerLayoutBinding = RightDrawerLayoutBinding.inflate(getLayoutInflater(), binding.rightNavView, false);

        binding.rightNavView.addView(rightDrawerLayoutBinding.getRoot());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rightDrawerLayoutBinding.drawerRecyclerView.setLayoutManager(linearLayoutManager);

        mGroupChatUsersRecyclerAdapter = new GroupChatUsersRecyclerAdapter(this, groupChatViewModel.getGroupId());
        rightDrawerLayoutBinding.drawerRecyclerView.setAdapter(mGroupChatUsersRecyclerAdapter);
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
        getViewModel().usernameTyping.set(username);
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
                _signout();
            }
        });
    }

    private void _signout() {
        firebaseAuth.signOut();
        groupChatViewModel.removeUserPresenceFromGroup();
        gcmHelper.unregister(UserPreferences.getInstance().getUserId() + "");
        UserPreferences.getInstance().clearUser();
        startActivity(new Intent(GroupChatActivity2.this, SignInActivity.class));
        finish();
    }

    private void showFirstMessageDialog(@NonNull final Context context) {
        if (UserPreferences.getInstance().hasShownSendFirstMessageDialog()) {
            return;
        }
        final DialogInputCommentBinding binding = DialogInputCommentBinding.inflate(getLayoutInflater(), null, false);
        binding.setName(getViewModel().myUsername());
        new ThemedAlertDialog.Builder(context).
                setView(binding.getRoot()).
                setCancelable(false).
                setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final String text = binding.inputText.getText().toString();
                        if (!TextUtils.isEmpty(text)) {
                            UserPreferences.getInstance().setShownSendFirstMessageDialog(true);
                            final FriendlyMessage friendlyMessage = new FriendlyMessage(text,
                                    groupChatViewModel.myUsername(),
                                    groupChatViewModel.myUserid(),
                                    groupChatViewModel.myDpid(), null, false, false, null, System.currentTimeMillis());
                            getViewModel().sendText(friendlyMessage);
                            firebaseAnalytics.logEvent(Events
                                    .WELCOME_MESSAGE_SENT, null);
                        }
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (!UserPreferences.getInstance().hasShownSendFirstMessageDialog()) {
                    showFirstMessageDialog(GroupChatActivity2.this);
                }
            }
        }).show();
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
        if (chatSummariesRecyclerAdapter != null)
            chatSummariesRecyclerAdapter.removeUserFromAllGroups(userid, exceptionGroupId);
    }

    public void showSendOptions(FriendlyMessage friendlyMessage) {
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
    public void toggleGroupChatAppBar() {
        if (isRightDrawerOpen()) {
            closeRightDrawer();
            return;
        } else if (isLeftDrawerOpen()) {
            closeLeftDrawer();
        }
        openRightDrawer();
    }

    @Override
    public void showBannedUsers() {
        if (isLeftDrawerOpen()) {
            closeLeftDrawer();
        }
        Fragment fragment = new BannedUsersFragment();
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim
                .slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment,
                BannedUsersFragment.TAG).addToBackStack(null).commit();
    }

    @Override
    public void showVerificationEmailSent() {
        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE).setContentText(getString(R.string.email_verification_sent));
        dialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                _signout();
            }
        });
    }

    @Override
    public void enterChat() {
        _signout();
    }

    @Override
    public void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
