package com.instachat.android.messaging;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.RemoteInput;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.PrivateChatActivity;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;

import org.json.JSONObject;

/**
 * Created by kevin on 10/18/2016.
 */

public class DirectReplyActivity extends AppCompatActivity {

    public static final String TAG = "DirectReplyActivity";

    public static Intent newIntent(Context context, int userid) {
        Intent intent = new Intent(context, DirectReplyActivity.class);
        intent.putExtra(Constants.KEY_USERID, userid);
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
        MLog.d(TAG, " to user id: ", userid, " direct reply: ", myDirectReply);

//        Notification repliedNotification =
//                new Notification.Builder(this)
//                        .setSmallIcon(android.R.drawable.ic_dialog_email)
//                        .setContentText("Reply sent!")
//                        .build();

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(userid, repliedNotification);

        if (TextUtils.isEmpty(myDirectReply)) {
            PrivateChatActivity.startPrivateChatActivity(this, userid, null, null);
            finish();
        } else {
            User me = Preferences.getInstance().getUser();
            final FriendlyMessage friendlyMessage = new FriendlyMessage(myDirectReply, me.getUsername(), me.getId(),
                    me.getProfilePicUrl(), null, null, System.currentTimeMillis());

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
                                            NetworkApi.gcmsend("" + userid, Constants.GcmMessageType.msg, o);
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
