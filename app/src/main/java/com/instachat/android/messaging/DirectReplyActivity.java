package com.instachat.android.messaging;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.instachat.android.Constants;
import com.instachat.android.PrivateChatActivity;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;

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
        int userid = intent.getIntExtra(Constants.KEY_USERID, 0);
        MLog.d(TAG, " to user id: ", userid, " direct reply: ", myDirectReply);

//        Notification repliedNotification =
//                new Notification.Builder(this)
//                        .setSmallIcon(android.R.drawable.ic_dialog_email)
//                        .setContentText("Reply sent!")
//                        .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(userid, repliedNotification);
        notificationManager.cancel(userid); //clear the waiting system tray notification

        if (TextUtils.isEmpty(myDirectReply)) {
            //todo: start private chat activity
            PrivateChatActivity.startPrivateChatActivity(this,userid,null,null);
            finish();
        } else {
            //todo: silently reply to the user with your direct reply
            //String text, String name, int userid, String dpid, String imageUrl, String imageId, long time
            User me = Preferences.getInstance().getUser();
            FriendlyMessage friendlyMessage = new FriendlyMessage(myDirectReply,me.getUsername(),me.getId(),
                    me.getProfilePicUrl(), null, null, System.currentTimeMillis());

            finish();
        }

    }

}
