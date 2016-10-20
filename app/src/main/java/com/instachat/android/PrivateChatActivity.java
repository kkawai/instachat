package com.instachat.android;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.blocks.BlockUserDialogHelper;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.GroupChatSummary;
import com.instachat.android.model.PrivateChatSummary;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.TimeUtil;
import com.tooltip.Tooltip;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
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
    private User mToUser;
    private static boolean sIsActive;
    private static int sToUserid;
    private ChatTypingHelper mTypingHelper;
    private long mLastTypingTime;
    private ValueEventListener mUserInfoValueEventListener = null;

    @Override
    protected int getLayout() {
        return R.layout.activity_private_chat;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MLog.d(TAG, "onCreate() ");
        onNewIntent(getIntent());
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
    }

    @Override
    protected void onHomeClicked() {
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getSupportActionBar().setTitle("");
        final int toUserid = getIntent().getIntExtra(Constants.KEY_USERID, 0);
        MLog.d(TAG, "onNewIntent() toUserid : " + toUserid);

        sToUserid = toUserid;
        NetworkApi.getUserById(this, toUserid, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    mToUser = User.fromResponse(response);
                    createPrivateChatSummary();
                    populateUserProfile();
                    //getSupportActionBar().setTitle(mToUser.getUsername());
                    setCustomTitles(mToUser.getUsername(), mToUser.getLastOnline());

                    mUserInfoValueEventListener = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(sToUserid)).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try {
                                if (dataSnapshot.getValue() != null) {
                                    User user = dataSnapshot.getValue(User.class);
                                    mToUser.setProfilePicUrl(user.getProfilePicUrl());
                                    mToUser.setBio(user.getBio());
                                    mToUser.setUsername(user.getUsername());
                                    setCustomTitles(user.getUsername(), user.getLastOnline());
                                    populateUserProfile();
                                }
                            } catch (Exception e) {
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });


                } catch (Exception e) {
                    Toast.makeText(PrivateChatActivity.this, getString(R.string.general_api_error, "1"), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                MLog.e(TAG, "NetworkApi.getUserById(" + toUserid + ") failed in onCreate()", error);
                Toast.makeText(PrivateChatActivity.this, getString(R.string.general_api_error, "2"), Toast.LENGTH_SHORT).show();
            }
        });
        final NotificationManager notificationManager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        notificationManager.cancel(toUserid);
        MLog.d(TAG, "Cancelled notification " + toUserid);
        clearPrivateUnreadMessages(toUserid);

        final View customTitlePairInParallax = findViewById(R.id.customTitlePairInParallax);
        final View customTitlePairInToolbar = findViewById(R.id.customTitlePairInToolbar);

        final ImageView toolbarProfileImageView = (ImageView) findViewById(R.id.topCornerUserThumb);
        final TextView bio = (TextView) findViewById(R.id.bio);
        final ImageView profilePic = (ImageView) findViewById(R.id.profile_pic);
        final AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //android.R.attr.actionBarSize
                float alpha = 1 - (float) (Math.abs(verticalOffset) + getToolbarHeight()) / appBarLayout.getHeight();
                if (verticalOffset == 0) {
                    alpha = 1;
                }
                bio.setAlpha(alpha);
                profilePic.setAlpha(alpha);
                customTitlePairInParallax.setAlpha(alpha);
                toolbarProfileImageView.setAlpha(1 - alpha);
                customTitlePairInToolbar.setAlpha(1 - alpha);

                MLog.d(TAG, "appBarLayout.height: " + appBarLayout.getHeight(), " verticalOffset ", verticalOffset, " toolbarHeight ", getToolbarHeight(), " alpha ", alpha);
                if (verticalOffset == 0) {
                    mIsAppBarExpanded = true;
                } else if (Math.abs(verticalOffset) + getToolbarHeight() == appBarLayout.getHeight()) {
                    mIsAppBarExpanded = false;
                    checkIfSeenToolbarProfileTooltip(profilePic);
                }
            }
        });

        final View.OnClickListener appBarOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleAppbar();
            }
        };
        appBarLayout.setOnClickListener(appBarOnClickListener);
        //findViewById(R.id.toolbar).setOnClickListener(appBarOnClickListener);
        //bio.setOnClickListener(appBarOnClickListener);
        //profilePic.setOnClickListener(appBarOnClickListener);
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
        }, Constants.MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME);

    }

    private void toggleAppbar() {
        final AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        if (!mIsAppBarExpanded)
            appBarLayout.setExpanded(true, true);
        else
            appBarLayout.setExpanded(false, true);
    }

    private boolean mIsAppBarExpanded = true; //initially it's expanded

    /**
     * If the relationship has already been established, don't
     * create it.  Otherwise, create it.
     */
    private void createPrivateChatSummary() {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF())
                .child(mToUser.getId() + "");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    ref.removeEventListener(this);
                    PrivateChatSummary summary = new PrivateChatSummary();
                    summary.setName(mToUser.getUsername());
                    summary.setDpid(mToUser.getProfilePicUrl());
                    ref.updateChildren(PrivateChatSummary.toMap(summary));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updatePrivateChatSummaryLastMessageSentTimestamp() {
        Map<String, Object> map = new HashMap<>(1);
        map.put(Constants.FIELD_LAST_MESSAGE_SENT_TIMESTAMP, System.currentTimeMillis());
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF());
        ref.child(mToUser.getId() + "").updateChildren(map);
    }

    @Override
    public void onResume() {
        sIsActive = true;
        super.onResume();
        mTypingHelper = new ChatTypingHelper();
        mTypingHelper.setListener(this);
        mTypingHelper.register();
    }

    @Override
    public void onPause() {
        sIsActive = false;
        super.onPause();
        if (mTypingHelper != null)
            mTypingHelper.unregister();
    }

    @Override
    public void onDestroy() {
        if (mUserInfoValueEventListener != null) {
            try {
                FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(sToUserid)).removeEventListener(mUserInfoValueEventListener);
            } catch (Exception e) {
                MLog.e(TAG, "", e);
            }
        }
        super.onDestroy();
    }

    @Override
    void initDatabaseRef() {
        int toUserid = getIntent().getIntExtra(Constants.KEY_USERID, 0);
        setDatabaseRoot(Constants.PRIVATE_CHAT_REF(toUserid));
    }

    @Override
    public void onFriendlyMessageSuccess(FriendlyMessage friendlyMessage) {
        super.onFriendlyMessageSuccess(friendlyMessage);
        try {
            JSONObject o = friendlyMessage.toLightweightJSONObject();
            NetworkApi.gcmsend("" + mToUser.getId(), Constants.GcmMessageType.msg, o);
        } catch (Exception e) {
            MLog.e(TAG, "onFriendlyMessageSent() failed", e);
            Toast.makeText(PrivateChatActivity.this, getString(R.string.general_api_error, "3"), Toast.LENGTH_SHORT).show();
        }
        if (mIsAppBarExpanded) {
            final AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
            appBarLayout.setExpanded(false, true);
        }
        updatePrivateChatSummaryLastMessageSentTimestamp();
    }

    @Override
    void onMeEnteringText() {
        try {
            if (System.currentTimeMillis() - mLastTypingTime < 3000) {
                return;
            }
            JSONObject o = new JSONObject();
            o.put(Constants.KEY_USERID, myUserid());
            NetworkApi.gcmsend("" + mToUser.getId(), Constants.GcmMessageType.typing, o);
            mLastTypingTime = System.currentTimeMillis();
        } catch (Exception e) {
            MLog.e(TAG, "onMeEnteringText() failed", e);
        }
    }

    public static void startPrivateChatActivity(final Context context, final int userid,
                                                final Uri sharePhotoUri, final String shareMessage) {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF() + userid);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MLog.d(TAG, "onDataChange() snapshot: " + dataSnapshot, " ref: ", ref);
                ref.removeEventListener(this);
                if (dataSnapshot.getValue() == null) {
                    startPrivateChatActivityInternal(context, userid, sharePhotoUri, shareMessage);
                } else {
                    Toast.makeText(context, R.string.cannot_chat_you_blocked_them, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ref.removeEventListener(this);
                Toast.makeText(context, context.getString(R.string.general_api_error, "(c)"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void startPrivateChatActivityInternal(Context context, int userid,
                                                         Uri sharePhotoUri, String shareMessage) {
        Intent intent = newIntent(context, userid);
        if (sharePhotoUri != null)
            intent.putExtra(Constants.KEY_SHARE_PHOTO_URI, sharePhotoUri);
        if (shareMessage != null)
            intent.putExtra(Constants.KEY_SHARE_MESSAGE, shareMessage);
        context.startActivity(intent);
    }

    public static Intent newIntent(Context context, int userid) {
        Intent intent = new Intent(context, PrivateChatActivity.class);
        intent.putExtra(Constants.KEY_USERID, userid);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        MLog.d(TAG, "instantiated intent with userid: " + userid);
        return intent;
    }

    public static boolean isActive() {
        return sIsActive;
    }

    public static int getActiveUserid() {
        return sToUserid;
    }

    @Override
    public void onRemoteUserTyping(int userid) {
        if (!sIsActive || sToUserid != userid || isActivityDestroyed()) {
            return;
        }
        showTypingDots();
    }

    private void populateUserProfile() {
        if (mToUser == null || isActivityDestroyed()) return;
        final ImageView toolbarProfileImageView = (ImageView) findViewById(R.id.topCornerUserThumb);
        final ImageView miniPic = (ImageView) findViewById(R.id.superSmallProfileImage);
        final TextView bio = (TextView) findViewById(R.id.bio);
        final ImageView profilePic = (ImageView) findViewById(R.id.profile_pic);
        Constants.DP_URL(mToUser.getId(), mToUser.getProfilePicUrl(), new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (isActivityDestroyed())
                    return;
                if (!task.isSuccessful()) {
                    miniPic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                    collapseAppbarAfterDelay();
                    return;
                }
                try {
                    Glide.with(PrivateChatActivity.this)
                            .load(task.getResult().toString())
                            .error(R.drawable.ic_account_circle_black_36dp)
                            .crossFade()
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
                            .into(profilePic);
                    Glide.with(PrivateChatActivity.this)
                            .load(task.getResult().toString())
                            .error(R.drawable.ic_account_circle_black_36dp)
                            .crossFade()
                            .into(miniPic);
                    Glide.with(PrivateChatActivity.this)
                            .load(task.getResult().toString())
                            .error(R.drawable.ic_account_circle_black_36dp)
                            .crossFade()
                            .into(toolbarProfileImageView);


                } catch (Exception e) {
                    MLog.e(TAG, "onDrawerOpened() could not find user photo in google cloud storage", e);
                    miniPic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                    collapseAppbarAfterDelay();
                }
            }
        });
        bio.setText(mToUser.getBio() + "");

    }

    private void collapseAppbarAfterDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                final AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
                if (mIsAppBarExpanded)
                    appBarLayout.setExpanded(false, true);
            }
        }, Constants.COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY);
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
            case android.R.id.home:
                onHomeClicked();
                return true;
            case R.id.view_profile:
                if (!mIsAppBarExpanded)
                    toggleAppbar();
                return true;
            case R.id.invite_menu:
                sendInvitation();
                return true;
            case R.id.block:
                new BlockUserDialogHelper().showBlockUserQuestionDialog(this, mToUser.getId(),
                        mToUser.getUsername(),
                        mToUser.getProfilePicUrl(),
                        getBlockedUserListener());
                return true;
            case R.id.report:
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
    }

    private long mLastOnlineTimestamp = 0;

    private void setCustomTitles(String username, long lastOnline) {

        if (lastOnline > mLastOnlineTimestamp) {
            String lastActive = "";
            lastActive = TimeUtil.getTimeAgo(new Date(lastOnline));
            mLastOnlineTimestamp = lastOnline;
            if (!TextUtils.isEmpty(lastActive)) {
                ((TextView) findViewById(R.id.customSubtitleInParallax)).setText("  •  " + lastActive);
                ((TextView) findViewById(R.id.customSubtitleInToolbar)).setText(lastActive);
            }
        }

        ((TextView) findViewById(R.id.customTitleInToolbar)).setText(username);
        ((TextView) findViewById(R.id.customTitleInParallax)).setText(username);
    }

    @Override
    protected String getPhotoUploadDialogTitle() {
        String username = mToUser != null ? mToUser.getUsername() : "";
        if (TextUtils.isEmpty(username))
            username = getString(R.string.anonymous_user);
        return getString(R.string.send_photo_to_group_or_person, username);
    }

    @Override
    public void onGroupChatClicked(GroupChatSummary groupChatSummary) {
        if (Preferences.getInstance().getLastGroupChatRoomVisited() == groupChatSummary.getId()) {
            finish();
            return;
        }
        super.onGroupChatClicked(groupChatSummary);
    }
}
