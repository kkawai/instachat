package com.instachat.android.app.activity;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.databinding.ViewDataBinding;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.view.inputmethod.EditorInfoCompat;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.MessageOptionsDialogHelper;
import com.instachat.android.app.activity.group.LogoutDialogHelper;
import com.instachat.android.app.adapter.ChatSummariesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.ui.base.BaseActivity;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.User;
import com.instachat.android.font.FontUtil;
import com.instachat.android.gcm.GCMHelper;
import com.instachat.android.messaging.InstachatMessagingService;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.rx.SchedulerProvider;
import com.tooltip.Tooltip;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.Observable;
import io.reactivex.functions.Action;

public abstract class AbstractChatActivity<T extends ViewDataBinding, V extends AbstractChatViewModel> extends BaseActivity<T, V>
        implements UploadListener, AttachPhotoOptionsDialogHelper.PhotoOptionsListener, AbstractChatNavigator {

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
    protected NetworkApi networkApi;

    @Inject
    protected LogoutDialogHelper logoutDialogHelper;

    @Inject
    protected ChatSummariesRecyclerAdapter chatsRecyclerViewAdapter;

    @Inject
    protected AdHelper adsHelper;

    @Inject
    protected PresenceHelper presenceHelper;

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

        if (mPhotoUploadHelper.getPhotoType() == PhotoUploadHelper.PhotoType.chatRoomPhoto) {

            final FriendlyMessage friendlyMessage = new FriendlyMessage("", getViewModel().myUsername(), getViewModel().myUserid(), getViewModel().myDpid(),
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
                        friendlyMessage.setMessageType(FriendlyMessage.MESSAGE_TYPE_NORMAL);
                        sendText(friendlyMessage);
                    }

                    @Override
                    public void onSendOneTimeRequested(FriendlyMessage friendlyMessage) {
                        friendlyMessage.setMessageType(FriendlyMessage.MESSAGE_TYPE_ONE_TIME);
                        sendText(friendlyMessage);
                    }
                });
    }

    public void sendText(FriendlyMessage friendlyMessage) {
        try {
            messagesAdapter.sendFriendlyMessage(friendlyMessage);
            mMessageEditText.setText("");//fast double taps on send can cause 2x sends!
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
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
                getViewModel().validateMessage(text, false);
            }
        });
        sendButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final String text = mMessageEditText.getText().toString();
                getViewModel().validateMessage(text, false);
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
}
