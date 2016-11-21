package com.instachat.android;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.blocks.BlockUserDialogHelper;
import com.instachat.android.blocks.ReportUserDialogHelper;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.GroupChatSummary;
import com.instachat.android.model.PrivateChatSummary;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.TimeUtil;
import com.tooltip.Tooltip;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by kevin on 9/16/2016.
 * The difference between Private and Group Chat:
 * 1) Title - Name of person you are talking to you
 * 2) Realtime database reference
 * 3) Every message you post gets sent to the person you are talking to
 * so if they are not in the chat room they will receive an notification
 * 4) we need to fetch the user that we are talking to from network api
 */
public class PrivateChatActivity extends GroupChatActivity {

    private static final String TAG = "PrivateChatActivity";
    private long mLastTypingTime;
    private ValueEventListener mUserInfoValueEventListener = null;
    private FirebaseRemoteConfig mConfig;
    private View mMessageRecyclerViewParent;
    private ImageView mProfilePic;
    private AppBarLayout mAppBarLayout;
    private GestureDetectorCompat mGestureDetector;
    private static boolean sIsActive;
    private static int sUserid;
    private static String sUsername, sProfilePicUrl;

    @Override
    protected int getLayout() {
        return R.layout.activity_private_chat;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_App);
        super.onCreate(savedInstanceState);
        MLog.d(TAG, "onCreate() ");
        onNewIntent(getIntent());
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        mConfig = FirebaseRemoteConfig.getInstance();
    }

    @Override
    protected void onHomeClicked() {
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mProfilePic = (ImageView) findViewById(R.id.profile_pic);
        mMessageRecyclerViewParent = findViewById(R.id.messageRecyclerViewParent);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        getSupportActionBar().setTitle("");
        sUserid = getIntent().getIntExtra(Constants.KEY_USERID, 0);
        sUsername = getIntent().getStringExtra(Constants.KEY_USERNAME);
        sProfilePicUrl = getIntent().getStringExtra(Constants.KEY_PROFILE_PIC_URL);
        MLog.d(TAG, "before grab from server, intent data: sUserid: ", sUserid, " sUsernane: ", sUsername, " sProfilePicUrl: ", sProfilePicUrl);
        loadBasicData(getIntent());
        NetworkApi.getUserById(this, sUserid, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final User toUser = User.fromResponse(response);
                    sUsername = toUser.getUsername();
                    sProfilePicUrl = toUser.getProfilePicUrl();
                    MLog.d(TAG, "after grab from server: toUser: ", toUser.getId(), " ", toUser.getUsername(), " ", toUser.getProfilePicUrl());
                    populateUserProfile(toUser);
                    setCustomTitles(toUser.getUsername(), 0);
                    listenForPartnerTyping();
                    checkIfPartnerIsBlocked();

                    mUserInfoValueEventListener = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(sUserid)).addValueEventListener(new ValueEventListener() {
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
                                    MLog.d(TAG, "user info changed onlyUpdateLastActiveTime: ", onlyUpdateLastActiveTime);

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
                mMessageRecyclerViewParent.setAlpha(1 - (alpha / 2f));

                MLog.d(TAG, "mAppBarLayout.height: " + appBarLayout.getHeight(), " verticalOffset ", verticalOffset, " toolbarHeight ", getToolbarHeight(), " alpha ", alpha);
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
        updateLastActiveTimestamp();
    }

    private void checkIfSeenToolbarProfileTooltip(View anchor) {
        if (Preferences.getInstance().hasShownToolbarProfileTooltip())
            return;
        Preferences.getInstance().setShownToolbarProfileTooltip(true);
        final Tooltip tooltip = new Tooltip.Builder(anchor, R.style.drawer_tooltip_non_cancellable)
                .setText(getString(R.string.toolbar_user_profile_tooltip))
                .show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                if (tooltip.isShowing())
                    tooltip.dismiss();
            }
        }, mConfig.getLong(Constants.KEY_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME));

    }

    private void showErrorToast(String extra) {
        try {
            Toast.makeText(PrivateChatActivity.this, getString(R.string.general_api_error, extra), Toast.LENGTH_SHORT).show();
        }catch (Exception e) {
            MLog.e(TAG,"",e);
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
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF())
                .child(sUserid + "");
        PrivateChatSummary summary = new PrivateChatSummary();
        summary.setName(sUsername);
        summary.setDpid(sProfilePicUrl);
        summary.setAccepted(true);
        Map<String, Object> map = summary.toMap();
        map.put(Constants.FIELD_LAST_MESSAGE_SENT_TIMESTAMP, ServerValue.TIMESTAMP);
        ref.updateChildren(map);
    }

    @Override
    public void onResume() {
        sIsActive = true;
        super.onResume();
    }

    @Override
    public void onPause() {
        sIsActive = false;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mUserInfoValueEventListener != null) {
            try {
                FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(sUserid)).removeEventListener(mUserInfoValueEventListener);
            } catch (Exception e) {
                MLog.e(TAG, "", e);
            }
        }
        if (mTypingValueEventListener != null && mTypingReference != null) {
            mTypingReference.removeEventListener(mTypingValueEventListener);
        }
        super.onDestroy();
    }

    @Override
    void initDatabaseRef() {
        setDatabaseRoot(Constants.PRIVATE_CHAT_REF(getIntent().getIntExtra(Constants.KEY_USERID, 0)));
    }

    @Override
    public void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage) {
        super.onFriendlyMessageSuccess(friendlyMessage);

        try {
            NetworkApi.gcmsend(sUserid, Constants.GcmMessageType.msg, friendlyMessage.toLightweightJSONObject());
        } catch (JSONException e) {
            MLog.e(TAG, "", e);
        }
        if (mIsAppBarExpanded) {
            mAppBarLayout.setExpanded(false, true);
        }
        initializePrivateChatSummary();
    }

    @Override
    protected void onMeTyping() {
        try {
            if (System.currentTimeMillis() - mLastTypingTime < 3000) {
                return;
            }
            FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHAT_TYPING_REF(sUserid)).child("" + myUserid()).child(Constants.CHILD_TYPING).setValue(true);
            mLastTypingTime = System.currentTimeMillis();
        } catch (Exception e) {
            MLog.e(TAG, "onMeTyping() failed", e);
        }
    }

    public static void startPrivateChatActivity(Activity activity, int userid, String username, String profilePicUrl,
                                                final boolean autoAddUser,
                                                final View transitionImageView,
                                                Uri sharePhotoUri, String shareMessage) {
        Intent intent = new Intent(activity, PrivateChatActivity.class);
        intent.putExtra(Constants.KEY_USERID, userid);
        intent.putExtra(Constants.KEY_USERNAME, username);
        intent.putExtra(Constants.KEY_PROFILE_PIC_URL, profilePicUrl);
        intent.putExtra(Constants.KEY_AUTO_ADD_PERSON, autoAddUser);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (sharePhotoUri != null)
            intent.putExtra(Constants.KEY_SHARE_PHOTO_URI, sharePhotoUri);
        if (shareMessage != null)
            intent.putExtra(Constants.KEY_SHARE_MESSAGE, shareMessage);

        if (transitionImageView != null) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImageView, "profilePic");
            activity.startActivity(intent, options.toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    public static boolean isActive() {
        return sIsActive;
    }

    public static int getActiveUserid() {
        return sUserid;
    }

    @Override
    protected void onRemoteUserTyping(int userid, String username, String dpid) {
        if (isActivityDestroyed() || !sIsActive || this.sUserid != userid) {
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
            final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF())
                    .child(sUserid + "");
            ref.updateChildren(privateChatSummary.toMap());

            /**
             * remove the person from your pending requests
             */
            FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_REQUEST_STATUS_PARENT_REF(sUserid, Preferences.getInstance().getUserId())).removeValue();
        }

    }

    private void populateUserProfile(final User toUser) {
        if (toUser == null || isActivityDestroyed()) return;
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
                Glide.with(PrivateChatActivity.this)
                        .load(toUser.getProfilePicUrl())
                        .error(R.drawable.ic_anon_person_36dp)
                        //.crossFade()
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                if (isActivityDestroyed())
                                    return false;
                                collapseAppbarAfterDelay();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                if (isActivityDestroyed())
                                    return false;
                                collapseAppbarAfterDelay();
                                return false;
                            }
                        })
                        .into(mProfilePic);
                Glide.with(PrivateChatActivity.this)
                        .load(toUser.getProfilePicUrl())
                        .error(R.drawable.ic_anon_person_36dp)
                        .crossFade()
                        .into(miniPic);
                Glide.with(PrivateChatActivity.this)
                        .load(toUser.getProfilePicUrl())
                        .error(R.drawable.ic_anon_person_36dp)
                        .crossFade()
                        .into(toolbarProfileImageView);

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
            activeGroup.setText(getString(R.string.user_active_in_group, toUser.getCurrentGroupName()));
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
        }, mConfig.getLong(Constants.KEY_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY));
    }

    private void clearPrivateUnreadMessages(int toUserid) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF());
            ref.child(toUserid + "").child(Constants.CHILD_UNREAD_MESSAGES).removeValue();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    @Override
    protected void addUserPresenceToGroup() {
        //user presence does not apply in private chat situations
    }

    @Override
    protected void removeUserPresenceFromGroup() {
        //user presence does not apply in private chat situations
    }

    @Override
    protected void setupRightDrawerContent() {
        // does not apply to private chat situations
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
            case R.id.menu_view_profile:
                if (!mIsAppBarExpanded)
                    toggleAppbar();
                return true;
            case R.id.menu_block_user:
                new BlockUserDialogHelper().showBlockUserQuestionDialog(this, sUserid,
                        sUsername,
                        sProfilePicUrl,
                        getBlockedUserListener());
                return true;
            case R.id.menu_report_user:
                new ReportUserDialogHelper().showReportUserQuestionDialog(this, sUserid,
                        sUsername,
                        sProfilePicUrl);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void setToolbarOnClickListener(Toolbar toolbar) {
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleAppbar();
            }
        });
    }

    @Override
    protected void onUserBlocked(int userid) {
        if (isActivityDestroyed())
            return;
        if (userid == getActiveUserid()) {
            finish();
        }
        super.onUserBlocked(userid);
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
        if (Preferences.getInstance().getLastGroupChatRoomVisited() == groupChatSummary.getId()) {
            finish();
        }
        super.onGroupChatClicked(groupChatSummary);
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

    @Override
    protected boolean isPrivateChat() {
        return true;
    }

    @Override
    public void onBackPressed() {
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
            super.onUserClicked(userid, username, dpid, transitionImageView);
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
            public boolean onFling(MotionEvent event1, MotionEvent event2,
                                   float velocityX, float velocityY) {
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
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF()).child(sUserid + "");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "onDataChange() snapshot: " + dataSnapshot, " ref: ", ref);
                ref.removeEventListener(this);
                if (dataSnapshot.getValue() == null) {
                    //
                } else {
                    Toast.makeText(PrivateChatActivity.this, getString(R.string.cannot_chat_you_blocked_them, sUsername), Toast.LENGTH_LONG).show();
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
        if (menu == null) return false;
        if (sUserid == myUserid()) {
            if (menu.findItem(R.id.menu_block_user) != null) {
                menu.removeItem(R.id.menu_block_user);
            }
            if (menu.findItem(R.id.menu_report_user) != null) {
                menu.removeItem(R.id.menu_report_user);
            }
        }
        return super.onMenuOpened(featureId, menu);
    }
}
