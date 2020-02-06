package com.instachat.android.app.activity;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
//import androidx.databinding.ViewDataBinding;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.MessageOptionsDialogHelper;
import com.instachat.android.app.activity.group.DeleteAccountDialogHelper;
import com.instachat.android.app.activity.group.GroupChatActivity;
import com.instachat.android.app.activity.group.LogoutDialogHelper;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.app.adapter.ChatSummariesRecyclerAdapter;
import com.instachat.android.app.adapter.ChatsItemClickedListener;
import com.instachat.android.app.adapter.FriendlyMessageListener;
import com.instachat.android.app.adapter.MessageTextClickedListener;
import com.instachat.android.app.adapter.MessagesDialogHelper;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.app.adapter.UserPresenceManager;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.bans.BanHelper;
import com.instachat.android.app.blocks.BlockUserDialogHelper;
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
import com.instachat.android.databinding.LeftDrawerLayoutBinding;
import com.instachat.android.databinding.LeftNavHeaderBinding;
import com.instachat.android.gcm.GCMHelper;
import com.instachat.android.messaging.InstachatMessagingService;
import com.instachat.android.messaging.NotificationHelper;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.FontUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;
import com.tooltip.Tooltip;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.databinding.ViewDataBinding;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import pub.devrel.easypermissions.EasyPermissions;

