package com.instachat.android;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ath.fuel.FuelInjector;
import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.cocosw.bottomsheet.BottomSheet;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.instachat.android.adapter.AdapterPopulateHolderListener;
import com.instachat.android.adapter.ChatSummariesRecyclerAdapter;
import com.instachat.android.adapter.ChatsItemClickedListener;
import com.instachat.android.adapter.FriendlyMessageListener;
import com.instachat.android.adapter.GroupChatUsersRecyclerAdapter;
import com.instachat.android.adapter.MessageTextClickedListener;
import com.instachat.android.adapter.MessageViewHolder;
import com.instachat.android.adapter.MessagesRecyclerAdapter;
import com.instachat.android.adapter.UserClickedListener;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.api.UploadListener;
import com.instachat.android.blocks.BlockedUserListener;
import com.instachat.android.blocks.BlocksFragment;
import com.instachat.android.font.FontUtil;
import com.instachat.android.fullscreen.FriendlyMessageContainer;
import com.instachat.android.fullscreen.FullScreenTextFragment;
import com.instachat.android.gcm.GCMHelper;
import com.instachat.android.likes.UserLikedUserFragment;
import com.instachat.android.likes.UserLikedUserListener;
import com.instachat.android.login.LogoutDialogHelper;
import com.instachat.android.login.SignInActivity;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.GroupChatSummary;
import com.instachat.android.model.PrivateChatSummary;
import com.instachat.android.model.User;
import com.instachat.android.options.MessageOptionsDialogHelper;
import com.instachat.android.requests.RequestsFragment;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.tooltip.Tooltip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class GroupChatActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener, FriendlyMessageContainer, EasyPermissions.PermissionCallbacks, UploadListener, UserClickedListener, ChatsItemClickedListener, FriendlyMessageListener {

    private static final String TAG = "GroupChatActivity";

    private static final int REQUEST_INVITE = 1;
    private static final String MESSAGE_SENT_EVENT = "message_sent";

    private Toolbar mToolbar;
    private View mSendButton, mAttachButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private MessagesRecyclerAdapter<FriendlyMessage, MessageViewHolder> mMessagesAdapter;
    private ProgressBar mProgressBar;
    private FirebaseAuth mFirebaseAuth;
    //private FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText mMessageEditText;
    private TextView mUsernameTyping;
    private AdView mAdView;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    //private GoogleApiClient mGoogleApiClient;
    private DrawerLayout mDrawerLayout;
    private DatabaseReference mMeTypingRef;

    private BroadcastReceiver mDownloadReceiver;
    private ProgressDialog mProgressDialog;

    // [START declare_ref]
    private PhotoUploadHelper mPhotoUploadHelper;
    private LeftDrawerHelper mLeftDrawerHelper;
    private String mDatabaseRoot;
    private View mDotsLayoutParent;
    private ChatSummariesRecyclerAdapter mChatsRecyclerViewAdapter;
    private GroupChatUsersRecyclerAdapter mGroupChatUsersRecyclerAdapter;
    private ExternalSendIntentConsumer mExternalSendIntentConsumer;
    private Uri mSharePhotoUri;
    private String mShareText;
    private long mGroupId = 0L;
    private boolean mIsPendingRequestsAvailable;

    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) { // Android support FragmentManager v4 erroneously remembers every Fragment, even when "retain instance" is
            // false; remove the saved Fragments so we don't get into a dueling layout issue between the new Fragments we're trying to make and the
            // old ones being restored.
            savedInstanceState.remove("android:support:fragments");
        }
        super.onCreate(savedInstanceState);
        initDatabaseRef();
        DataBindingUtil.setContentView(this, getLayout());
        initPhotoHelper(savedInstanceState);
        setupDrawers();
        setupToolbar();
        //mDotsLoader = (AnimatedDotLoadingView) findViewById(R.id.text_dot_loader);
        mDotsLayoutParent = findViewById(R.id.dotsLayout);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        if (!Preferences.getInstance().isLoggedIn()) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        GCMHelper.onCreate(this);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

