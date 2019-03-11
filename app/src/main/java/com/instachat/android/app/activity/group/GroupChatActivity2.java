package com.instachat.android.app.activity.group;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.AbstractChatActivity2;
import com.instachat.android.app.activity.UsersInGroupListener;
import com.instachat.android.app.bans.BannedUsersFragment;
import com.instachat.android.app.blocks.BlocksFragment;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.databinding.ActivityMain2Binding;
import com.instachat.android.databinding.RightDrawerLayoutBinding;
import com.instachat.android.util.AdminUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;

import javax.inject.Inject;

import cn.pedant.SweetAlert.SweetAlertDialog;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;

public class GroupChatActivity2 extends AbstractChatActivity2<ActivityMain2Binding, GroupChatViewModel2>
        implements GoogleApiClient.OnConnectionFailedListener, GroupChatNavigator, HasSupportFragmentInjector {

    @Inject
    DispatchingAndroidInjector<Fragment> fragmentDispatchingAndroidInjector;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private static final String TAG = "GroupChatActivity";

    //private GroupChatUsersRecyclerAdapter mGroupChatUsersRecyclerAdapter;

    private ActivityMain2Binding binding;
    private GroupChatViewModel2 groupChatViewModel2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupChatViewModel2.setNavigator(this);
        initDatabaseRef();
        binding = getViewDataBinding();
        setupDrawers(binding.navView);
        //setupToolbar();
        initPhotoHelper(savedInstanceState);

        gcmHelper.onCreate(this);

        //initFirebaseAdapter(binding.fragmentContent, binding.messageRecyclerView,this, linearLayoutManager);
        //binding.messageRecyclerView.setLayoutManager(linearLayoutManager);
        //binding.messageRecyclerView.setAdapter(messagesAdapter);

        //adsHelper.loadAd(this, firebaseRemoteConfig);

        groupChatViewModel2.fetchConfig(firebaseRemoteConfig);

        //if (getIntent() != null && getIntent().hasExtra(Constants.KEY_GROUP_NAME)) {
        //    getSupportActionBar().setTitle(getIntent().getStringExtra(Constants.KEY_GROUP_NAME));
        //}

        //initExternalSendIntentConsumer(binding.drawerLayout);
        //checkIncomingShareIntent();
        //groupChatViewModel2.listenForTyping();
        //cancelNotificationsDueToEntry();
        //groupChatViewModel2.smallProgressCheck();
        showGroupChatFragment();
    }

    private void showGroupChatFragment() {
        Fragment fragment = new GroupChatFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_content, fragment,
                GroupChatFragment.TAG).addToBackStack(null).commit();
    }

    private String getCount(int count) {
        if (count > 0)
            return " (" + count + ")";
        return "";
    }

    @Override
    public void onDestroy() {
        groupChatViewModel2.cleanup();
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
                menu.findItem(R.id.menu_clear_room).setTitle("Clear comments: " + getViewModel().getRoomCommentCount());
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
                if (getSupportActionBar() != null && groupId == groupChatViewModel2.getGroupId())
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

        groupChatViewModel2.fetchGroupName();
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
        groupChatViewModel2.removeUserPresenceFromGroup();
        gcmHelper.unregister(UserPreferences.getInstance().getUserId() + "");
        UserPreferences.getInstance().clearUser();
        startActivity(new Intent(GroupChatActivity2.this, SignInActivity.class));
        finish();
    }

    @Override
    public GroupChatViewModel2 getViewModel() {
        return (groupChatViewModel2 = ViewModelProviders.of(this, viewModelFactory).get(GroupChatViewModel2.class));
    }

    @Override
    public int getBindingVariable() {
        return BR.viewModel;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main2;
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
        groupChatViewModel2.removeUserPresenceFromGroup();
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

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentDispatchingAndroidInjector;
    }
}
