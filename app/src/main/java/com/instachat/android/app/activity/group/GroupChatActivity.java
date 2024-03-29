package com.instachat.android.app.activity.group;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.TheApp;
import com.instachat.android.app.activity.AbstractChatActivity;
import com.instachat.android.app.activity.AbstractChatViewModel;
import com.instachat.android.app.activity.AttachPhotoOptionsDialogHelper;
import com.instachat.android.app.activity.LauncherActivity;
import com.instachat.android.app.activity.PhotoUploadHelper;
import com.instachat.android.app.activity.UsersInGroupListener;
import com.instachat.android.app.adapter.FriendlyMessageListener;
import com.instachat.android.app.adapter.GroupChatUsersRecyclerAdapter;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.bans.BannedUsersFragment;
import com.instachat.android.app.blocks.BlocksFragment;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.databinding.ActivityMainBinding;
import com.instachat.android.databinding.DialogInputCommentBinding;
import com.instachat.android.databinding.RightDrawerLayoutBinding;
import com.instachat.android.util.AdminUtil;
import com.instachat.android.util.DeviceUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.view.ThemedAlertDialog;
import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.ErrorCode;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.exception.AdReceiveFailed;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.functions.Action;

public class GroupChatActivity extends AbstractChatActivity<ActivityMainBinding, GroupChatViewModel> implements
        UploadListener, FriendlyMessageListener,
        AttachPhotoOptionsDialogHelper.PhotoOptionsListener,
        AdListenerInterface, GroupChatNavigator {

    private static final String TAG = "GroupChatActivity";

    private RightDrawerLayoutBinding rightDrawerLayoutBinding;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

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
        setupDrawers(binding.navView);
        setupToolbar();

        gcmHelper.onCreate(this);

        initFirebaseAdapter(binding.fragmentContent, binding.messageRecyclerView, this, linearLayoutManager);
        binding.messageRecyclerView.setLayoutManager(linearLayoutManager);
        binding.messageRecyclerView.setAdapter(messagesAdapter);

        adsHelper.loadBannerAd(this, firebaseRemoteConfig);

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
        if (TheApp.isSavedDeviceId == false) {
            TheApp.isSavedDeviceId = true;
            FirebaseDatabase.getInstance()
                    .getReference(Constants.USER_INFO_REF(UserPreferences.getInstance().getUserId()))
                    .updateChildren(DeviceUtil.getAndroidIdMap(this));
        }
        groupChatViewModel.checkTermsOfService();

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
            NetworkApi.init();
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
        if (UserPreferences.getInstance().isLoggedIn()) {
            groupChatViewModel.removeUserPresenceFromGroup();
        }
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
    public void onFriendlyMessageSuccess(final @NonNull FriendlyMessage friendlyMessage) {
        try {
            if (isActivityDestroyed())
                return;
            MLog.d(TAG, "C kevin scroll: " + (messagesAdapter.getItemCount() - 1));
            binding.messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
            presenceHelper.updateLastActiveTimestamp();
        } catch (final Exception e) {
            MLog.e(TAG, "", e);
        }
        getViewModel().onFriendlyMessageSuccess(friendlyMessage);
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
                menu.findItem(R.id.menu_clear_room).setTitle("Clear Posts: " + getViewModel().getRoomCommentCount());
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
            case R.id.menu_delete_account:
                deleteAccount();
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

    @Override
    public void setupRightDrawerContent() {

        rightDrawerLayoutBinding = RightDrawerLayoutBinding.inflate(getLayoutInflater(), binding.rightNavView, false);

        binding.rightNavView.addView(rightDrawerLayoutBinding.getRoot());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
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
                signoutAndFinish();
            }
        });
    }

    private void deleteAccount() {

        deleteAccountDialogHelper.showDeleteAccountDialog(this, new DeleteAccountDialogHelper.DeleteAccountListener() {
            @Override
            public void onConfirmDeleteAccount() {
                getViewModel().deleteAccount(UserPreferences.getInstance().getUserId());
            }
        });
    }

    @Override
    public void showAccountDeleted() {
        UserPreferences.getInstance().removeLastSignIn();
        signoutAndFinish();
    }

    private void signoutAndFinish() {
        AbstractChatViewModel.isCaptchaVerified = false;
        TheApp.isSavedDeviceId = false;
        groupChatViewModel.removeUserPresenceFromGroup();
        gcmHelper.unregister(UserPreferences.getInstance().getUserId() + "");
        UserPreferences.getInstance().signOut();
        firebaseAuth.signOut();
        startActivity(new Intent(GroupChatActivity.this, LauncherActivity.class));
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
                    showFirstMessageDialog(GroupChatActivity.this);
                }
            }
        }).show();
    }

    @Override
    public void onReceiveAd(AdDownloaderInterface adDownloaderInterface, ReceivedBannerInterface receivedBanner) throws AdReceiveFailed {
        if (receivedBanner.getErrorCode() != ErrorCode.NO_ERROR) {
            adsHelper.loadBannerAd(this, firebaseRemoteConfig);
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
        if (chatSummariesRecyclerAdapter != null)
            chatSummariesRecyclerAdapter.removeUserFromAllGroups(userid, exceptionGroupId);
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

    //Don't be fooled by 'enterChat'
    //this is really signing out
    @Override
    public void enterChat() {
        signoutAndFinish();
    }

    @Override
    public void showTermsOfService() {

        /**
         * MAKE SURE THEY READ THE TERMS FOR A MINIMUM OF SOME SECONDS!
         * AFTER SOME SECONDS, RE-SHOW THE TERMS DIALOG WITH THE AGREE/DISMISS BUTTON!
         */
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.terms_of_use_title).setMessage(R.string.terms_of_use)
                .setCancelable(false)
                .create();
        try {
            alertDialog.show();
        } catch (Throwable t) {}

        add(Observable.timer(3500, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        try {
                            alertDialog.dismiss();
                        } catch (Throwable t) {
                        }
                        try {
                            new AlertDialog.Builder(GroupChatActivity.this)
                                    .setTitle(R.string.terms_of_use_title).setMessage(R.string.terms_of_use)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                dialog.dismiss();
                                            }catch (Throwable t){}
                                        }
                                    })
                                    .show();
                        } catch (Throwable t) {
                        }
                    }
                }).subscribe());
    }

    @Override
    protected PhotoUploadHelper.PhotoType getRoomPhotoType() {
        return PhotoUploadHelper.PhotoType.groupChatRoomPhoto;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.PRIVATE_CHAT_REQUEST_CODE) {
            adsHelper.loadBannerAd(this, firebaseRemoteConfig);
        }
    }
}
