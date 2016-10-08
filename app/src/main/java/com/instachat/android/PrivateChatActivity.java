package com.instachat.android;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.PrivateChatSummary;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;

import org.json.JSONObject;

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
                    initPrivateChatSummaryIfNecessary();
                    populateUserProfile();
                    getSupportActionBar().setTitle(mToUser.getUsername());
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
                toolbarProfileImageView.setAlpha(1 - alpha);

                MLog.d(TAG, "appBarLayout.height: " + appBarLayout.getHeight(), " verticalOffset ", verticalOffset, " toolbarHeight ", getToolbarHeight(), " alpha ", alpha);
                if (verticalOffset == 0) {
                    mIsAppBarExpanded = true;
                } else if (Math.abs(verticalOffset) + getToolbarHeight() == appBarLayout.getHeight()) {
                    mIsAppBarExpanded = false;
                }
            }
        });

        final View.OnClickListener onClickExpandCollapseListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsAppBarExpanded)
                    appBarLayout.setExpanded(true, true);
                else
                    appBarLayout.setExpanded(false, true);
            }
        };
        findViewById(R.id.toolbar).setOnClickListener(onClickExpandCollapseListener);
        bio.setOnClickListener(onClickExpandCollapseListener);
        profilePic.setOnClickListener(onClickExpandCollapseListener);


    }

    private boolean mIsAppBarExpanded = true; //initially it's expanded

    private void initPrivateChatSummaryIfNecessary() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHATS_SUMMARY_PARENT_REF());
        PrivateChatSummary summary = new PrivateChatSummary();
        summary.setName(mToUser.getUsername());
        summary.setDpid(mToUser.getProfilePicUrl());
        ref.child(mToUser.getId() + "").updateChildren(PrivateChatSummary.toMap(summary));
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

    public static void startPrivateChatActivity(Context context, int userid) {
        Intent intent = newIntent(context, userid);
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
        }, 1750);
    }

    private void clearPrivateUnreadMessages(int toUserid) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHATS_SUMMARY_PARENT_REF());
            ref.child(toUserid + "").child(Constants.CHILD_UNREAD_MESSAGES).removeValue();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

}
