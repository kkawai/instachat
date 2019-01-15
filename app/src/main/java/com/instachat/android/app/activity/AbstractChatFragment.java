package com.instachat.android.app.activity;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.ViewDataBinding;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.view.inputmethod.EditorInfoCompat;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.MessageOptionsDialogHelper;
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
import com.instachat.android.app.ui.base.BaseFragment;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.GroupChatSummary;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.gcm.GCMHelper;
import com.instachat.android.messaging.InstachatMessagingService;
import com.instachat.android.messaging.NotificationHelper;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.FontUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.rx.SchedulerProvider;
import com.tooltip.Tooltip;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import pub.devrel.easypermissions.EasyPermissions;

import static android.content.Context.NOTIFICATION_SERVICE;

public abstract class AbstractChatFragment<T extends ViewDataBinding, V extends AbstractChatViewModel> extends BaseFragment<T, V>
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
    protected RecyclerView messageRecyclerView;
    private FrameLayout entireScreenView;
    protected View dotsLayout;
    protected View sendButton;
    protected FirebaseAnalytics firebaseAnalytics;


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
        NotificationHelper.createNotificationChannels(getActivity());
        linearLayoutManager.setStackFromEnd(true);

        //cant use data binding here, but will fix later
        messageRecyclerView = getView().findViewById(R.id.messageRecyclerView);
        dotsLayout = getView().findViewById(R.id.dotsLayout);
        sendButton = getView().findViewById(R.id.sendButton);

        getViewModel().setFirebaseAnalytics(firebaseAnalytics);
    }

    @Override
    public void onResume() {
        super.onResume();
        sendButton.setEnabled(mMessageEditText.getText().toString().trim().length() > 0);
        if (mExternalSendIntentConsumer != null)
            mExternalSendIntentConsumer.consumeIntent(getActivity().getIntent());
        if (chatSummariesRecyclerAdapter != null)
            chatSummariesRecyclerAdapter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (chatSummariesRecyclerAdapter != null)
            chatSummariesRecyclerAdapter.pause();
    }

    protected void showProgressDialog() {
        if (mPercentageProgressDialog == null) {
            mPercentageProgressDialog = new ProgressDialog(getActivity());
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

        if (mPhotoUploadHelper.getPhotoType() == PhotoUploadHelper.PhotoType.chatRoomPhoto) {

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

        } else if (mPhotoUploadHelper.getPhotoType() == PhotoUploadHelper.PhotoType.userProfilePhoto) {
            getViewModel().saveUserPhoto(photoUrl);
        }
    }

    @Override
    public void onPhotoUploadError(Exception exception) {
        MLog.i(TAG, "onPhotoUploadError() ", exception);
        if (isActivityDestroyed())
            return;
        hideProgressDialog();
        new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.error_send_photo))
                .show();
    }

    private void showPhotoReduceError() {
        getBaseActivity().add(Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Toast.makeText(getActivity(), "Could not read photo", Toast.LENGTH_SHORT).show();
                return false;
            }
        }).subscribeOn(schedulerProvider.ui()).observeOn(schedulerProvider.ui()).subscribe());
    }

    protected void initPhotoHelper(Bundle savedInstanceState) {
        mPhotoUploadHelper = new PhotoUploadHelper(getBaseActivity(), getBaseActivity());
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
        if (getActivity().getIntent() != null && getActivity().getIntent().hasExtra(Constants.KEY_SHARE_PHOTO_URI)) {
            mPhotoUploadHelper.setStorageRefString(getViewModel().getDatabaseRoot());
            mPhotoUploadHelper.consumeExternallySharedPhoto((Uri) getActivity().getIntent().getParcelableExtra(Constants
                    .KEY_SHARE_PHOTO_URI));
            getActivity().getIntent().removeExtra(Constants.KEY_SHARE_PHOTO_URI);
        }
        if (getActivity().getIntent() != null && getActivity().getIntent().hasExtra(Constants.KEY_SHARE_MESSAGE)) {
            mMessageEditText.setText(getActivity().getIntent().getStringExtra(Constants.KEY_SHARE_MESSAGE));
            getActivity().getIntent().removeExtra(Constants.KEY_SHARE_MESSAGE);
        }
    }

    protected void initExternalSendIntentConsumer(final DrawerLayout drawerLayout) {
        mExternalSendIntentConsumer = new ExternalSendIntentConsumer(getActivity());
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
        final NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(InstachatMessagingService.NOTIFICATION_ID_PENDING_REQUESTS);
        notificationManager.cancel(InstachatMessagingService.NOTIFICATION_ID_FRIEND_JUMPED_IN);
    }

    @Override
    public void onDestroy() {
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

        final Animation hideAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_scale_down);
        final Animation showAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_scale_up);

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
        getBaseActivity().add(Observable.timer(2000, TimeUnit.MILLISECONDS)
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
                    getViewModel().onMeTyping();
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
        AppCompatEditText editText = new AppCompatEditText(getActivity()) {
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
        new MessageOptionsDialogHelper().showSendOptions(getActivity(), sendButton, friendlyMessage, new
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
            getBaseActivity().add(Observable.timer(175, TimeUnit.MILLISECONDS)
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
        new AttachPhotoOptionsDialogHelper(getActivity(), this).showBottomDialog();
    }

    @Override
    public void showNeedPhotoDialog() {
        new SweetAlertDialog(getActivity(), SweetAlertDialog.NORMAL_TYPE).setTitleText(this.getString(R.string
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
                new MessageOptionsDialogHelper().showSendOptions(getActivity(), attachButton, null, new
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

    private AbstractChatActivity getMainActivity() {
        return (AbstractChatActivity)getBaseActivity();
    }

    public boolean isLeftDrawerOpen() {
        return getMainActivity().isLeftDrawerOpen();
    }

    public boolean isRightDrawerOpen() {
        return getMainActivity().isRightDrawerOpen();
    }

    public void closeLeftDrawer() {
        getMainActivity().closeLeftDrawer();
    }

    public void closeRightDrawer() {
        getMainActivity().closeRightDrawer();
    }

    public void openRightDrawer() {
        getMainActivity().openRightDrawer();
    }

    public void openLeftDrawer() {
        getMainActivity().openLeftDrawer();
    }

    public boolean closeBothDrawers() {
        return getMainActivity().closeBothDrawers();
    }

    public abstract void listenForUsersInGroup();

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
        PrivateChatActivity.startPrivateChatActivity(getActivity(), Integer.parseInt(privateChatSummary.getId()),
                privateChatSummary.getName(), privateChatSummary.getDpid(), false, null, mSharePhotoUri, mShareText);
        mSharePhotoUri = null;
        mShareText = null;
    }

    @Override
    public void showGroupChatActivity(long groupId, String groupName, Uri sharePhotoUri,
                                              String shareMessage) {
        Intent intent = newIntent(getActivity(), groupId, groupName);
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

    public void showSignIn() {
        startActivity(new Intent(getActivity(), SignInActivity.class));
        getActivity().finish();
    }

    @Override
    public void setMaxMessageLength(int maxMessageLength) {
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxMessageLength)});
    }

    public void showFullScreenTextView(final int startingPos) {
        closeBothDrawers();
        ScreenUtil.hideVirtualKeyboard(mMessageEditText);
        Fragment fragment = getFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
        if (fragment != null) {
            return;
        }
        fragment = new FullScreenTextFragment();
        final Bundle args = new Bundle();
        args.putInt(Constants.KEY_STARTING_POS, startingPos);
        fragment.setArguments(args);
        getFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim
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
        new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string
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
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
        mPhotoUploadHelper.launchCamera(false);
    }

    @Override
    public void onPhotoTake() {
        mPhotoUploadHelper.setStorageRefString(getViewModel().getDatabaseRoot());
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
        mPhotoUploadHelper.launchCamera(true);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MLog.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        mPhotoUploadHelper.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INVITE) {
            if (resultCode == Activity.RESULT_OK) {
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
        Fragment fragment = getFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
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
        final View tempAnchorView = new View(getActivity());
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
        new MessageOptionsDialogHelper().showMessageOptions(getActivity(), tempAnchorView, friendlyMessage, new MessageOptionsDialogHelper.MessageOptionsListener() {

            @Override
            public void onMessageOptionsDismissed() {
                entireScreenView.removeView(tempAnchorView);
            }

            @Override
            public void onCopyTextRequested(FriendlyMessage friendlyMessage) {
                final ClipboardManager cm = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setText(friendlyMessage.getText());
                Toast.makeText(getActivity(), R.string.message_copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteMessageRequested(final FriendlyMessage friendlyMessage) {
                MLog.d(TAG, " msg.getImageUrl(): " + friendlyMessage.getImageUrl() + " " + friendlyMessage.getImageId());
                new MessagesDialogHelper().showDeleteMessageDialog(getActivity(),
                        friendlyMessage, getViewModel());
            }

            @Override
            public void onBlockPersonRequested(final FriendlyMessage friendlyMessage) {
                new BlockUserDialogHelper(FirebaseDatabase.getInstance()).showBlockUserQuestionDialog(getActivity(),
                        friendlyMessage.getUserid(),
                        friendlyMessage.getName(),
                        friendlyMessage.getDpid(),
                        getViewModel().getBlockedUserListener());
            }

            @Override
            public void onReportPersonRequested(FriendlyMessage friendlyMessage) {
                new ReportUserDialogHelper().showReportUserQuestionDialog(getActivity(),
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
        messagesAdapter.setActivity(getActivity(), getBaseActivity());
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
        PrivateChatActivity.startPrivateChatActivity(getActivity(), userid, username, dpid, false, transitionImageView, null,
                null);
    }

    protected void setupToolbar() {
        mToolbar = getView().findViewById(R.id.toolbar);
        getBaseActivity().setSupportActionBar(mToolbar);

        ActionBar ab = getBaseActivity().getSupportActionBar();
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
            Toast.makeText(getActivity(), getString(R.string.general_api_error, extra), Toast
                    .LENGTH_SHORT).show();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    @Override
    public void showUsernameExistsDialog(@NonNull String badUsername) {
        new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.username_exists, badUsername)).show();
    }

    @Override
    public void showProfileUpdatedDialog() {
        ((AbstractChatActivity)getBaseActivity()).showProfileUpdatedDialog();
    }

    @Override
    public void showYouHaveBeenBanned() {
        Toast.makeText(getActivity(),R.string.you_have_been_banned, Toast.LENGTH_SHORT).show();
    }

    private Toast slowDownToast;
    @Override
    public void showSlowDown() {
        if (slowDownToast != null) {
            slowDownToast.cancel();
        }
        slowDownToast = Toast.makeText(getActivity(),R.string.slow_down, Toast.LENGTH_SHORT);
        slowDownToast.show();
    }
}
