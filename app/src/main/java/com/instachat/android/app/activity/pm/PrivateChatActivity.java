package com.instachat.android.app.activity.pm;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v13.view.inputmethod.EditorInfoCompat;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
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
import com.instachat.android.app.activity.group.GroupChatActivity;
import com.instachat.android.app.activity.group.LogoutDialogHelper;
import com.instachat.android.app.adapter.ChatSummariesRecyclerAdapter;
import com.instachat.android.app.adapter.ChatsItemClickedListener;
import com.instachat.android.app.adapter.FriendlyMessageListener;
import com.instachat.android.app.adapter.MessageTextClickedListener;
import com.instachat.android.app.adapter.MessageViewHolder;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.blocks.BlockUserDialogHelper;
import com.instachat.android.app.blocks.BlockedUserListener;
import com.instachat.android.app.blocks.BlocksFragment;
import com.instachat.android.app.blocks.ReportUserDialogHelper;
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
import com.instachat.android.databinding.ActivityPrivateChatBinding;
import com.instachat.android.font.FontUtil;
import com.instachat.android.gcm.GCMHelper;
import com.instachat.android.messaging.InstachatMessagingService;
import com.instachat.android.messaging.NotificationHelper;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.TimeUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;
import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.ErrorCode;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.exception.AdReceiveFailed;
import com.tooltip.Tooltip;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by kevin on 9/16/2016.
 * The difference between Private and Group Chat:
 * Title in toolbar
 * Private has user photo
 * Different database references
 * Private messages result in partner devices receiving notifications
 * Private needs to fetch partner user from network api
 */
