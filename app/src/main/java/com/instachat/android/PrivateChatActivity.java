package com.instachat.android;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MLog.d(TAG, "onCreate() ");
        onNewIntent(getIntent());
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_keyboard_arrow_left_white_36dp);
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
                    fillMiniPic();
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
    }

    private void initPrivateChatSummaryIfNecessary() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHATS_SUMMARY_PARENT_REF());
        PrivateChatSummary summary = new PrivateChatSummary();
        summary.setName(mToUser.getUsername());
        summary.setDpid(mToUser.getProfilePicUrl());
        ref.child(mToUser.getId() + "").updateChildren(PrivateChatSummary.toMap(summary));

        /*DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHATS_SUMMARY_PARENT_REF());
        ref.limitToLast(1000).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                try {
                    PrivateChatSummary summary = new PrivateChatSummary();
                    summary.setId(dataSnapshot.getKey());
                    String name = (String) dataSnapshot.child("name").getValue();
                    String dpid = (String) dataSnapshot.child("dpid").getValue();
                    long lastMessageTime = (Long) dataSnapshot.child("lastMessageTime").getValue();
                    MLog.d(TAG, "got summary: ", name, " dpid: ", dpid, " lastMessageTime: ", lastMessageTime);
                } catch (Exception e) {
                    MLog.e(TAG, "", e);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/
        /*DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHATS_SUMMARY_PARENT_REF());
        PrivateChatSummary summary = new PrivateChatSummary();
        summary.setName(mToUser.getUsername());
        summary.setDpid(mToUser.getProfilePicUrl());
        summary.setLastMessageTime(100);
        ref.child(mToUser.getId() + "").setValue(summary);

        summary.setName("fakeTestUser");
        summary.setDpid("some fake dpid");
        summary.setLastMessageTime(99);
        ref.child("1111").setValue(summary);

        summary.setName("fakeTestUser 2");
        summary.setDpid("some fake dpid 2");
        summary.setLastMessageTime(101);
        ref.child("1112").setValue(summary);*/


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
        setDatabaseRef(Constants.PRIVATE_CHAT_REF(toUserid));
    }

    @Override
    void onFriendlyMessageSent(FriendlyMessage friendlyMessage) {
        try {
            JSONObject o = friendlyMessage.toJSONObject();
            o.put(Constants.KEY_GCM_MSG_TYPE, Constants.GcmMessageType.msg.name());
            NetworkApi.gcmsend("" + mToUser.getId(), o);
        } catch (Exception e) {
            MLog.e(TAG, "onFriendlyMessageSent() failed", e);
            Toast.makeText(PrivateChatActivity.this, getString(R.string.general_api_error, "3"), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    void onMeEnteringText() {
        try {
            if (System.currentTimeMillis() - mLastTypingTime < 3000) {
                return;
            }
            JSONObject o = new JSONObject();
            o.put(Constants.KEY_GCM_MSG_TYPE, Constants.GcmMessageType.typing.name());
            o.put(Constants.KEY_USERID, myUserid());
            NetworkApi.gcmsend("" + mToUser.getId(), o);
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

    private void fillMiniPic() {
        if (mToUser == null || isActivityDestroyed()) return;
        final ImageView miniPic = (ImageView) findViewById(R.id.superSmallProfileImage);
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
                            .into(miniPic);
                } catch (Exception e) {
                    MLog.e(TAG, "onDrawerOpened() could not find user photo in google cloud storage", e);
                    miniPic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                }
            }
        });

    }

}
