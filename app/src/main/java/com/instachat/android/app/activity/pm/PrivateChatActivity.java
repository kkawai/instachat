package com.instachat.android.app.activity.pm;

import android.app.Activity;
import android.app.NotificationManager;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.AbstractChatActivity;
import com.instachat.android.app.activity.AttachPhotoOptionsDialogHelper;
import com.instachat.android.app.activity.PhotoUploadHelper;
import com.instachat.android.app.adapter.FriendlyMessageListener;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.blocks.BlockUserDialogHelper;
import com.instachat.android.app.blocks.BlockedUserListener;
import com.instachat.android.app.blocks.BlocksFragment;
import com.instachat.android.app.blocks.ReportUserDialogHelper;
import com.instachat.android.app.fullscreen.FriendlyMessageContainer;
import com.instachat.android.app.likes.UserLikedUserFragment;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.data.model.User;
import com.instachat.android.databinding.ActivityPrivateChatBinding;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.Bindings;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.TimeUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.view.FlingGestureListener;
import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.ErrorCode;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.exception.AdReceiveFailed;
import com.tooltip.Tooltip;

import org.json.JSONException;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.lifecycle.ViewModelProviders;
import io.reactivex.Observable;
import io.reactivex.functions.Action;

/**
 * Created by kevin on 9/16/2016.
 * The difference between Private and Group Chat:
 * Title in toolbar
 * Private has user photo
 * Different database references
 * Private messages result in partner devices receiving notifications
 * Private needs to fetch partner user from network api
 */