public class PrivateChatActivity extends BaseActivity<ActivityPrivateChatBinding, PrivateChatViewModel> implements GoogleApiClient.OnConnectionFailedListener,
        FriendlyMessageContainer, EasyPermissions.PermissionCallbacks, UploadListener, UserClickedListener,
        ChatsItemClickedListener, FriendlyMessageListener, AttachPhotoOptionsDialogHelper.PhotoOptionsListener,
        AdListenerInterface, PrivateChatNavigator {

    private static final int REQUEST_INVITE = 1;

    private static final String TAG = "PrivateChatActivity";
    private long mLastTypingTime;
    private ValueEventListener mUserInfoValueEventListener = null;
    private ImageView mProfilePic;
    private AppBarLayout mAppBarLayout;
    private GestureDetectorCompat mGestureDetector;
    private static int sUserid;
    private static String sUsername, sProfilePicUrl;
    private ActivityPrivateChatBinding binding;
    private PrivateChatViewModel privateChatViewModel;

    private Toolbar mToolbar;

    private EditText mMessageEditText;

    private ProgressDialog measuredProgressDialog;

    private PhotoUploadHelper mPhotoUploadHelper;
    private LeftDrawerHelper mLeftDrawerHelper;
    private String mDatabaseRoot;
    private ExternalSendIntentConsumer mExternalSendIntentConsumer;
    private Uri mSharePhotoUri;
    private String mShareText;
    private boolean mIsPendingRequestsAvailable;

    @Inject
    AdHelper adsHelper;

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
    protected NetworkApi networkApi;

    @Inject
    GCMHelper gcmHelper;

    @Inject
    LogoutDialogHelper logoutDialogHelper;

    @Inject
    FirebaseAuth firebaseAuth;

    @Inject
    ChatSummariesRecyclerAdapter chatsRecyclerViewAdapter;

    //@Override
    protected void setVisibleAd(boolean visibleAd) {
        binding.setVisibleAd(visibleAd);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_App);
        super.onCreate(savedInstanceState);
        privateChatViewModel.setNavigator(this);
        privateChatViewModel.onCreate();
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

        LinearLayout messageEditTextParent = (LinearLayout) findViewById(R.id.messageEditTextParent);
        mMessageEditText = createEditTextWithContentMimeTypes(
                new String[]{"image/png", "image/gif", "image/jpeg", "image/webp"});
        messageEditTextParent.addView(mMessageEditText);
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
                    onMeTyping();
                    showSendOptionsTooltip(binding.sendButton);
                } else {
                    setEnableSendButton(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        privateChatViewModel.fetchConfig(firebaseRemoteConfig);
        initButtons();

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
            mPhotoUploadHelper.setStorageRefString(privateChatViewModel.getDatabaseRoot());
            mPhotoUploadHelper.consumeExternallySharedPhoto((Uri) getIntent().getParcelableExtra(Constants
                    .KEY_SHARE_PHOTO_URI));
            getIntent().removeExtra(Constants.KEY_SHARE_PHOTO_URI);
        }
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_SHARE_MESSAGE)) {
            mMessageEditText.setText(getIntent().getStringExtra(Constants.KEY_SHARE_MESSAGE));
            getIntent().removeExtra(Constants.KEY_SHARE_MESSAGE);
        }
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(InstachatMessagingService.NOTIFICATION_ID_PENDING_REQUESTS);
        notificationManager.cancel(InstachatMessagingService.NOTIFICATION_ID_FRIEND_JUMPED_IN);
        privateChatViewModel.smallProgressCheck();
        onNewIntent(getIntent());
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
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
        loadBasicData(getIntent());
        networkApi.getUserById(this, sUserid, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final User toUser = User.fromResponse(response);
                    sUsername = toUser.getUsername();
                    sProfilePicUrl = toUser.getProfilePicUrl();
                    MLog.d(TAG, "after grab from server: toUser: ", toUser.getId(), " ", toUser.getUsername(), " ",
                            toUser.getProfilePicUrl());
                    populateUserProfile(toUser);
                    setCustomTitles(toUser.getUsername(), 0);
                    listenForPartnerTyping();
                    checkIfPartnerIsBlocked();

                    mUserInfoValueEventListener = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF
                            (sUserid)).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (isActivityDestroyed())
                                return;
                            try {
                                if (dataSnapshot.getValue() != null) {
                                    User user = dataSnapshot.getValue(User.class);

                                    //check if only the last active time changed
                                    boolean onlyUpdateLastActiveTime = true;
                                    if (!toUser.getUsername().equals(user.getUsername())) {
                                        onlyUpdateLastActiveTime = false;
                                        toUser.setUsername(user.getUsername());
                                    }
                                    if (!toUser.getProfilePicUrl().equals(user.getProfilePicUrl())) {
                                        onlyUpdateLastActiveTime = false;
                                        toUser.setProfilePicUrl(user.getProfilePicUrl());
                                    }
                                    String existingBio = toUser.getBio();
                                    String newBio = user.getBio();
                                    if (!existingBio.equals(newBio)) {
                                        onlyUpdateLastActiveTime = false;
                                        toUser.setBio(user.getBio());
                                    }

                                    if (toUser.getCurrentGroupId() != user.getCurrentGroupId()) {
                                        onlyUpdateLastActiveTime = false;
                                        toUser.setCurrentGroupName(user.getCurrentGroupName());
                                        toUser.setCurrentGroupId(user.getCurrentGroupId());
                                    }
                                    setCustomTitles(user.getUsername(), user.getLastOnline());
                                    if (!onlyUpdateLastActiveTime) {
                                        populateUserProfile(toUser);
                                    }
                                    MLog.d(TAG, "user info changed onlyUpdateLastActiveTime: ",
                                            onlyUpdateLastActiveTime);

                                }
                            } catch (Exception e) {
                                MLog.e(TAG, "", e);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                } catch (Exception e) {
                    showErrorToast("pca 1");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                MLog.e(TAG, "NetworkApi.getUserById(" + sUserid + ") failed in onCreate()", error);
                showErrorToast("pca 2");
            }
        });
        final NotificationManager notificationManager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        notificationManager.cancel(sUserid);
        MLog.d(TAG, "Cancelled notification " + sUserid);
        clearPrivateUnreadMessages(sUserid);

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

        final View.OnClickListener appBarOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleAppbar();
            }
        };
        mAppBarLayout.setOnClickListener(appBarOnClickListener);
        mGestureDetector = initializeGestureDetector();
        getToolbar().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mGestureDetector.onTouchEvent(motionEvent);
            }
        });
        presenceHelper.updateLastActiveTimestamp();
        listenForUpdatedLikeCount(sUserid);
    }

    private void checkIfSeenToolbarProfileTooltip(View anchor) {
        if (UserPreferences.getInstance().hasShownToolbarProfileTooltip())
            return;
        UserPreferences.getInstance().setShownToolbarProfileTooltip(true);
        final Tooltip tooltip = new Tooltip.Builder(anchor, R.style.drawer_tooltip_non_cancellable).setText(getString
                (R.string.toolbar_user_profile_tooltip)).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                if (tooltip.isShowing())
                    tooltip.dismiss();
            }
        }, firebaseRemoteConfig.getLong(Constants.KEY_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME));

    }

    private void showErrorToast(String extra) {
        try {
            Toast.makeText(PrivateChatActivity.this, getString(R.string.general_api_error, extra), Toast
                    .LENGTH_SHORT).show();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    private void toggleAppbar() {
        if (!mIsAppBarExpanded) {
            mAppBarLayout.setExpanded(true, true);
            ScreenUtil.hideKeyboard(this);
        } else
            mAppBarLayout.setExpanded(false, true);
    }

    private boolean mIsAppBarExpanded = true; //initially it's expanded

    private void initializePrivateChatSummary() {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants
                .MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).child(sUserid + "");
        PrivateChatSummary summary = new PrivateChatSummary();
        summary.setName(sUsername);
        summary.setDpid(sProfilePicUrl);
        summary.setAccepted(true);
        Map<String, Object> map = summary.toMap();
        map.put(Constants.FIELD_LAST_MESSAGE_SENT_TIMESTAMP, ServerValue.TIMESTAMP);
        ref.updateChildren(map);
    }

    @Override
    public void onDestroy() {
        if (mUserInfoValueEventListener != null) {
            try {
                FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(sUserid)).removeEventListener
                        (mUserInfoValueEventListener);
            } catch (Exception e) {
                MLog.e(TAG, "", e);
            }
        }
        if (mTypingValueEventListener != null && mTypingReference != null) {
            mTypingReference.removeEventListener(mTypingValueEventListener);
        }
        if (mTotalLikesRef != null)
            mTotalLikesRef.removeEventListener(mTotalLikesEventListener);
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
            MLog.d(TAG,"C kevin scroll: "+(messagesAdapter.getItemCount() - 1) + " text: "+ messagesAdapter.peekLastMessage());
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
        initializePrivateChatSummary();
        if (isPrivateChat()) {
            Bundle payload = new Bundle();
            payload.putString("to", sUsername);
            payload.putString("from", privateChatViewModel.myUsername());
            payload.putString("type", friendlyMessage.getImageUrl() != null ? "photo" : "text");
            payload.putBoolean("one-time", friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME);
            FirebaseAnalytics.getInstance(this).logEvent(Events.MESSAGE_PRIVATE_SENT_EVENT, payload);
        }
    }

    //@Override
    protected void onMeTyping() {
        try {
            if (System.currentTimeMillis() - mLastTypingTime < 3000) {
                return;
            }
            FirebaseDatabase.getInstance()
                    .getReference(Constants.PRIVATE_CHAT_TYPING_REF(sUserid))
                    .child("" +privateChatViewModel.myUserid())
                    .child(Constants.CHILD_TYPING).setValue(true);
            mLastTypingTime = System.currentTimeMillis();
        } catch (Exception e) {
            MLog.e(TAG, "onMeTyping() failed", e);
        }
    }

    public static void startPrivateChatActivity(Activity activity, int userid, String username, String profilePicUrl,
                                                final boolean autoAddUser, final View transitionImageView, Uri
                                                        sharePhotoUri, String shareMessage) {
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
        } else {
            activity.startActivity(intent);
        }
    }

    public static int getActiveUserid() {
        return sUserid;
    }

    //@Override
    protected void onRemoteUserTyping(int userid, String username, String dpid) {
        if (isActivityDestroyed() || this.sUserid != userid) {
            return;
        }
        showTypingDots();
    }

    private void loadBasicData(Intent intent) {

        ((TextView) findViewById(R.id.customTitleInToolbar)).setText(sUsername);
        ((TextView) findViewById(R.id.customTitleInParallax)).setText(sUsername);

        if (intent.getBooleanExtra(Constants.KEY_AUTO_ADD_PERSON, false)) {
            //add this person to my left drawer and remove them from pending requests
            PrivateChatSummary privateChatSummary = new PrivateChatSummary();
            privateChatSummary.setName(sUsername);
            privateChatSummary.setDpid(sProfilePicUrl);
            privateChatSummary.setAccepted(true);
            privateChatSummary.setLastMessageSentTimestamp(System.currentTimeMillis());
            final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants
                    .MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).child(sUserid + "");
            ref.updateChildren(privateChatSummary.toMap());

            /**
             * remove the person from your pending requests
             */
            FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_REQUEST_STATUS_PARENT_REF(sUserid,
                    UserPreferences.getInstance().getUserId())).removeValue();
        }

    }

    private void populateUserProfile(final User toUser) {
        if (toUser == null || isActivityDestroyed())
            return;
        final ImageView toolbarProfileImageView = (ImageView) findViewById(R.id.topCornerUserThumb);
        final ImageView miniPic = (ImageView) findViewById(R.id.superSmallProfileImage);
        final TextView bio = (TextView) findViewById(R.id.bio);

        if (TextUtils.isEmpty(toUser.getProfilePicUrl())) {
            toolbarProfileImageView.setImageResource(R.drawable.ic_anon_person_36dp);
            miniPic.setImageResource(R.drawable.ic_anon_person_36dp);
            mProfilePic.setImageResource(R.drawable.ic_anon_person_36dp);
            collapseAppbarAfterDelay();
        } else {
            try {
                Glide.with(PrivateChatActivity.this).load(toUser.getProfilePicUrl()).error(R.drawable
                        .ic_anon_person_36dp)
                        //.crossFade()
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                                       boolean isFirstResource) {
                                if (isActivityDestroyed())
                                    return false;
                                collapseAppbarAfterDelay();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model,
                                                           Target<GlideDrawable> target, boolean isFromMemoryCache,
                                                           boolean isFirstResource) {
                                if (isActivityDestroyed())
                                    return false;
                                collapseAppbarAfterDelay();
                                return false;
                            }
                        }).into(mProfilePic);
                Glide.with(PrivateChatActivity.this).load(toUser.getProfilePicUrl()).error(R.drawable
                        .ic_anon_person_36dp).crossFade().into(miniPic);
                Glide.with(PrivateChatActivity.this).load(toUser.getProfilePicUrl()).error(R.drawable
                        .ic_anon_person_36dp).crossFade().into(toolbarProfileImageView);

            } catch (Exception e) {
                MLog.e(TAG, "onDrawerOpened() could not find user photo in google cloud storage", e);
                miniPic.setImageResource(R.drawable.ic_anon_person_36dp);
                collapseAppbarAfterDelay();
            }
        }
        bio.setVisibility(TextUtils.isEmpty(toUser.getBio()) ? View.GONE : View.VISIBLE);
        String bioStr = toUser.getBio() + "";
        bioStr = bioStr.equals("null") ? "" : bioStr;
        bio.setText(bioStr);
        TextView activeGroup = (TextView) findViewById(R.id.activeGroup);
        if (toUser.getCurrentGroupId() != 0 && !TextUtils.isEmpty(toUser.getCurrentGroupName())) {
            activeGroup.setVisibility(View.VISIBLE);
            try {
                activeGroup.setText(getString(R.string.user_active_in_group, toUser.getCurrentGroupName()));
            } catch (Exception e) {
                activeGroup.setText(toUser.getCurrentGroupName());
            }
            activeGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    GroupChatSummary groupChatSummary = new GroupChatSummary();
                    groupChatSummary.setId(toUser.getCurrentGroupId());
                    groupChatSummary.setName(toUser.getCurrentGroupName());
                    onGroupChatClicked(groupChatSummary);
                }
            });
        } else {
            activeGroup.setVisibility(View.GONE);
        }

    }

    private void collapseAppbarAfterDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                if (mIsAppBarExpanded)
                    mAppBarLayout.setExpanded(false, true);
            }
        }, firebaseRemoteConfig.getLong(Constants.KEY_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY));
    }

    private void clearPrivateUnreadMessages(int toUserid) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants
                    .MY_PRIVATE_CHATS_SUMMARY_PARENT_REF());
            ref.child(toUserid + "").child(Constants.CHILD_UNREAD_MESSAGES).removeValue();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.private_chat_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onHomeClicked();
                return true;
            case R.id.menu_view_profile:
                if (!mIsAppBarExpanded)
                    toggleAppbar();
                return true;
            case R.id.menu_block_user:
                new BlockUserDialogHelper().showBlockUserQuestionDialog(this, sUserid, sUsername, sProfilePicUrl,
                        getBlockedUserListener());
                return true;
            case R.id.menu_report_user:
                new ReportUserDialogHelper().showReportUserQuestionDialog(this, sUserid, sUsername, sProfilePicUrl);
                return true;
            case R.id.menu_pending_requests:
                onPendingRequestsClicked();
                return true;
            case R.id.menu_invite:
                sendInvitation();
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
    protected void setToolbarOnClickListener(Toolbar toolbar) {
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleAppbar();
            }
        });
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

    private void setCustomTitles(String username, long lastOnline) {

        if (lastOnline > mLastOnlineTimestamp) {
            String lastActive = "";
            lastActive = TimeUtil.getTimeAgo(lastOnline);
            mLastOnlineTimestamp = lastOnline;
            if (!TextUtils.isEmpty(lastActive)) {
                if (lastActive.equals(getString(R.string.just_now))) {
                    lastActive = getString(R.string.online_now);
                }
                ((TextView) findViewById(R.id.customSubtitleInParallax)).setText(lastActive);
                ((TextView) findViewById(R.id.customSubtitleInToolbar)).setText(lastActive);
            }
        }

        ((TextView) findViewById(R.id.customTitleInToolbar)).setText(username);
        ((TextView) findViewById(R.id.customTitleInParallax)).setText(username);
    }

    @Override
    public void onGroupChatClicked(GroupChatSummary groupChatSummary) {
        if (UserPreferences.getInstance().getLastGroupChatRoomVisited() == groupChatSummary.getId()) {
            finish();
        }
        closeBothDrawers();
        GroupChatActivity.startGroupChatActivity(this, groupChatSummary.getId(), groupChatSummary.getName(), mSharePhotoUri, mShareText);
        mSharePhotoUri = null;
        mShareText = null;
    }

    private DatabaseReference mTypingReference;
    private ValueEventListener mTypingValueEventListener;

    private void listenForPartnerTyping() {
        mTypingReference = FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHAT_TYPING_REF(sUserid)).
                child("" + sUserid).child(Constants.CHILD_TYPING);
        mTypingReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mTypingValueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (isActivityDestroyed())
                            return;
                        MLog.d(TAG, "isTyping: onDataChange() dataSnapshot ", dataSnapshot);
                        if (dataSnapshot.exists()) {
                            boolean isTyping = dataSnapshot.getValue(Boolean.class);
                            MLog.d(TAG, "isTyping: ", isTyping);
                            if (isTyping) {
                                onRemoteUserTyping(sUserid, sUsername, sProfilePicUrl);
                                mTypingReference.setValue(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };
                mTypingReference.addValueEventListener(mTypingValueEventListener);
            }
        });

    }

    //@Override
    protected boolean isPrivateChat() {
        return true;
    }

    @Override
    public void onBackPressed() {

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        if (mIsAppBarExpanded) {
            toggleAppbar();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onUserClicked(int userid, String username, String dpid, View transitionImageView) {
        if (userid == sUserid) {
            mAppBarLayout.setExpanded(true, true);
        } else {
            closeBothDrawers();
            ScreenUtil.hideVirtualKeyboard(mMessageEditText);
            PrivateChatActivity.startPrivateChatActivity(this, userid, username, dpid, false, transitionImageView, null,
                    null);
        }
    }

    private GestureDetectorCompat initializeGestureDetector() {
        return new GestureDetectorCompat(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                if (velocityY > 20) {
                    if (!mIsAppBarExpanded) {
                        toggleAppbar();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void checkIfPartnerIsBlocked() {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF()).child
                (sUserid + "");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "onDataChange() snapshot: " + dataSnapshot, " ref: ", ref);
                ref.removeEventListener(this);
                if (dataSnapshot.getValue() != null) {
                    Toast.makeText(PrivateChatActivity.this,
                            getString(R.string.cannot_chat_you_blocked_them,
                            sUsername), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ref.removeEventListener(this);
                showErrorToast("p");
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

        if (isPrivateChat() && sUserid == privateChatViewModel.myUserid()) {
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
            if (menu.findItem(R.id.menu_sign_out) != null) {
                menu.removeItem(R.id.menu_sign_out);
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    private DatabaseReference mTotalLikesRef;
    private ValueEventListener mTotalLikesEventListener;

    private void listenForUpdatedLikeCount(int userid) {
        final View likesParent = findViewById(R.id.likesParent);
        final TextView likesCount = findViewById(R.id.likesCount);
        mTotalLikesRef = FirebaseDatabase.getInstance().getReference(Constants.USER_TOTAL_LIKES_RECEIVED_REF(userid));
        mTotalLikesEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (likesParent.getVisibility() != View.VISIBLE) {
                        likesParent.setVisibility(View.VISIBLE);
                        likesCount.setVisibility(View.VISIBLE);
                    }
                    long count = dataSnapshot.getValue(Long.class);
                    if (count == 1) {
                        likesCount.setText(getString(R.string.like_singular));
                    } else {
                        likesCount.setText(getString(R.string.likes_plural, count + ""));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mTotalLikesRef.addValueEventListener(mTotalLikesEventListener);
        likesParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment fragment = new UserLikedUserFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(Constants.KEY_USERID, sUserid);
                bundle.putString(Constants.KEY_USERNAME, sUsername);
                fragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim
                        .slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment,
                        UserLikedUserFragment.TAG).addToBackStack(null).commit();

            }
        });
    }

    private void hideSmallProgressAfter() {
        addDisposable(Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                .take(5)
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        hideSmallProgressCircle();
                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long value) throws Exception {
                        if (messagesAdapter.getItemCount() > 0) {
                            hideSmallProgressCircle();
                        }
                    }
                }));

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
    public void setCurrentFriendlyMessage(int position) {
        MLog.d(TAG,"A kevin scroll: "+(position + 1) + " text: "+ messagesAdapter.peekLastMessage());
        binding.messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount()-1);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        mPhotoUploadHelper.onPermissionsGranted(requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    @Override
    public void onPrivateChatClicked(PrivateChatSummary privateChatSummary) {
        closeBothDrawers();
        PrivateChatActivity.startPrivateChatActivity(this, Integer.parseInt(privateChatSummary.getId()),
                privateChatSummary.getName(), privateChatSummary.getDpid(), false, null, mSharePhotoUri, mShareText);
        mSharePhotoUri = null;
        mShareText = null;
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
    public void onErrorReducingPhotoSize() {
        MLog.i(TAG, "onErrorReducingPhotoSize()");
        if (isActivityDestroyed())
            return;
        showPhotoReduceError();
    }


    private void showPhotoReduceError() {
        addDisposable(Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Toast.makeText(PrivateChatActivity.this, "Could not read photo", Toast.LENGTH_SHORT).show();
                return false;
            }
        }).subscribeOn(schedulerProvider.ui()).subscribe());
    }

    @Override
    public FriendlyMessage getFriendlyMessage(int position) {
        return (FriendlyMessage) messagesAdapter.getItem(position);
    }

    @Override
    public String getFriendlyMessageDatabase() {
        return mDatabaseRoot;
    }

    @Override
    public int getFriendlyMessageCount() {
        return messagesAdapter.getItemCount();
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
        if (measuredProgressDialog != null) {
            try {
                measuredProgressDialog.setMax(max);
                measuredProgressDialog.setProgress(current);
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

            final FriendlyMessage friendlyMessage = new FriendlyMessage("",
                    privateChatViewModel.myUsername(),
                    privateChatViewModel.myUserid(),
                    privateChatViewModel.myDpid(),
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

    private void showProgressDialog() {
        if (measuredProgressDialog == null) {
            measuredProgressDialog = new ProgressDialog(this);
            measuredProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            measuredProgressDialog.setIndeterminate(false);
            measuredProgressDialog.setProgressNumberFormat("%1dk / %2dk");
        }
        measuredProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (measuredProgressDialog != null && measuredProgressDialog.isShowing()) {
            measuredProgressDialog.dismiss();
        }
    }

    private int mAttachPhotoMessageType;

    @Override
    public void onPhotoGallery() {
        mPhotoUploadHelper.setStorageRefString(privateChatViewModel.getDatabaseRoot());
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
        mPhotoUploadHelper.launchCamera(false);
    }

    @Override
    public void onPhotoTake() {
        mPhotoUploadHelper.setStorageRefString(privateChatViewModel.getDatabaseRoot());
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
        mPhotoUploadHelper.launchCamera(true);
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
    public void onFriendlyMessageFail(FriendlyMessage friendlyMessage) {
        if (isActivityDestroyed())
            return;
        mMessageEditText.setText(friendlyMessage.getText());
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string
                .could_not_send_message)).show();
        FirebaseAnalytics.getInstance(this).logEvent(Events.MESSAGE_FAILED, null);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        MLog.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onReceiveAd(AdDownloaderInterface adDownloaderInterface, ReceivedBannerInterface receivedBanner) throws AdReceiveFailed {
        if(receivedBanner.getErrorCode() != ErrorCode.NO_ERROR){
            setVisibleAd(false);
            adsHelper.loadAd(this);
        } else {
            setVisibleAd(true);
        }
    }

    private void initPhotoHelper(Bundle savedInstanceState) {
        mPhotoUploadHelper = new PhotoUploadHelper(this, this);
        mPhotoUploadHelper.setPhotoUploadListener(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY_PHOTO_TYPE)) {
            PhotoUploadHelper.PhotoType photoType = PhotoUploadHelper.PhotoType.valueOf(savedInstanceState.getString
                    (Constants.KEY_PHOTO_TYPE));
            mPhotoUploadHelper.setPhotoType(photoType);
            MLog.d(TAG, "initPhotoHelper: retrieved from saved instance state: " + photoType);
        }
    }

    private void setupDrawers() {
        setupLeftDrawerContent();
    }

    private void setupToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);
        setupToolbarTitle(mToolbar);
        setToolbarOnClickListener(mToolbar);
    }

    private void initFirebaseAdapter() {
        messagesAdapter = privateChatViewModel.getMessagesAdapter(firebaseRemoteConfig, map);
        messagesAdapter.setActivity(this, this, (FrameLayout) findViewById(R.id.fragment_content));
        messagesAdapter.setMessageTextClickedListener(new MessageTextClickedListener() {
            @Override
            public void onMessageClicked(final int position) {
                openFullScreenTextView(position);
            }
        });
        messagesAdapter.setBlockedUserListener(mBlockedUserListener);
        messagesAdapter.setFriendlyMessageListener(this);
        messagesAdapter.setUserThumbClickedListener(this);
        messagesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {

                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = messagesAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
                MLog.d(TAG,"scroll debug: lastVisiblePosition: "+lastVisiblePosition + " text: "+ messagesAdapter.peekLastMessage()
                        +" positionStart: "+positionStart + " friendlyMessageCount: "+friendlyMessageCount);
                if (lastVisiblePosition == -1 || ((lastVisiblePosition+4) >=  positionStart)) {
                    MLog.d(TAG,"B kevin scroll: "+(positionStart) + " text: "+ messagesAdapter.peekLastMessage());
                    binding.messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount()-1);
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
                                return PrivateChatActivity.this.onCommitContent(
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
        if (inputContentInfo != null && inputContentInfo.getDescription() != null) {
            if (inputContentInfo.getDescription().toString().contains("image/gif")) {
                final FriendlyMessage friendlyMessage = new FriendlyMessage("",
                        privateChatViewModel.myUsername(),
                        privateChatViewModel.myUserid(),
                        privateChatViewModel.myDpid(),
                        linkUri.toString(), false, false, null, System.currentTimeMillis());
                friendlyMessage.setMessageType(FriendlyMessage.MESSAGE_TYPE_NORMAL);
                messagesAdapter.sendFriendlyMessage(friendlyMessage);
            }
        }
        return true;
    }

    private void setEnableSendButton(final boolean isEnable) {

        if (isEnable && binding.sendButton.isEnabled() || !isEnable && !binding.sendButton.isEnabled())
            return; //already set

        binding.sendButton.setEnabled(isEnable);

        final Animation hideAnimation = AnimationUtils.loadAnimation(PrivateChatActivity.this, R.anim.fab_scale_down);
        final Animation showAnimation = AnimationUtils.loadAnimation(PrivateChatActivity.this, R.anim.fab_scale_up);

        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.sendButton.startAnimation(showAnimation);
                //binding.sendButton.setEnabled(isEnable);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        binding.sendButton.startAnimation(hideAnimation);
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
                payload.putString("username", privateChatViewModel.myUsername());
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

    private void showSendOptionsTooltip(View anchor) {
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

    private boolean mShownSendOptionsProtips;

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
                new MessageOptionsDialogHelper().showSendOptions(PrivateChatActivity.this, binding.attachButton, null, new
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

    private boolean validateBeforeSendText(final String text, boolean showOptions) {
        if (StringUtil.isEmpty(text)) {
            return false;
        }
        if (isNeedsDp())
            return false;
        final FriendlyMessage friendlyMessage = new FriendlyMessage(text,
                privateChatViewModel.myUsername(),
                privateChatViewModel.myUserid(),
                privateChatViewModel.myDpid(), null,
                false, false, null, System.currentTimeMillis());
        if (!showOptions) {
            sendText(friendlyMessage);
            return true;
        }
        new MessageOptionsDialogHelper().showSendOptions(PrivateChatActivity.this, binding.sendButton, friendlyMessage, new
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

    private boolean isNeedsDp() {

        if (!TextUtils.isEmpty(privateChatViewModel.myDpid()))
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

    protected BlockedUserListener getBlockedUserListener() {
        return mBlockedUserListener;
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

    private void onPendingRequestsClicked() {
        closeLeftDrawer();
        Fragment fragment = new RequestsFragment();
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim
                .slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, RequestsFragment.TAG)
                .addToBackStack(null).commit();
    }

    protected void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title)).setMessage
                (getString(R.string.invitation_message)).setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    protected void onUserUnblocked(int userid) {
        Bundle payload = new Bundle();
        payload.putString("by", privateChatViewModel.myUsername());
        payload.putInt("userid", userid);
        FirebaseAnalytics.getInstance(this).logEvent(Events.USER_UNBLOCKED, payload);
    }

    protected int getToolbarHeight() {
        return mToolbar.getHeight();
    }

    protected Toolbar getToolbar() {
        return mToolbar;
    }

    protected void showTypingDots() {
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

    private void sendText(FriendlyMessage friendlyMessage) {
        try {
            messagesAdapter.sendFriendlyMessage(friendlyMessage);
            mMessageEditText.setText("");//fast double taps on send can cause 2x sends!
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
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

    private void setupLeftDrawerContent() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView == null)
            return;
        View headerView = getLayoutInflater().inflate(R.layout.left_nav_header, navigationView, false);
        View drawerView = getLayoutInflater().inflate(R.layout.left_drawer_layout, navigationView, false);
        navigationView.addView(drawerView);
        navigationView.addHeaderView(headerView);
        mLeftDrawerHelper = new LeftDrawerHelper(networkApi, this, this, binding.drawerLayout, mLeftDrawerEventListener);
        mLeftDrawerHelper.setup(navigationView);
        mLeftDrawerHelper.setUserLikedUserListener(mUserLikedUserListener);

        chatsRecyclerViewAdapter.setup(this, this, true);
        RecyclerView recyclerView = (RecyclerView) drawerView.findViewById(R.id.drawerRecyclerView);
        recyclerView.setLayoutManager(new StickyLayoutManager(this, chatsRecyclerViewAdapter));
        recyclerView.setAdapter(chatsRecyclerViewAdapter);
        chatsRecyclerViewAdapter.populateData();
    }

    private LeftDrawerEventListener mLeftDrawerEventListener = new LeftDrawerEventListener() {
        @Override
        public void onProfilePicChangeRequest(boolean isLaunchCamera) {
            PrivateChatActivity.this.onProfilePicChangeRequest(isLaunchCamera);
        }

        @Override
        public void onPendingRequestsClicked() {
            PrivateChatActivity.this.onPendingRequestsClicked();
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
        mPhotoUploadHelper.setStorageRefString(Constants.DP_STORAGE_BASE_REF(privateChatViewModel.myUserid()));
        mPhotoUploadHelper.launchCamera(isLaunchCamera);
    }

    private UserLikedUserListener mUserLikedUserListener = new UserLikedUserListener() {
        @Override
        public void onMyLikersClicked() {
            Fragment fragment = new UserLikedUserFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.KEY_USERID, privateChatViewModel.myUserid());
            bundle.putString(Constants.KEY_USERNAME, privateChatViewModel.myUsername());
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R
                    .anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, UserLikedUserFragment
                    .TAG).addToBackStack(null).commit();
        }
    };

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