//        try {
//            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
//            MLog.w(TAG, "FirebaseDatabase.getInstance().setPersistenceEnabled(true) succeeded");
//        } catch (Exception e) {
//            //MLog.e(TAG, "FirebaseDatabase.getInstance().setPersistenceEnabled(true) failed: " + e);
//        }

        initRemoteConfig();
        fetchConfig();
        initFirebaseAdapter();

        FuelInjector.ignite(this, this);

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mMessagesAdapter);

        // Initialize and request AdMob ad.
        /*
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        */
        // Initialize Firebase Measurement.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mUsernameTyping = ((TextView) findViewById(R.id.usernameTyping));
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        FontUtil.setTextViewFont(mMessageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter((int) mFirebaseRemoteConfig.getLong(Constants.KEY_MAX_MESSAGE_LENGTH))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

                int length = mMessageEditText.getText().toString().trim().length();
                //MLog.i(TAG, "input onTextChanged() text [start]: " + start + " [before]: " + before + " [count]: " + count, " last delta: ", lastDelta, " length: ", length);

                if (length > 0) {
                    setEnableSendButton(true);
                    onMeTyping();
                    showSendOptionsTooltip(mSendButton);
                } else {
                    setEnableSendButton(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        //initDownloadReceiver();
        initButtons();

        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_GROUP_NAME)) {
            getSupportActionBar().setTitle(getIntent().getStringExtra(Constants.KEY_GROUP_NAME));
        }

        mExternalSendIntentConsumer = new ExternalSendIntentConsumer(this);
        mExternalSendIntentConsumer.setListener(new ExternalSendIntentConsumer.ExternalSendIntentListener() {
            @Override
            public void onHandleSendImage(final Uri imageUri) {
                mDrawerLayout.openDrawer(GravityCompat.START);
                mSharePhotoUri = imageUri;
            }

            @Override
            public void onHandleSendText(final String text) {
                mDrawerLayout.openDrawer(GravityCompat.START);
                mShareText = text;
            }
        });
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_SHARE_PHOTO_URI)) {
            mPhotoUploadHelper.setStorageRefString(getDatabaseRoot());
            mPhotoUploadHelper.consumeExternallySharedPhoto((Uri) getIntent().getParcelableExtra(Constants.KEY_SHARE_PHOTO_URI));
            getIntent().removeExtra(Constants.KEY_SHARE_PHOTO_URI);
        }
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_SHARE_MESSAGE)) {
            mMessageEditText.setText(getIntent().getStringExtra(Constants.KEY_SHARE_MESSAGE));
            getIntent().removeExtra(Constants.KEY_SHARE_MESSAGE);
        }
        addUserPresenceToGroup();
        listenForTyping();
    }

    private DatabaseReference mGroupSummaryRef;
    private ValueEventListener mGroupSummaryListener;

    private void removeGroupInfoListener() {
        if (mGroupSummaryRef != null && mGroupSummaryListener != null) {
            mGroupSummaryRef.removeEventListener(mGroupSummaryListener);
        }
    }

    private long mLastTypingTime;

    private Map<String, Object> mMeTypingMap = new HashMap<>(3);

    protected void onMeTyping() {
        try {
            if (System.currentTimeMillis() - mLastTypingTime < 3000)
                return;
            mLastTypingTime = System.currentTimeMillis();
            if (mMeTypingMap.size() == 0) {
                mMeTypingMap.put(Constants.CHILD_TYPING, true);
                mMeTypingMap.put(Constants.CHILD_USERNAME, myUsername());
            }
            FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_TYPING_REF(mGroupId, myUserid())).setValue(mMeTypingMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //immediately flip the value back to false in order
                            //to pick up further typing by my person
                            mMeTypingRef.setValue(false);
                        }
                    });
        } catch (Exception e) {
            MLog.e(TAG, "onMeTyping() failed", e);
        }
    }

    private DatabaseReference mTypingInRoomReference;
    private ChildEventListener mTypingInRoomEventListener;

    private void listenForTyping() {

        mMeTypingRef = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_TYPING_REF(mGroupId, myUserid())).
                child(Constants.CHILD_TYPING);
        mMeTypingRef.setValue(false);

        mTypingInRoomReference = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_TYPING_PARENT_REF(mGroupId));
        mTypingInRoomEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "isTyping: onChildAdded() dataSnapshot ", dataSnapshot, " s: ", s);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                MLog.d(TAG, "isTyping: onChildChanged() dataSnapshot ", dataSnapshot, " s: ", s);
                if (isActivityDestroyed())
                    return;
                MLog.d(TAG, "isTyping: onDataChange() dataSnapshot ", dataSnapshot);
                if (dataSnapshot.exists() && dataSnapshot.hasChild(Constants.CHILD_TYPING)) {
                    boolean isTyping = dataSnapshot.child(Constants.CHILD_TYPING).getValue(Boolean.class);
                    String username = dataSnapshot.child(Constants.CHILD_USERNAME).getValue(String.class);
                    //String dpid = dataSnapshot.child(Constants.CHILD_DPID).getValue(String.class);
                    int userid = Integer.parseInt(dataSnapshot.getKey());
                    //MLog.d(TAG, "isTyping: ", isTyping, " dpid: ", dpid, " userid ", userid);
                    if (isTyping) {
                        onRemoteUserTyping(userid, username, null);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "isTyping: onChildRemoved() dataSnapshot ", dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mTypingInRoomReference.addChildEventListener(mTypingInRoomEventListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register download receiver
        //LocalBroadcastManager.getInstance(this)
        //        .registerReceiver(mDownloadReceiver, MyDownloadService.getIntentFilter());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
        mSendButton.setEnabled(mMessageEditText.getText().toString().trim().length() > 0);
        if (Preferences.getInstance().isLoggedIn()) {
            GCMHelper.onResume(this);
        }
        if (mExternalSendIntentConsumer != null)
            mExternalSendIntentConsumer.consumeIntent(getIntent());
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
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
    public void onStop() {
        super.onStop();
        // Unregister download receiver
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mDownloadReceiver);
    }

    @Override
    public void onDestroy() {
        if (mPhotoUploadHelper != null)
            mPhotoUploadHelper.cleanup();
        if (mMessagesAdapter != null)
            mMessagesAdapter.cleanup();
        if (mLeftDrawerHelper != null)
            mLeftDrawerHelper.cleanup();
        if (mAdView != null)
            mAdView.destroy();
        if (mChatsRecyclerViewAdapter != null)
            mChatsRecyclerViewAdapter.cleanup();
        if (mExternalSendIntentConsumer != null)
            mExternalSendIntentConsumer.cleanup();
        if (mGroupChatUsersRecyclerAdapter != null)
            mGroupChatUsersRecyclerAdapter.cleanup();
        mBlockedUserListener = null;
        if (mTypingInRoomReference != null && mTypingInRoomEventListener != null)
            mTypingInRoomReference.removeEventListener(mTypingInRoomEventListener);
        super.onDestroy();
    }

    void initDatabaseRef() {
        String databaseRef;
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_GROUPID)) {
            mGroupId = getIntent().getLongExtra(Constants.KEY_GROUPID, Constants.DEFAULT_PUBLIC_GROUP_ID);
            databaseRef = Constants.GROUP_CHAT_REF(mGroupId);
            Preferences.getInstance().setLastGroupChatRoomVisited(mGroupId);
        } else {
            mGroupId = Preferences.getInstance().getLastGroupChatRoomVisited();
            databaseRef = Constants.GROUP_CHAT_REF(mGroupId);
        }
        setDatabaseRoot(databaseRef);
    }

    final String getDatabaseRoot() {
        return mDatabaseRoot;
    }

    final void setDatabaseRoot(final String root) {
        mDatabaseRoot = root;
//        try {
//            FirebaseDatabase.getInstance().getReference(mDatabaseRoot).keepSynced(true);
//        } catch (Exception e) {
//            MLog.e(TAG, "", e);
//        }
    }

    private void setEnableSendButton(final boolean isEnable) {

        if (isEnable && mSendButton.isEnabled() || !isEnable && !mSendButton.isEnabled())
            return; //already set

        mSendButton.setEnabled(isEnable);

        final Animation hideAnimation = AnimationUtils.loadAnimation(GroupChatActivity.this, R.anim.fab_scale_down);
        final Animation showAnimation = AnimationUtils.loadAnimation(GroupChatActivity.this, R.anim.fab_scale_up);

        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                showAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                mSendButton.startAnimation(showAnimation);
                //mSendButton.setEnabled(isEnable);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mSendButton.startAnimation(hideAnimation);
    }

    private void initPhotoHelper(Bundle savedInstanceState) {
        mPhotoUploadHelper = new PhotoUploadHelper(this, this);
        mPhotoUploadHelper.setPhotoUploadListener(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY_PHOTO_TYPE)) {
            PhotoUploadHelper.PhotoType photoType = PhotoUploadHelper.PhotoType.valueOf(savedInstanceState.getString(Constants.KEY_PHOTO_TYPE));
            mPhotoUploadHelper.setPhotoType(photoType);
            MLog.d(TAG, "initPhotoHelper: retrieved from saved instance state: " + photoType);
        }
    }

    /*private void beginDownload() {
        // Get path
        String path = "photos/" + mFileUri.getLastPathSegment();

        // Kick off download service
        Intent intent = new Intent(this, MyDownloadService.class);
        intent.setAction(MyDownloadService.ACTION_DOWNLOAD);
        intent.putExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH, path);
        startService(intent);

        // Show loading spinner
        showProgressDialog();
    }*/

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

    /*private void initDownloadReceiver() {
        mDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "downloadReceiver:onReceive:" + intent);
                hideProgressDialog();

                if (MyDownloadService.ACTION_COMPLETED.equals(intent.getAction())) {
                    String path = intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH);
                    long numBytes = intent.getLongExtra(MyDownloadService.EXTRA_BYTES_DOWNLOADED, 0);

                    // Alert success
                    showMessageDialog(getString(R.string.success), String.format(Locale.getDefault(),
                            "%d bytes downloaded from %s", numBytes, path));
                }

                if (MyDownloadService.ACTION_ERROR.equals(intent.getAction())) {
                    String path = intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH);

                    // Alert failure
                    showMessageDialog("Error", String.format(Locale.getDefault(),
                            "Failed to download from %s", path));
                }
            }
        };
    }*/

    private int mAttachPhotoMessageType;

    private void initButtons() {
        mSendButton = findViewById(R.id.sendButton);
        mAttachButton = findViewById(R.id.attachButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String text = mMessageEditText.getText().toString();
                validateBeforeSendText(text, false);
            }
        });
        mSendButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final String text = mMessageEditText.getText().toString();
                validateBeforeSendText(text, true);
                return true;
            }
        });
        mAttachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNeedsDp())
                    return;
                mAttachPhotoMessageType = FriendlyMessage.MESSAGE_TYPE_NORMAL;
                showFileOptions();
            }
        });
        mAttachButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new MessageOptionsDialogHelper().showSendOptions(GroupChatActivity.this, mAttachButton, null, new MessageOptionsDialogHelper.SendOptionsListener() {
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
            mMessagesAdapter.sendFriendlyMessage(friendlyMessage);
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
        final FriendlyMessage friendlyMessage = new FriendlyMessage(text, myUsername(), myUserid(), myDpid(), null, false, false, null, System.currentTimeMillis());
        if (!showOptions) {
            sendText(friendlyMessage);
            return true;
        }
        new MessageOptionsDialogHelper().showSendOptions(GroupChatActivity.this, mSendButton, friendlyMessage, new MessageOptionsDialogHelper.SendOptionsListener() {
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

    private PresenceHelper mPresenceHelper = new PresenceHelper();

    protected final void updateLastActiveTimestamp() {
        mPresenceHelper.updateLastActiveTimestamp();
    }

    @Override
    public void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage) {
        try {
            if (isActivityDestroyed())
                return;
            mMessageRecyclerView.scrollToPosition(mMessagesAdapter.getItemCount() - 1);
            updateLastActiveTimestamp();
        } catch (final Exception e) {
            MLog.e(TAG, "", e);
        }
        mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
    }

    @Override
    public void onFriendlyMessageFail(FriendlyMessage friendlyMessage) {
        mMessageEditText.setText(friendlyMessage.getText());
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.could_not_send_message)).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_chat_options_menu, menu);
        return true;
    }

    protected void onHomeClicked() {
        mDrawerLayout.openDrawer(GravityCompat.START);
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
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, BlocksFragment.TAG).addToBackStack(null).commit();
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
            case R.id.crash_menu:
                FirebaseCrash.logcat(Log.ERROR, TAG, "crash caused");
                causeCrash();
                return true;
            case R.id.menu_sign_out:
                signout();
                return true;
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            case R.id.full_screen_texts_menu:
                openFullScreenTextView(-1);
                return true;
            case R.id.download:
                //beginDownload();
                return true;
            case R.id.menu_pending_requests:
                onPendingRequestsClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void causeCrash() {
        throw new NullPointerException("Fake null pointer exception");
    }

    protected void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title)).setMessage(getString(R.string.invitation_message)).setCallToActionText(getString(R.string.invitation_cta)).build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    // Fetch the config to determine the allowed length of messages.
    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
                mFirebaseRemoteConfig.activateFetched();
                applyRetrievedLengthLimit();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // There has been an error fetching the config
                MLog.w(TAG, "Error fetching config: " + e.getMessage());
                applyRetrievedLengthLimit();
            }
        });
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
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                MLog.d(TAG, "Failed to send invitation.");
            }
        }

    }

    private void showPhotoReduceError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                Toast.makeText(GroupChatActivity.this, "Could not read photo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(Constants.KEY_MAX_MESSAGE_LENGTH);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
        MLog.d(TAG, "FML is: " + friendly_msg_length);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        MLog.d(TAG, "onConnectionFailed:" + connectionResult);
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

    Toolbar getToolbar() {
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

    protected int getToolbarHeight() {
        return mToolbar.getHeight();
    }

    private void setupDrawers() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        }

        @Override
        public void onPendingRequestsCleared() {
            mIsPendingRequestsAvailable = false;
        }
    };

    private void onProfilePicChangeRequest(boolean isLaunchCamera) {
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.userProfilePhoto);
        mPhotoUploadHelper.setStorageRefString(Constants.DP_STORAGE_BASE_REF(myUserid()));
        mPhotoUploadHelper.launchCamera(isLaunchCamera);
    }

    private void setupLeftDrawerContent() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView == null)
            return;
        View headerView = getLayoutInflater().inflate(R.layout.left_nav_header, navigationView, false);
        View drawerView = getLayoutInflater().inflate(R.layout.left_drawer_layout, navigationView, false);
        navigationView.addView(drawerView);
        navigationView.addHeaderView(headerView);
        mLeftDrawerHelper = new LeftDrawerHelper(this, this, mDrawerLayout, mLeftDrawerEventListener);
        mLeftDrawerHelper.setup(navigationView);
        mLeftDrawerHelper.setUserLikedUserListener(mUserLikedUserListener);

        mChatsRecyclerViewAdapter = new ChatSummariesRecyclerAdapter(this, this);
        RecyclerView recyclerView = (RecyclerView) drawerView.findViewById(R.id.drawerRecyclerView);
        recyclerView.setLayoutManager(new StickyLayoutManager(this, mChatsRecyclerViewAdapter));
        recyclerView.setAdapter(mChatsRecyclerViewAdapter);
        mChatsRecyclerViewAdapter.populateData();
    }

    protected void setupRightDrawerContent() {

        NavigationView navigationView = (NavigationView) findViewById(R.id.right_nav_view);
        if (navigationView == null) {
            return;
        }
        //View headerView = getLayoutInflater().inflate(R.layout.nav_header, navigationView, false);
        View drawerRecyclerView = getLayoutInflater().inflate(R.layout.right_drawer_layout, navigationView, false);
        final View headerView = getLayoutInflater().inflate(R.layout.right_nav_header, navigationView, false);

        FirebaseDatabase.getInstance().getReference(Constants.PUBLIC_CHATS_SUMMARY_PARENT_REF).child(mGroupId + "").
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        MLog.d(TAG, "setupRightDrawerContent() dataSnapshot: ", dataSnapshot);
                        GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                        ((TextView) headerView.findViewById(R.id.groupname)).setText(groupChatSummary.getName());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        navigationView.addHeaderView(headerView);
        navigationView.addHeaderView(drawerRecyclerView);

        RecyclerView recyclerView = (RecyclerView) drawerRecyclerView.findViewById(R.id.drawerRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        mGroupChatUsersRecyclerAdapter = new GroupChatUsersRecyclerAdapter(this, this, this, mGroupId);
        recyclerView.setAdapter(mGroupChatUsersRecyclerAdapter);
        mGroupChatUsersRecyclerAdapter.populateData();
    }

    private boolean isLeftDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    private boolean isRightDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.END);
    }

    private void closeLeftDrawer() {
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void closeRightDrawer() {
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(GravityCompat.END);
    }

    private void openRightDrawer() {
        if (mDrawerLayout != null)
            mDrawerLayout.openDrawer(GravityCompat.END);
    }

    private void openLeftDrawer() {
        if (mDrawerLayout != null)
            mDrawerLayout.openDrawer(GravityCompat.START);
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
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        removeUserPresenceFromGroup();
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
        ((FullScreenTextFragment) fragment).setFriendlyMessageContainer(this);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, FullScreenTextFragment.TAG).addToBackStack(null).commit();
    }

    private void notifyPagerAdapterDataSetChanged() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
        if (fragment == null) {
            return;
        }
        ((FullScreenTextFragment) fragment).notifyDataSetChanged();
    }

    @Override
    public FriendlyMessage getFriendlyMessage(int position) {
        return mMessagesAdapter.getItem(position);
    }

    @Override
    public String getFriendlyMessageDatabase() {
        return mDatabaseRoot;
    }

    @Override
    public int getFriendlyMessageCount() {
        return mMessagesAdapter.getItemCount();
    }

    @Override
    public void setCurrentFriendlyMessage(int position) {
        mMessageRecyclerView.scrollToPosition(position + 1);
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
            MLog.i(TAG, "onRequestPermissionsResult() requestCode: " + requestCode, " ", "grant result ", grantResults[i]);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        mPhotoUploadHelper.onPermissionsGranted(requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    private BlockedUserListener mBlockedUserListener = new BlockedUserListener() {
        @Override
        public void onUserBlocked(int userid) {
            GroupChatActivity.this.onUserBlocked(userid);
        }

        @Override
        public void onUserUnblocked(int userid) {
            GroupChatActivity.this.onUserUnblocked(userid);
        }
    };

    protected void onUserBlocked(int userid) {
    }

    protected void onUserUnblocked(int userid) {
    }

    protected boolean isPrivateChat() {
        return false;
    }

    private void initFirebaseAdapter() {
        mMessagesAdapter = new MessagesRecyclerAdapter<>(FriendlyMessage.class, R.layout.item_message, MessageViewHolder.class,
                FirebaseDatabase.getInstance().
                        getReference(mDatabaseRoot).
                        limitToLast((int) mFirebaseRemoteConfig.getLong(Constants.KEY_MAX_MESSAGE_HISTORY)));
        mMessagesAdapter.setIsPrivateChat(isPrivateChat());
        mMessagesAdapter.setDatabaseRoot(mDatabaseRoot);
        mMessagesAdapter.setActivity(this, this, (FrameLayout) findViewById(R.id.fragment_content));
        mMessagesAdapter.setAdapterPopulateHolderListener(new AdapterPopulateHolderListener() {
            @Override
            public void onViewHolderPopulated() {
                if (mProgressBar.isShown())
                    mProgressBar.setVisibility(ProgressBar.GONE);
            }
        });
        mMessagesAdapter.setMessageTextClickedListener(new MessageTextClickedListener() {
            @Override
            public void onMessageClicked(final int position) {
                openFullScreenTextView(position);
            }
        });
        mMessagesAdapter.setBlockedUserListener(mBlockedUserListener);
        mMessagesAdapter.setFriendlyMessageListener(this);
        mMessagesAdapter.setUserThumbClickedListener(this);
        mMessagesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {

                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mMessagesAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 || positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1)) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
                notifyPagerAdapterDataSetChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                notifyPagerAdapterDataSetChanged();
            }
        });
        //don't show initial indeterminate progress more than 5 seconds
        //no matter how long it takes to pull messages from firebase
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                if (mProgressBar.getVisibility() == View.VISIBLE) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        }, mFirebaseRemoteConfig.getLong(Constants.KEY_MAX_INDETERMINATE_MESSAGE_FETCH_PROGRESS));
    }

    Integer myUserid() {
        return Preferences.getInstance().getUserId();
    }

    String myDpid() {
        return Preferences.getInstance().getUser().getProfilePicUrl();
    }

    String myUsername() {
        return Preferences.getInstance().getUsername();
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
                    showBottomDialog();
                }
            }, 175);
        } else {
            showBottomDialog();
        }
    }

    private void showBottomDialog() {

        /*final BottomSheetMenuDialog dialog = new BottomSheetBuilder(this, R.style.AppTheme_BottomSheetDialog)
                .setMode(BottomSheetBuilder.MODE_LIST)
                .setMenu(R.menu.file_upload_options)
                .setItemClickListener(new BottomSheetItemClickListener() {
                    @Override
                    public void onBottomSheetItemClick(final MenuItem item) {
                        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
                        mPhotoUploadHelper.setStorageRefString(mDatabaseRoot);
                        if (item.getItemId() == R.id.menu_take_photo) {
                            mPhotoUploadHelper.launchCamera(false);
                        } else if (item.getItemId() == R.id.menu_choose_photo) {
                            mPhotoUploadHelper.launchCamera(true);
                        }
                    }
                })
                .createDialog();

        dialog.show();*/
        new BottomSheet.Builder(this).sheet(R.menu.file_upload_options).listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case R.id.menu_choose_photo:
                        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
                        mPhotoUploadHelper.setStorageRefString(mDatabaseRoot);
                        mPhotoUploadHelper.launchCamera(true);
                        break;
                    case R.id.menu_take_photo:
                        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
                        mPhotoUploadHelper.setStorageRefString(mDatabaseRoot);
                        mPhotoUploadHelper.launchCamera(false);
                        break;
                    default:
                        break;
                }
            }
        }).show();
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

            final FriendlyMessage friendlyMessage = new FriendlyMessage("", myUsername(), myUserid(), myDpid(),
                    photoUrl, isPossiblyAdultImage, isPossiblyViolentImage, null,
                    System.currentTimeMillis());
            friendlyMessage.setMessageType(mAttachPhotoMessageType);
            MLog.d(TAG, "uploadFromUri:onSuccess photoUrl: " + photoUrl, " debug possibleAdult: ", friendlyMessage.isPossibleAdultImage(), " parameter: ", isPossiblyAdultImage);
            try {
                mMessagesAdapter.sendFriendlyMessage(friendlyMessage);
            } catch (final Exception e) {
                MLog.e(TAG, "", e);
            }

        } else if (mPhotoUploadHelper.getPhotoType() == PhotoUploadHelper.PhotoType.userProfilePhoto) {

            final User user = Preferences.getInstance().getUser();
            user.setProfilePicUrl(photoUrl);
            Preferences.getInstance().saveUser(user);
            NetworkApi.saveUser(null, user, new Response.Listener<String>() {
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

        if (!TextUtils.isEmpty(myDpid()))
            return false;
        new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE).setTitleText(this.getString(R.string.display_photo_title)).setContentText(this.getString(R.string.display_photo)).setCancelText(this.getString(android.R.string.cancel)).setConfirmText(this.getString(android.R.string.ok)).showCancelButton(true).setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
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
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.error_send_photo)).show();
    }

    @Override
    public void onUserClicked(int userid, String username, String dpid, View transitionImageView) {
        closeBothDrawers();
        ScreenUtil.hideVirtualKeyboard(mMessageEditText);
        PrivateChatActivity.startPrivateChatActivity(this, userid, username, dpid, false, transitionImageView, null, null);
    }

    protected void onRemoteUserTyping(int userid, String username, String dpid) {
        if (isActivityDestroyed()) {
            return;
        }
        mUsernameTyping.setText(username);
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

    void showTypingDots() {
        showDotsParent(true);
        mDotsHandler.removeCallbacks(mDotsHideRunner);
        mDotsHandler.postDelayed(mDotsHideRunner, mFirebaseRemoteConfig.getLong(Constants.KEY_MAX_TYPING_DOTS_DISPLAY_TIME));
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
        PrivateChatActivity.startPrivateChatActivity(this, Integer.parseInt(privateChatSummary.getId()), privateChatSummary.getName(), privateChatSummary.getDpid(), false, null, mSharePhotoUri, mShareText);
        mSharePhotoUri = null;
        mShareText = null;
    }

    private void hideDotsParent() {
        if (mDotsLayoutParent.getVisibility() == View.GONE)
            return;
        mDotsLayoutParent.setVisibility(View.GONE);
    }

    private void showDotsParent(boolean isAnimate) {
        if (mDotsLayoutParent.getVisibility() == View.VISIBLE)
            return;
        mDotsLayoutParent.setVisibility(View.VISIBLE);
        if (isAnimate)
            AnimationUtil.fadeInAnimation(mDotsLayoutParent);
    }

    public static void startGroupChatActivity(Context context, long groupId, String groupName, Uri sharePhotoUri, String shareMessage) {
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

        mGroupSummaryRef = FirebaseDatabase.getInstance().getReference(Constants.PUBLIC_CHATS_SUMMARY_PARENT_REF).
                child(mGroupId + "");
        mGroupSummaryListener = mGroupSummaryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    final GroupChatSummary groupChatSummary = dataSnapshot.getValue(GroupChatSummary.class);
                    getSupportActionBar().setTitle(groupChatSummary.getName());
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

                            MLog.d(TAG, "addUserPresenceToGroup() mGroupId: ", mGroupId, " username: ", myUsername());
                            User me = Preferences.getInstance().getUser();
                            final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_REF(mGroupId)).
                                    child(myUserid() + "");
                            ref.updateChildren(me.toMap(true)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (mChatsRecyclerViewAdapter != null)
                                        mChatsRecyclerViewAdapter.removeUserFromAllGroups(myUserid(), mGroupId);
                                }
                            });
                            me.setCurrentGroupId(groupChatSummary.getId());
                            me.setCurrentGroupName(groupChatSummary.getName());
                            FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(myUserid())).updateChildren(me.toMap(true));

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
        MLog.d(TAG, "removeUserPresenceFromGroup() mGroupId: ", mGroupId, " username: ", myUsername());
        FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_REF(mGroupId)).child(myUserid() + "").removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(myUserid())).child(Constants.FIELD_CURRENT_GROUP_ID).removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(myUserid())).child(Constants.FIELD_CURRENT_GROUP_NAME).removeValue();
        FirebaseDatabase.getInstance().getReference(Constants.GROUP_CHAT_USERS_TYPING_REF(mGroupId, myUserid())).removeValue();
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

        new LogoutDialogHelper().showLogoutDialog(this, new LogoutDialogHelper.LogoutListener() {
            @Override
            public void onConfirmLogout() {
                mFirebaseAuth.signOut();
                removeUserPresenceFromGroup();
                GCMHelper.unregister(Preferences.getInstance().getUserId() + "");
                Preferences.getInstance().saveUser(null);
                startActivity(new Intent(GroupChatActivity.this, SignInActivity.class));
                finish();
            }
        });
    }

    protected BlockedUserListener getBlockedUserListener() {
        return mBlockedUserListener;
    }

    private void initRemoteConfig() {
        // Initialize Firebase Remote Config.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(false).build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(Constants.KEY_MAX_MESSAGE_HISTORY, Constants.DEFAULT_MAX_MESSAGE_HISTORY);
        defaultConfigMap.put(Constants.KEY_MAX_INDETERMINATE_MESSAGE_FETCH_PROGRESS, Constants.DEFAULT_MAX_INDETERMINATE_MESSAGE_FETCH_PROGRESS);
        defaultConfigMap.put(Constants.KEY_MAX_TYPING_DOTS_DISPLAY_TIME, Constants.DEFAULT_MAX_TYPING_DOTS_DISPLAY_TIME);
        defaultConfigMap.put(Constants.KEY_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY, Constants.DEFAULT_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY);
        defaultConfigMap.put(Constants.KEY_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME, Constants.DEFAULT_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME);
        defaultConfigMap.put(Constants.KEY_MAX_MESSAGE_LENGTH, Constants.DEFAULT_MAX_MESSAGE_LENGTH);
        defaultConfigMap.put(Constants.KEY_MAX_PERISCOPABLE_LIKES_PER_ITEM, Constants.DEFAULT_MAX_PERISCOPABLE_LIKES_PER_ITEM);
        defaultConfigMap.put(Constants.KEY_ALLOW_DELETE_OTHER_MESSAGES, Constants.DEFAULT_ALLOW_DELETE_OTHER_MESSAGES);
        defaultConfigMap.put(Constants.KEY_DO_SHORTEN_IMAGE_URLS, Constants.DEFAULT_DO_SHORTEN_IMAGE_URLS);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
    }

    private UserLikedUserListener mUserLikedUserListener = new UserLikedUserListener() {
        @Override
        public void onMyLikersClicked() {
            Fragment fragment = new UserLikedUserFragment();
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, UserLikedUserFragment.TAG).addToBackStack(null).commit();
        }
    };

    private boolean mShownSendOptionsProtips;

    private void showSendOptionsTooltip(View anchor) {
//        if (Preferences.getInstance().hasShownToolbarProfileTooltip())
//            return;
//        Preferences.getInstance().setShownToolbarProfileTooltip(true);
        if (mShownSendOptionsProtips) {
            return;
        }
        mShownSendOptionsProtips = true;
        final Tooltip tooltip = new Tooltip.Builder(anchor, R.style.drawer_tooltip_non_cancellable)
                .setText(getString(R.string.send_option_protips))
                .show();
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
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, RequestsFragment.TAG).addToBackStack(null).commit();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu == null) return false;
        if (!mIsPendingRequestsAvailable) {
            if (menu.findItem(R.id.menu_pending_requests) != null)
                menu.removeItem(R.id.menu_pending_requests);
        } else {
            if (menu.findItem(R.id.menu_pending_requests) == null)
                menu.add(0, R.id.menu_pending_requests, 0, getString(R.string.menu_option_pending_requests));
        }
        if (mMessagesAdapter != null && mMessagesAdapter.getNumBlockedUsers() > 0) {
            if (menu.findItem(R.id.menu_manage_blocks) == null) {
                menu.add(0, R.id.menu_manage_blocks, 1, getString(R.string.manage_blocks));
            }
        } else {
            if (menu.findItem(R.id.menu_manage_blocks) != null)
                menu.removeItem(R.id.menu_manage_blocks);
        }
        return super.onMenuOpened(featureId, menu);
    }
}
