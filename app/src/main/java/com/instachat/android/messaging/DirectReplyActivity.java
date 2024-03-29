package com.instachat.android.messaging;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.RemoteInput;

import android.text.TextUtils;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;

import org.json.JSONObject;

import javax.inject.Inject;

/**
 * Created by kevin on 10/18/2016.
 */

public class DirectReplyActivity extends AppCompatActivity {

    public static final String TAG = "DirectReplyActivity";

    @Inject
    NetworkApi networkApi;

    public static Intent newIntent(Context context, int userid, String username, String dpid) {
        Intent intent = new Intent(context, DirectReplyActivity.class);
        intent.putExtra(Constants.KEY_USERID, userid);
        intent.putExtra(Constants.KEY_USERNAME, username);
        intent.putExtra(Constants.KEY_PROFILE_PIC_URL, dpid);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        MLog.d(TAG, "instantiated intent with userid: " + userid);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

        String myDirectReply = "";
        if (remoteInput != null) {
            myDirectReply = remoteInput.getCharSequence(Constants.KEY_TEXT_REPLY).toString();
        }
        final int userid = intent.getIntExtra(Constants.KEY_USERID, 0);
        final String username = intent.getStringExtra(Constants.KEY_USERNAME);
        final String profilePicUrl = intent.getStringExtra(Constants.KEY_PROFILE_PIC_URL);
        MLog.d(TAG, " to user id: ", userid, " direct reply: ", myDirectReply);

//        Notification repliedNotification =
//                new Notification.Builder(this)
//                        .setSmallIcon(android.R.drawable.ic_dialog_email)
//                        .setContentText("Reply sent!")
//                        .build();

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(userid, repliedNotification);

        if (TextUtils.isEmpty(myDirectReply)) {
            PrivateChatActivity.startPrivateChatActivity(this, userid, username, profilePicUrl, false, null, null, null);
            finish();
        } else {
            User me = UserPreferences.getInstance().getUser();
            final FriendlyMessage friendlyMessage = new FriendlyMessage(myDirectReply, me.getUsername(), me.getId(),
                    me.getProfilePicUrl(), null, false, false, null, System.currentTimeMillis());

            FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHAT_REF(userid)).push().addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    friendlyMessage.setId(dataSnapshot.getKey());
                    FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_CHAT_REF(userid)).
                            child(friendlyMessage.getId()).
                            setValue(friendlyMessage).
                            addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    notificationManager.cancel(userid);
                                    if (task.isSuccessful()) {
                                        try {
                                            JSONObject o = friendlyMessage.toLightweightJSONObject();
                                            networkApi.gcmsend(userid, Constants.GcmMessageType.msg, o);
                                        } catch (Exception e) {
                                            MLog.e(TAG, "gcmsend() failed", e);
                                        }
                                    } else {
                                        MLog.e(TAG, "gcmsend() failed.  task is not successful");
                                    }
                                }
                            });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    notificationManager.cancel(userid);
                }
            });
            finish();
        }

    }

}