public abstract class AbstractChatActivity<T extends ViewDataBinding, V extends AbstractChatViewModel> extends BaseActivity<T, V>
        implements UploadListener, AttachPhotoOptionsDialogHelper.PhotoOptionsListener, AbstractChatNavigator,
        ChatsItemClickedListener, FriendlyMessageListener, FriendlyMessageContainer, MessageTextClickedListener, EasyPermissions.PermissionCallbacks,
        UserClickedListener {

    private static final String TAG = "ChatActivity";

    protected final int REQUEST_INVITE = 1;

    @Inject
    protected SchedulerProvider schedulerProvider;

    @Inject
    protected FirebaseRemoteConfig firebaseRemoteConfig;

    @Inject
    protected FirebaseDatabase firebaseDatabase;

    @Inject
    protected FirebaseAuth firebaseAuth;

    @Inject
    protected MessagesRecyclerAdapterHelper map;

    @Inject
    protected GCMHelper gcmHelper;

    @Inject
    protected BanHelper banHelper;

    @Inject
    protected NetworkApi networkApi;

    @Inject
    protected LogoutDialogHelper logoutDialogHelper;

    @Inject
    protected DeleteAccountDialogHelper deleteAccountDialogHelper;

    protected ChatSummariesRecyclerAdapter chatSummariesRecyclerAdapter;

    @Inject
    protected UserPresenceManager userPresenceManager;

    @Inject
    protected AdsHelper adsHelper;

    @Inject
    protected PresenceHelper presenceHelper;

    @Inject
    protected LinearLayoutManager linearLayoutManager;

    protected Uri mSharePhotoUri;
    protected String mShareText;
    protected ExternalSendIntentConsumer mExternalSendIntentConsumer;
    protected ProgressDialog mPercentageProgressDialog;
    protected int mAttachPhotoMessageType;
    protected PhotoUploadHelper mPhotoUploadHelper;
    protected LeftDrawerHelper mLeftDrawerHelper;
    protected MessagesRecyclerAdapter messagesAdapter;
    protected Toolbar mToolbar;
    protected EditText mMessageEditText;
    protected boolean mIsPendingRequestsAvailable;
    protected DrawerLayout drawerLayout;
    protected RecyclerView messageRecyclerView;
    private FrameLayout entireScreenView;
    protected View dotsLayout;
    protected View sendButton;
    protected LeftNavHeaderBinding leftNavHeaderBinding;
    protected LeftDrawerLayoutBinding leftDrawerLayoutBinding;
    protected FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdsHelper.init(this);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        NotificationHelper.createNotificationChannels(this);
        linearLayoutManager.setStackFromEnd(true);

        //cant use data binding here, but will fix later
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        drawerLayout = findViewById(R.id.drawer_layout);
        dotsLayout = findViewById(R.id.dotsLayout);
        sendButton = findViewById(R.id.sendButton);

        getViewModel().setFirebaseAnalytics(firebaseAnalytics);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendButton.setEnabled(mMessageEditText.getText().toString().trim().length() > 0);
        if (mExternalSendIntentConsumer != null)
            mExternalSendIntentConsumer.consumeIntent(getIntent());
        if (chatSummariesRecyclerAdapter != null)
            chatSummariesRecyclerAdapter.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatSummariesRecyclerAdapter != null)
            chatSummariesRecyclerAdapter.pause();
    }

    protected void showProgressDialog() {
        if (mPercentageProgressDialog == null) {
            mPercentageProgressDialog = new ProgressDialog(this);
            mPercentageProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mPercentageProgressDialog.setIndeterminate(false);
            mPercentageProgressDialog.setProgressNumberFormat("%1dk / %2dk");
        }
        mPercentageProgressDialog.show();
    }

    protected void hideProgressDialog() {
        if (mPercentageProgressDialog != null && mPercentageProgressDialog.isShowing()) {
            mPercentageProgressDialog.dismiss();
        }
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
        if (mPercentageProgressDialog != null) {
            try {
                mPercentageProgressDialog.setMax(max);
                mPercentageProgressDialog.setProgress(current);
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

        if (mPhotoUploadHelper.getPhotoType() != PhotoUploadHelper.PhotoType.userProfilePhoto) {

            final FriendlyMessage friendlyMessage = new FriendlyMessage("", getViewModel().myUsername(), getViewModel().myUserid(), getViewModel().myDpid(),
                    photoUrl, isPossiblyAdultImage, isPossiblyViolentImage, null, System.currentTimeMillis());
            friendlyMessage.setMT(mAttachPhotoMessageType);
            MLog.d(TAG, "uploadFromUri:onSuccess photoUrl: " + photoUrl, " debug possibleAdult: ", friendlyMessage
                    .isPossibleAdultImage(), " parameter: ", isPossiblyAdultImage);
            try {
                getViewModel().sendText(friendlyMessage);
            } catch (final Exception e) {
                MLog.e(TAG, "", e);
            }

        } else {
            getViewModel().saveUserPhoto(photoUrl);
        }
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

    private void showPhotoReduceError() {
        add(Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Toast.makeText(AbstractChatActivity.this, "Could not read photo", Toast.LENGTH_SHORT).show();
                return false;
            }
        }).subscribeOn(schedulerProvider.ui()).observeOn(schedulerProvider.ui()).subscribe());
    }

    protected void initPhotoHelper(Bundle savedInstanceState) {
        mPhotoUploadHelper = new PhotoUploadHelper(this, this);
        mPhotoUploadHelper.setStorageRefString(getViewModel().getDatabaseRoot());
        mPhotoUploadHelper.setPhotoUploadListener(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY_PHOTO_TYPE)) {
            PhotoUploadHelper.PhotoType photoType = PhotoUploadHelper.PhotoType.valueOf(savedInstanceState.getString
                    (Constants.KEY_PHOTO_TYPE));
            mPhotoUploadHelper.setPhotoType(photoType);
            MLog.d(TAG, "initPhotoHelper: retrieved from saved instance state: " + photoType);
        }
    }

    protected void checkIncomingShareIntent() {
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_SHARE_PHOTO_URI)) {
            mPhotoUploadHelper.setStorageRefString(getViewModel().getDatabaseRoot());
            mPhotoUploadHelper.consumeExternallySharedPhoto((Uri) getIntent().getParcelableExtra(Constants
                    .KEY_SHARE_PHOTO_URI));
            getIntent().removeExtra(Constants.KEY_SHARE_PHOTO_URI);
        }
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_SHARE_MESSAGE)) {
            mMessageEditText.setText(getIntent().getStringExtra(Constants.KEY_SHARE_MESSAGE));
            getIntent().removeExtra(Constants.KEY_SHARE_MESSAGE);
        }
    }

    protected void initExternalSendIntentConsumer(final DrawerLayout drawerLayout) {
        mExternalSendIntentConsumer = new ExternalSendIntentConsumer(this);
        mExternalSendIntentConsumer.setListener(new ExternalSendIntentConsumer.ExternalSendIntentListener() {
            @Override
            public void onHandleSendImage(final Uri imageUri) {
                drawerLayout.openDrawer(GravityCompat.START);
                mSharePhotoUri = imageUri;
            }

            @Override
            public void onHandleSendText(final String text) {
                drawerLayout.openDrawer(GravityCompat.START);
                mShareText = text;
            }
        });
    }

    protected void cancelNotificationsDueToEntry() {
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(InstachatMessagingService.NOTIFICATION_ID_PENDING_REQUESTS);
        notificationManager.cancel(InstachatMessagingService.NOTIFICATION_ID_FRIEND_JUMPED_IN);
    }

    @Override
    protected void onDestroy() {
        if (mExternalSendIntentConsumer != null)
            mExternalSendIntentConsumer.cleanup();
        if (mPhotoUploadHelper != null)
            mPhotoUploadHelper.cleanup();
        if (messagesAdapter != null)
            messagesAdapter.cleanup();
        if (mLeftDrawerHelper != null)
            mLeftDrawerHelper.cleanup();
        if (chatSummariesRecyclerAdapter != null)
            chatSummariesRecyclerAdapter.cleanup();
        mDotsHandler.removeCallbacks(mDotsHideRunner);
        userPresenceManager.cleanup();
        presenceHelper.cleanup();
        super.onDestroy();
    }

    private void setEnableSendButton(final boolean isEnable,
                                     final View sendButton) {

        if (isEnable && sendButton.isEnabled() || !isEnable && !sendButton.isEnabled())
            return; //already set

        sendButton.setEnabled(isEnable);

        final Animation hideAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_scale_down);
        final Animation showAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_scale_up);

        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                sendButton.startAnimation(showAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        sendButton.startAnimation(hideAnimation);
    }

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
        add(Observable.timer(2000, TimeUnit.MILLISECONDS)
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

    public void initMessageEditText(final View sendButton, ViewGroup messageEditTextParent) {
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
                    setEnableSendButton(true, sendButton);
                    try {
                        if (UserPreferences.getInstance().getUserId() != 7) {
                            getViewModel().onMeTyping();
                        }
                    }catch (Throwable t){}
                    showSendOptionsTooltip(sendButton);
                } else {
                    setEnableSendButton(false, sendButton);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
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
                                return getViewModel().onCommitContent(
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

    public void showSendOptions(FriendlyMessage friendlyMessage, View sendButton) {
        new MessageOptionsDialogHelper().showSendOptions(this, sendButton, friendlyMessage, new
                MessageOptionsDialogHelper.SendOptionsListener() {
                    @Override
                    public void onSendNormalRequested(FriendlyMessage friendlyMessage) {
                        friendlyMessage.setMT(FriendlyMessage.MESSAGE_TYPE_NORMAL);
                        getViewModel().sendText(friendlyMessage);
                    }

                    @Override
                    public void onSendOneTimeRequested(FriendlyMessage friendlyMessage) {
                        friendlyMessage.setMT(FriendlyMessage.MESSAGE_TYPE_ONE_TIME);
                        getViewModel().sendText(friendlyMessage);
                    }
                });
    }

    @Override
    public void clearTextField() {
        try {
            mMessageEditText.setText("");//fast double taps on send can cause 2x sends!
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    private void showFileOptions() {

        if (getViewModel().isBanned()) {
            showYouHaveBeenBanned();
            return;
        }

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
    public void showNeedPhotoDialog() {
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
    }

    public void initButtons(final View sendButton, final View attachButton) {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String text = mMessageEditText.getText().toString();
                getViewModel().sendText(text, false);
            }
        });
        sendButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final String text = mMessageEditText.getText().toString();
                getViewModel().sendText(text, true);
                return true;
            }
        });
        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (StringUtil.isEmpty(getViewModel().myDpid())) {
                    showNeedPhotoDialog();
                    return;
                }
                mAttachPhotoMessageType = FriendlyMessage.MESSAGE_TYPE_NORMAL;
                showFileOptions();
            }
        });
        attachButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new MessageOptionsDialogHelper().showSendOptions(AbstractChatActivity.this, attachButton, null, new
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

    public boolean isLeftDrawerOpen() {
        return drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public boolean isRightDrawerOpen() {
        return drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END);
    }

    public void closeLeftDrawer() {
        if (drawerLayout != null)
            drawerLayout.closeDrawer(GravityCompat.START);
    }

    public void closeRightDrawer() {
        if (drawerLayout != null)
            drawerLayout.closeDrawer(GravityCompat.END);
    }

    public void openRightDrawer() {
        if (drawerLayout != null)
            drawerLayout.openDrawer(GravityCompat.END);
    }

    public void openLeftDrawer() {
        if (drawerLayout != null)
            drawerLayout.openDrawer(GravityCompat.START);
    }

    public boolean closeBothDrawers() {
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

    public void setupDrawers(NavigationView navigationView) {
        setupLeftDrawerContent(navigationView);
        setupRightDrawerContent();
    }

    private UserLikedUserListener mUserLikedUserListener = new UserLikedUserListener() {
        @Override
        public void onMyLikersClicked() {
            Fragment fragment = new UserLikedUserFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.KEY_USERID, getViewModel().myUserid());
            bundle.putString(Constants.KEY_USERNAME, getViewModel().myUsername());
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R
                    .anim.slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, UserLikedUserFragment
                    .TAG).addToBackStack(null).commit();
        }
    };

    public void setupLeftDrawerContent(NavigationView navigationView) {
        leftNavHeaderBinding = LeftNavHeaderBinding.inflate(getLayoutInflater(), navigationView, false);
        leftDrawerLayoutBinding = LeftDrawerLayoutBinding.inflate(getLayoutInflater(), navigationView, false);
        leftNavHeaderBinding.setViewModel(getViewModel());
        leftDrawerLayoutBinding.setViewModel(getViewModel());
        navigationView.addView(leftDrawerLayoutBinding.getRoot());
        navigationView.addHeaderView(leftNavHeaderBinding.getRoot());
        mLeftDrawerHelper = new LeftDrawerHelper(this, getViewModel(),this, this, drawerLayout, mLeftDrawerEventListener);
        mLeftDrawerHelper.setup(leftDrawerLayoutBinding, leftNavHeaderBinding);
        mLeftDrawerHelper.setUserLikedUserListener(mUserLikedUserListener);
        chatSummariesRecyclerAdapter = new ChatSummariesRecyclerAdapter(userPresenceManager);
        chatSummariesRecyclerAdapter.setup(AbstractChatActivity.this, AbstractChatActivity.this, true);
        leftDrawerLayoutBinding.drawerRecyclerView.setLayoutManager(new StickyLayoutManager(AbstractChatActivity.this, chatSummariesRecyclerAdapter));
        leftDrawerLayoutBinding.drawerRecyclerView.setAdapter(chatSummariesRecyclerAdapter);

        add(Observable.timer(2000, TimeUnit.MILLISECONDS)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {

                        chatSummariesRecyclerAdapter.populateData();
                        listenForUsersInGroup();
                    }
                }).subscribe());
    }

    public abstract void listenForUsersInGroup();

    public void setupRightDrawerContent() {

    }

    @Override
    public void onGroupChatClicked(GroupChatSummary groupChatSummary) {
        closeBothDrawers();
        showGroupChatActivity(groupChatSummary.getId(), groupChatSummary.getName(), mSharePhotoUri, mShareText);
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

    @Override
    public void showGroupChatActivity(long groupId, String groupName, Uri sharePhotoUri,
                                              String shareMessage) {
        Intent intent = newIntent(this, groupId, groupName);
        if (sharePhotoUri != null)
            intent.putExtra(Constants.KEY_SHARE_PHOTO_URI, sharePhotoUri);
        if (shareMessage != null)
            intent.putExtra(Constants.KEY_SHARE_MESSAGE, shareMessage);
        startActivity(intent);
    }

    public static Intent newIntent(Context context, long groupId, String groupName) {
        Intent intent = new Intent(context, GroupChatActivity.class);
        intent.putExtra(Constants.KEY_GROUPID, groupId);
        intent.putExtra(Constants.KEY_GROUP_NAME, groupName);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private LeftDrawerEventListener mLeftDrawerEventListener = new LeftDrawerEventListener() {
        @Override
        public void onProfilePicChangeRequest(boolean isLaunchCamera) {
            changePhoto(isLaunchCamera);
        }

        @Override
        public void onPendingRequestsClicked() {
            showPendingRequests();
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

    private void changePhoto(boolean isLaunchCamera) {
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.userProfilePhoto);
        mPhotoUploadHelper.setStorageRefString(Constants.DP_STORAGE_BASE_REF(getViewModel().myUserid()));
        mPhotoUploadHelper.launchCamera(isLaunchCamera);
    }

    public void showPendingRequests() {
        closeLeftDrawer();
        Fragment fragment = new RequestsFragment();
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim
                .slide_up, R.anim.slide_down).replace(R.id.fragment_content, fragment, RequestsFragment.TAG)
                .addToBackStack(null).commit();
    }

    public void showSignIn() {
        startActivity(new Intent(this, SignInActivity.class));
        finish();
    }

    @Override
    public void setMaxMessageLength(int maxMessageLength) {
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxMessageLength)});
    }

    public void showFullScreenTextView(final int startingPos) {
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

    public void showAppInviteActivity() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                //.setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                //.setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
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
        firebaseAnalytics.logEvent(Events.MESSAGE_FAILED, null);
    }

    public void setupToolbarTitle(Toolbar toolbar) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                FontUtil.setTextViewFont((TextView) view);
                break;
            }
        }
    }

    @Override
    public void onFriendlyMessageRemoved() {
        getViewModel().checkMessageSortOrder();
    }

    @Override
    public void onPhotoGallery() {
        mPhotoUploadHelper.setStorageRefString(getViewModel().getDatabaseRoot());
        mPhotoUploadHelper.setPhotoType(getRoomPhotoType());
        mPhotoUploadHelper.launchCamera(false);
    }

    @Override
    public void onPhotoTake() {
        mPhotoUploadHelper.setStorageRefString(getViewModel().getDatabaseRoot());
        mPhotoUploadHelper.setPhotoType(getRoomPhotoType());
        mPhotoUploadHelper.launchCamera(true);
    }

    protected abstract PhotoUploadHelper.PhotoType getRoomPhotoType();

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
        return getViewModel().getDatabaseRoot();
    }

    /**
     * Implementation of {@link FriendlyMessageContainer}
     */
    @Override
    public int getFriendlyMessageCount() {
        return messagesAdapter.getItemCount();
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        mPhotoUploadHelper.onPermissionsGranted(requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

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
                payload.putString("username", getViewModel().myUsername());
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);
                // Sending failed or it was canceled, show failure message to the user
                MLog.d(TAG, "Failed to send invitation.");
            }
        }

    }

    private void notifyPagerAdapterDataSetChanged() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
        if (fragment == null) {
            return;
        }
        ((FullScreenTextFragment) fragment).notifyDataSetChanged();
    }

    @Override
    public void onMessageClicked(int position) {
        showFullScreenTextView(position);
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            x = motionEvent.getX();
            y = motionEvent.getY();
            return false;
        }
    };
    private float x, y;

    @Override
    public void onMessageLongClicked(final FriendlyMessage friendlyMessage) {
        if (TextUtils.isEmpty(friendlyMessage.getName())) {
            return;
        }
        final View tempAnchorView = new View(this);
        tempAnchorView.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(10, 10);
        params.leftMargin = (int) x;
        params.topMargin = (int) y;
        entireScreenView.addView(tempAnchorView, params);
        entireScreenView.post(new Runnable() {
            @Override
            public void run() {
                showMessageOptions(tempAnchorView, friendlyMessage);
            }
        });
    }

    private void showMessageOptions(final View tempAnchorView, FriendlyMessage friendlyMessage) {
        new MessageOptionsDialogHelper().showMessageOptions(this, tempAnchorView, friendlyMessage, new MessageOptionsDialogHelper.MessageOptionsListener() {

            @Override
            public void onMessageOptionsDismissed() {
                entireScreenView.removeView(tempAnchorView);
            }

            @Override
            public void onCopyTextRequested(FriendlyMessage friendlyMessage) {
                final ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setText(friendlyMessage.getText());
                Toast.makeText(AbstractChatActivity.this, R.string.message_copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteMessageRequested(final FriendlyMessage friendlyMessage) {
                MLog.d(TAG, " msg.getImageUrl(): " + friendlyMessage.getImageUrl() + " " + friendlyMessage.getImageId());
                new MessagesDialogHelper().showDeleteMessageDialog(AbstractChatActivity.this,
                        friendlyMessage, getViewModel());
            }

            @Override
            public void onBlockPersonRequested(final FriendlyMessage friendlyMessage) {
                new BlockUserDialogHelper(FirebaseDatabase.getInstance()).showBlockUserQuestionDialog(AbstractChatActivity.this,
                        friendlyMessage.getUserid(),
                        friendlyMessage.getName(),
                        friendlyMessage.getDpid(),
                        getViewModel().getBlockedUserListener());
            }

            @Override
            public void onReportPersonRequested(FriendlyMessage friendlyMessage) {
                new ReportUserDialogHelper().showReportUserQuestionDialog(AbstractChatActivity.this,
                        friendlyMessage.getUserid(),
                        friendlyMessage.getName(),
                        friendlyMessage.getDpid());
            }

            @Override
            public void onRemoveCommentsClicked(FriendlyMessage friendlyMessage) {
                getViewModel().removeMessages(friendlyMessage);
            }

            @Override
            public void onBan5Minutes(FriendlyMessage friendlyMessage) {
                ban(friendlyMessage,5);
            }

            @Override
            public void onBan15Minutes(FriendlyMessage friendlyMessage) {
                ban(friendlyMessage,15);
            }

            @Override
            public void onBan2Days(FriendlyMessage friendlyMessage) {
                ban(friendlyMessage,60*24*2);
            }
        });
    }

    private void ban(FriendlyMessage friendlyMessage, int durationMinutes) {
        if (!canBan(friendlyMessage.getUserid())) {
            return;
        }
        banHelper.ban(friendlyMessage, durationMinutes);
    }

    private boolean canBan(int userid) {
//        if (userid == Constants.SUPER_ADMIN_1 || userid == Constants.SUPER_ADMIN_2) {
//            Toast.makeText(this,"Sorry, you cannot ban this member.", Toast.LENGTH_SHORT).show();
//            return false;
//        }
        return true;
    }

    protected void initFirebaseAdapter(FrameLayout frameLayout, final RecyclerView messageRecyclerView, UserClickedListener userClickedListener,
                                       final LinearLayoutManager linearLayoutManager) {
        entireScreenView = frameLayout;
        entireScreenView.setOnTouchListener(mOnTouchListener);
        messagesAdapter = getViewModel().createMessagesAdapter(firebaseRemoteConfig, map);
        messagesAdapter.setActivity(this, this);
        messagesAdapter.setMessageTextClickedListener(this);
        messagesAdapter.setFriendlyMessageListener(this);
        messagesAdapter.setUserThumbClickedListener(userClickedListener);
        messagesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {

                int vis = linearLayoutManager.findLastVisibleItemPosition();
//                MLog.d(TAG, "scroll debug: vis: " + vis + " text: " + messagesAdapter.peekLastMessage()
//                        + " positionStart: " + positionStart);
                if (vis == -1 || (vis+3) >= positionStart) {
                    //MLog.d(TAG, "B kevin scroll: " + (positionStart) + " text: " + messagesAdapter.peekLastMessage());
                    messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
                }
                notifyPagerAdapterDataSetChanged();
                if (itemCount > 0) {
                    MLog.d(TAG,"sort_tag check sort order");
                    getViewModel().checkMessageSortOrder();
                }
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                notifyPagerAdapterDataSetChanged();
            }
        });
    }

    @Override
    public void onUserClicked(int userid, String username, String dpid, View transitionImageView) {
        closeBothDrawers();
        ScreenUtil.hideVirtualKeyboard(mMessageEditText);
        PrivateChatActivity.startPrivateChatActivity(this, userid, username, dpid, false, transitionImageView, null,
                null);
    }

    protected void setupToolbar() {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);
        setupToolbarTitle(mToolbar);
    }

    protected Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public void setCurrentFriendlyMessage(int position) {
        if (position >= 0 && position < messagesAdapter.getItemCount()) {
            messageRecyclerView.scrollToPosition(position);
        } else {
            messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount()-1);
        }
    }

    private Handler mDotsHandler = new Handler();
    private Runnable mDotsHideRunner = new Runnable() {
        @Override
        public void run() {
            hideDotsLayout();
        }
    };

    @Override
    public void showTypingDots() {
        showDotsParent(true);
        mDotsHandler.removeCallbacks(mDotsHideRunner);
        mDotsHandler.postDelayed(mDotsHideRunner, firebaseRemoteConfig.getLong(Constants
                .KEY_MAX_TYPING_DOTS_DISPLAY_TIME));
    }

    private void hideDotsLayout() {
        if (dotsLayout.getVisibility() == View.GONE)
            return;
        dotsLayout.setVisibility(View.GONE);
    }

    private void showDotsParent(boolean isAnimate) {
        if (dotsLayout.getVisibility() == View.VISIBLE)
            return;
        dotsLayout.setVisibility(View.VISIBLE);
        if (isAnimate)
            AnimationUtil.fadeInAnimation(dotsLayout);
    }

    @Override
    public void showErrorToast(@NonNull String extra) {
        try {
            Toast.makeText(this, getString(R.string.general_api_error) + " " + extra, Toast
                    .LENGTH_SHORT).show();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    @Override
    public void showUsernameExistsDialog(@NonNull String badUsername) {
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.username_exists) + " " + badUsername).show();
    }

    @Override
    public void showProfileUpdatedDialog() {
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE).setTitleText(getString(R.string.your_profile_has_been_updated_title)).setContentText(getString(R.string.your_profile_has_been_updated_msg)).show();
        leftNavHeaderBinding.saveUsername.setVisibility(View.GONE);
    }

    @Override
    public void showYouHaveBeenBanned() {
        Toast.makeText(this,R.string.you_have_been_banned, Toast.LENGTH_SHORT).show();
    }

    private Toast slowDownToast;
    @Override
    public void showSlowDown() {
        if (slowDownToast != null) {
            slowDownToast.cancel();
        }
        slowDownToast = Toast.makeText(this,R.string.slow_down, Toast.LENGTH_SHORT);
        slowDownToast.show();
    }
}