public class PrivateChatActivity extends AbstractChatActivity<ActivityPrivateChatBinding, PrivateChatViewModel> implements
        FriendlyMessageContainer, UploadListener, UserClickedListener,
        FriendlyMessageListener, AttachPhotoOptionsDialogHelper.PhotoOptionsListener,
        AdListenerInterface, PrivateChatNavigator {

    private static final String TAG = "PrivateChatActivity";
    private ImageView mProfilePic;
    private AppBarLayout mAppBarLayout;
    private GestureDetectorCompat mGestureDetector;
    private static int sUserid;
    private static String sUsername, sProfilePicUrl;
    private ActivityPrivateChatBinding binding;
    private PrivateChatViewModel privateChatViewModel;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_App);
        super.onCreate(savedInstanceState);
        privateChatViewModel.setNavigator(this);
        initDatabaseRef();
        binding = getViewDataBinding();
        initPhotoHelper(savedInstanceState);
        setupDrawers(binding.navView);
        setupToolbar();

        gcmHelper.onCreate(this);

        initFirebaseAdapter(binding.fragmentContent, binding.messageRecyclerView,this, linearLayoutManager);
        messagesAdapter.setIsPrivateChat(true);
        binding.messageRecyclerView.setLayoutManager(linearLayoutManager);
        binding.messageRecyclerView.setAdapter(messagesAdapter);

        adsHelper.loadBannerAd(this, firebaseRemoteConfig);

        initMessageEditText(binding.sendButton, binding.messageEditTextParent);
        privateChatViewModel.fetchConfig(firebaseRemoteConfig);
        initButtons(binding.sendButton, binding.attachButton);

        initExternalSendIntentConsumer(binding.drawerLayout);
        checkIncomingShareIntent();
        privateChatViewModel.smallProgressCheck();
        onNewIntent(getIntent());
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);

        add(Observable.timer(2000, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        adsHelper.loadRewardedAd(PrivateChatActivity.this);
                    }
                }).subscribe());

    }

    //@Override
    protected void onHomeClicked() {
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mProfilePic = (ImageView) findViewById(R.id.profile_pic);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        getSupportActionBar().setTitle("");
        sUserid = getIntent().getIntExtra(Constants.KEY_USERID, 0);
        sUsername = getIntent().getStringExtra(Constants.KEY_USERNAME);
        sProfilePicUrl = getIntent().getStringExtra(Constants.KEY_PROFILE_PIC_URL);
        if (Build.VERSION.SDK_INT >= 21) {
            mProfilePic.setTransitionName("profilePic" + sUserid);
        }
        MLog.d(TAG, "before grab from server, intent data: sUserid: ", sUserid, " sUsernane: ", sUsername, " " +
                "sProfilePicUrl: ", sProfilePicUrl);
        setPartnerInfo(getIntent());
        privateChatViewModel.fetchUser(sUserid);
        final NotificationManager notificationManager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        notificationManager.cancel(sUserid);
        MLog.d(TAG, "Cancelled notification " + sUserid);
        privateChatViewModel.clearPrivateUnreadMessages(sUserid);

        final View customTitlePairInParallax = findViewById(R.id.customTitlePairInParallax);
        final View customTitlePairInToolbar = findViewById(R.id.customTitlePairInToolbar);

        final ImageView toolbarProfileImageView = (ImageView) findViewById(R.id.topCornerUserThumb);
        final TextView bio = (TextView) findViewById(R.id.bio);
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                float alpha = 1 - (float) (Math.abs(verticalOffset) + getToolbarHeight()) / appBarLayout.getHeight();
                if (verticalOffset == 0) {
                    alpha = 1;
                }
                bio.setAlpha(alpha);
                mProfilePic.setAlpha(alpha);
                customTitlePairInParallax.setAlpha(alpha);
                toolbarProfileImageView.setAlpha(1 - alpha);
                customTitlePairInToolbar.setAlpha(1 - alpha);
                binding.messageRecyclerViewParent.setAlpha(1 - (alpha / 2f));

                MLog.d(TAG, "mAppBarLayout.height: " + appBarLayout.getHeight(), " verticalOffset ", verticalOffset,
                        " toolbarHeight ", getToolbarHeight(), " alpha ", alpha);
                if (verticalOffset == 0) {
                    mIsAppBarExpanded = true;
                } else if (Math.abs(verticalOffset) + getToolbarHeight() == appBarLayout.getHeight()) {
                    mIsAppBarExpanded = false;
                    checkIfSeenToolbarProfileTooltip(mProfilePic);
                }
            }
        });

        mGestureDetector = initializeGestureDetector();
        getToolbar().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mGestureDetector.onTouchEvent(motionEvent);
            }
        });
        presenceHelper.updateLastActiveTimestamp();
        privateChatViewModel.listenForUpdatedLikeCount(sUserid);
    }

    private void checkIfSeenToolbarProfileTooltip(View anchor) {
        if (UserPreferences.getInstance().hasShownToolbarProfileTooltip())
            return;
        UserPreferences.getInstance().setShownToolbarProfileTooltip(true);
        final Tooltip tooltip = new Tooltip.Builder(anchor, R.style.drawer_tooltip_non_cancellable).setText(getString
                (R.string.toolbar_user_profile_tooltip)).show();
        add(Observable.timer(firebaseRemoteConfig.getLong(Constants.KEY_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME), TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        if (tooltip.isShowing())
                            tooltip.dismiss();
                    }
                }).subscribe());
    }

    private boolean mIsAppBarExpanded = true; //initially it's expanded

    @Override
    public void onDestroy() {
        super.onDestroy();
        sUserid = 0;
        sUsername = null;
        sProfilePicUrl = null;
    }

    //@Override
    protected void initDatabaseRef() {
        privateChatViewModel.setDatabaseRoot(Constants.PRIVATE_CHAT_REF(getIntent().getIntExtra(Constants.KEY_USERID, 0)));
    }

    @Override
    public void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage) {

        try {
            if (isActivityDestroyed())
                return;
            MLog.d(TAG, "C kevin scroll: " + (messagesAdapter.getItemCount() - 1));
            binding.messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
            presenceHelper.updateLastActiveTimestamp();
        } catch (final Exception e) {
            MLog.e(TAG, "", e);
        }

        try {
            networkApi.gcmsend(sUserid, Constants.GcmMessageType.msg, friendlyMessage.toLightweightJSONObject());
        } catch (JSONException e) {
            MLog.e(TAG, "", e);
        }
        if (mIsAppBarExpanded) {
            mAppBarLayout.setExpanded(false, true);
        }
        privateChatViewModel.initializePrivateChatSummary(sUsername, sUserid, sProfilePicUrl);
        privateChatViewModel.onFriendlyMessageSuccess(friendlyMessage, sUsername);
    }

    public static void startPrivateChatActivity(Activity activity, int userid, String username, String profilePicUrl,
                                                final boolean autoAddUser, final View transitionImageView,
                                                Uri sharePhotoUri, String shareMessage) {
        Intent intent = new Intent(activity, PrivateChatActivity.class);
        intent.putExtra(Constants.KEY_USERID, userid);
        intent.putExtra(Constants.KEY_USERNAME, username);
        intent.putExtra(Constants.KEY_PROFILE_PIC_URL, profilePicUrl);
        intent.putExtra(Constants.KEY_AUTO_ADD_PERSON, autoAddUser);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (sharePhotoUri != null)
            intent.putExtra(Constants.KEY_SHARE_PHOTO_URI, sharePhotoUri);
        if (shareMessage != null)
            intent.putExtra(Constants.KEY_SHARE_MESSAGE, shareMessage);

        if (transitionImageView != null) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                    transitionImageView, "profilePic" + userid);
            activity.startActivity(intent, options.toBundle());
            activity.startActivityForResult(intent, Constants.PRIVATE_CHAT_REQUEST_CODE, options.toBundle());
        } else {
            activity.startActivityForResult(intent, Constants.PRIVATE_CHAT_REQUEST_CODE);
        }
    }

    public static int getActiveUserid() {
        return sUserid;
    }

    private void setPartnerInfo(Intent intent) {

        getViewModel().partnerUsername.set(sUsername);

        if (intent.getBooleanExtra(Constants.KEY_AUTO_ADD_PERSON, false)) {
            //add this person to my left drawer and remove them from pending requests
            privateChatViewModel.addUser(sUsername, sProfilePicUrl, sUserid);
        }

    }

    @Override
    public void showUserProfile(final User toUser) {
        if (toUser == null || isActivityDestroyed())
            return;
        sUsername = toUser.getUsername(); //username might have changed at the server
        sProfilePicUrl = toUser.getProfilePicUrl(); //profile pic might have changed at server
        Bindings.setPartnerProfilePic(mProfilePic, getViewModel());
        String bioStr = toUser.getBio() + "";
        bioStr = bioStr.equals("null") ? "" : bioStr;
        getViewModel().partnerBio.set(bioStr);
        getViewModel().partnerBio.set(bioStr);
        try {
            if (StringUtil.isNotEmpty(toUser.getCurrentGroupName()) && !toUser.getCurrentGroupName().equals("null")) {
                getViewModel().partnerCurrentGroup.set(getString(R.string.user_active_in_group) + " " + toUser.getCurrentGroupName());
            }
        } catch (Exception e) {
            getViewModel().partnerCurrentGroup.set("");
        }
    }

    public void collapseAppBar() {
        if (mIsAppBarExpanded)
            mAppBarLayout.setExpanded(false, true);
    }

    public void expandAppBar() {
        mAppBarLayout.setExpanded(true, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.private_chat_options_menu, menu);
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

        if (sUserid == privateChatViewModel.myUserid()) {
            if (menu.findItem(R.id.menu_block_user) != null) {
                menu.removeItem(R.id.menu_block_user);
            }
            if (menu.findItem(R.id.menu_report_user) != null) {
                menu.removeItem(R.id.menu_report_user);
            }
            if (menu.findItem(R.id.menu_sign_out) == null) {
                menu.add(0, R.id.menu_sign_out, 1, getString(R.string.sign_out));
            }
        } else {
            //if (menu.findItem(R.id.menu_sign_out) != null) {
            //    menu.removeItem(R.id.menu_sign_out);
            //}
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onHomeClicked();
                return true;
            case R.id.menu_add_friend:
                privateChatViewModel.addUser(sUsername, sProfilePicUrl, sUserid);
                return true;
            case R.id.menu_view_profile:
                if (!mIsAppBarExpanded)
                    togglePrivateChatAppBar();
                return true;
            case R.id.menu_block_user:
                new BlockUserDialogHelper(firebaseDatabase).showBlockUserQuestionDialog(this, sUserid, sUsername, sProfilePicUrl, mBlockedUserListener);
                return true;
            case R.id.menu_report_user:
                new ReportUserDialogHelper().showReportUserQuestionDialog(this, sUserid, sUsername, sProfilePicUrl);
                return true;
            case R.id.menu_pending_requests:
                showPendingRequests();
                return true;
            case R.id.menu_invite:
                showAppInviteActivity();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //@Override
    protected void onUserBlocked(int userid) {
        if (isActivityDestroyed())
            return;
        if (userid == getActiveUserid()) {
            finish();
        }
        Bundle payload = new Bundle();
        payload.putString("by", privateChatViewModel.myUsername());
        payload.putInt("userid", userid);
        FirebaseAnalytics.getInstance(this).logEvent(Events.USER_BLOCKED, payload);
    }

    private long mLastOnlineTimestamp = 0;

    @Override
    public void showCustomTitles(String username, long lastOnline) {

        if (lastOnline > mLastOnlineTimestamp) {
            String lastActive = "";
            lastActive = TimeUtil.getTimeAgo(lastOnline);
            mLastOnlineTimestamp = lastOnline;
            if (!TextUtils.isEmpty(lastActive)) {
                if (lastActive.equals(getString(R.string.just_now))) {
                    lastActive = getString(R.string.online_now);
                }
                getViewModel().partnerLastActive.set(lastActive);
            }
        }

        getViewModel().partnerUsername.set(username);
    }

    @Override
    public void onGroupChatClicked(GroupChatSummary groupChatSummary) {
        if (UserPreferences.getInstance().getLastGroupChatRoomVisited() == groupChatSummary.getId()) {
            finish();
        }
        super.onGroupChatClicked(groupChatSummary);
    }

    @Override
    public void onBackPressed() {

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        if (mIsAppBarExpanded) {
            togglePrivateChatAppBar();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onUserClicked(int userid, String username, String dpid, View transitionImageView) {
        if (userid == sUserid) {
            expandAppBar();
        } else {
            super.onUserClicked(userid,username, dpid, transitionImageView);
        }
    }

    private GestureDetectorCompat initializeGestureDetector() {
        return new GestureDetectorCompat(this, new FlingGestureListener() {
            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                if (velocityY > 20) {
                    if (!mIsAppBarExpanded) {
                        togglePrivateChatAppBar();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public PrivateChatViewModel getViewModel() {
        return (privateChatViewModel = ViewModelProviders.of(this, viewModelFactory).get(PrivateChatViewModel.class));
    }

    @Override
    public int getBindingVariable() {
        return BR.viewModel;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_private_chat;
    }

    @Override
    public void onReceiveAd(AdDownloaderInterface adDownloaderInterface, ReceivedBannerInterface receivedBanner) throws AdReceiveFailed {
        if (receivedBanner.getErrorCode() != ErrorCode.NO_ERROR) {
            adsHelper.loadBannerAd(this, firebaseRemoteConfig);
        }
    }

    private BlockedUserListener mBlockedUserListener = new BlockedUserListener() {
        @Override
        public void onUserBlocked(int userid) {
            PrivateChatActivity.this.onUserBlocked(userid);
        }

        @Override
        public void onUserUnblocked(int userid) {
            PrivateChatActivity.this.onUserUnblocked(userid);
        }
    };

    protected void onUserUnblocked(int userid) {
        Bundle payload = new Bundle();
        payload.putString("by", getViewModel().myUsername());
        payload.putInt("userid", userid);
        FirebaseAnalytics.getInstance(this).logEvent(Events.USER_UNBLOCKED, payload);
    }

    protected int getToolbarHeight() {
        return mToolbar.getHeight();
    }

    @Override
    public void showTypingDots() {
        showDotsParent(true);
        mDotsHandler.removeCallbacks(mDotsHideRunner);
        mDotsHandler.postDelayed(mDotsHideRunner, firebaseRemoteConfig.getLong(Constants
                .KEY_MAX_TYPING_DOTS_DISPLAY_TIME));
    }

    private void showDotsParent(boolean isAnimate) {
        if (binding.dotsLayout.getVisibility() == View.VISIBLE)
            return;
        binding.dotsLayout.setVisibility(View.VISIBLE);
        if (isAnimate)
            AnimationUtil.fadeInAnimation(binding.dotsLayout);
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

    private void hideDotsParent() {
        if (binding.dotsLayout.getVisibility() == View.GONE)
            return;
        binding.dotsLayout.setVisibility(View.GONE);
    }

    @Override
    public void showCannotChatWithBlockedUser(String username) {
        Toast.makeText(PrivateChatActivity.this,
                getString(R.string.cannot_chat_you_blocked_them) + " " +sUsername, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showSendOptions(FriendlyMessage friendlyMessage) {
        showSendOptions(friendlyMessage, binding.sendButton);
    }

    @Override
    public void showLikesCount(int count) {
        getViewModel().partnerLikesCount.set(count);
    }

    @Override
    public void showPartnerLikes() {
        Fragment fragment = UserLikedUserFragment.newInstance(sUserid,sUsername);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim
                .slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment,
                UserLikedUserFragment.TAG).addToBackStack(null).commit();
    }

    @Override
    public void togglePrivateChatAppBar() {
        if (!mIsAppBarExpanded) {
            mAppBarLayout.setExpanded(true, true);
            ScreenUtil.hideKeyboard(this);
        } else
            mAppBarLayout.setExpanded(false, true);
    }

    @Override
    public void listenForUsersInGroup() {
        //not applicable to private chat
    }

    @Override
    protected PhotoUploadHelper.PhotoType getRoomPhotoType() {
        return PhotoUploadHelper.PhotoType.privateChatRoomPhoto;
    }
}
