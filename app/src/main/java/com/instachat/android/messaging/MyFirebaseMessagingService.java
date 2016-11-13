package com.instachat.android.messaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.instachat.android.Constants;
import com.instachat.android.PrivateChatActivity;
import com.instachat.android.R;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.PrivateChatSummary;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.ThreadWrapper;
import com.instachat.android.view.CircleTransform;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Handle data payload of FCM messages.
        MLog.d(TAG, "FCM Message Id: " + remoteMessage.getMessageId());
        MLog.d(TAG, "FCM Notification Message: " + remoteMessage.getNotification());
        MLog.d(TAG, "FCM Data Message: " + remoteMessage.getData());
        /*
         * {msg={"text":"ok talk to me","name":"kkawai","time":1474173076069,"userid":3733523,"dpid":"ea34ff82-066a-413f-9efe-a816d59863a7.jpg"}}
         */
        try {
            JSONObject object = new JSONObject(remoteMessage.getData());
            if (object.has(Constants.KEY_GCM_MESSAGE)) {
                JSONObject msg = new JSONObject(object.getString(Constants.KEY_GCM_MESSAGE));
                if (msg.has(Constants.KEY_GCM_MSG_TYPE) && msg.getString(Constants.KEY_GCM_MSG_TYPE).equals(Constants.GcmMessageType.typing.name())) {
                    Intent intent = new Intent(Constants.ACTION_USER_TYPING);
                    intent.putExtra(Constants.KEY_USERID, msg.getInt(Constants.KEY_USERID));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                } else if (msg.has(Constants.KEY_GCM_MSG_TYPE) && msg.getString(Constants.KEY_GCM_MSG_TYPE).equals(Constants.GcmMessageType.msg.name())) {
                    final FriendlyMessage friendlyMessage = FriendlyMessage.fromJSONObject(msg);

                    if (PrivateChatActivity.isActive() && PrivateChatActivity.getActiveUserid() == friendlyMessage.getUserid()) {
                    /* Already actively chatting with this person in the PrivateChatActivity
                     * so no need to put up a notification in the system tray.
                     * For debugging purposes, however, if it's myself, then it's ok.
                     */
                        if (PrivateChatActivity.getActiveUserid() != Preferences.getInstance().getUserId())
                            return;
                    }

                    final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF() + friendlyMessage.getUserid());
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            MLog.d(TAG, "onDataChange() snapshot: " + dataSnapshot, " ref: ", ref);
                            if (dataSnapshot.getValue() == null) {
                                MLog.d(TAG, "user ", friendlyMessage.getName(), " is not blocked.  consume message now. ");
                                consumeFriendlyMessage(friendlyMessage);
                            } else {
                                MLog.d(TAG, "user ", friendlyMessage.getName(), " is blocked.  do not consume. ");
                            }
                            ref.removeEventListener(this);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            ref.removeEventListener(this);
                        }
                    });

                }
            }
        } catch (Exception e) {
            MLog.e(TAG, "", e); //todo better error handling/message
        }

        //Glide.with(MyApp.getInstance()).loa
    }

    private void consumeFriendlyMessage(final FriendlyMessage friendlyMessage) {
        incrementPrivateUnreadMessages(friendlyMessage);
        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = Glide.
                            with(MyFirebaseMessagingService.this).
                            load(friendlyMessage.getDpid()).
                            asBitmap().
                            transform(new CircleTransform(MyFirebaseMessagingService.this)).
                            into(thumbSize(), thumbSize()). // Width and height
                            get();
                    showNotification(friendlyMessage, bitmap);
                } catch (Exception e) {
                    MLog.e(TAG, "", e); //todo better error handling/message
                    showNotification(friendlyMessage, null);
                }
            }
        });
    }

    private void showNotification(FriendlyMessage friendlyMessage, Bitmap bitmap) {

        String replyLabel = getString(R.string.type_your_reply_here);
        RemoteInput remoteInput =
                new RemoteInput.Builder(Constants.KEY_TEXT_REPLY)
                        .setLabel(replyLabel)
                        .build();

        // This intent is fired when notification is clicked
        Intent intent = DirectReplyActivity.newIntent(this, friendlyMessage.getUserid());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, friendlyMessage.getUserid(), intent, 0);

        Uri customSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getPackageName() + "/raw/sound_yourturn");

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_dialog_email,
                        getString(R.string.reply), pendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        // Use NotificationCompat.Builder to set up our notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.addAction(replyAction)
                //.setPriority(Notification.PRIORITY_HIGH)  hold off until we figure things out
                .setContentTitle(friendlyMessage.getName())
                .setSmallIcon(R.drawable.ic_stat_ic_message_white_18dp)
                .setLargeIcon(bitmap == null ? BitmapFactory.decodeResource(getResources(), R.drawable.ic_anon_person_36dp) : bitmap)
                .setContentIntent(pendingIntent)
                .setSound(customSound)
                .setSubText(getString(R.string.sent_via, getString(R.string.app_name)));


        //icon appears in device notification bar and right hand corner of notification
        //builder.setSmallIcon(R.drawable.ic_stat_ic_message_white_18dp);

        // Set the intent that will fire when the user taps the notification.
        //builder.setContentIntent(pendingIntent);

        // Large icon appears on the left of the notification

        // Content title, which appears in large type at the top of the notification
        //builder.setContentTitle(friendlyMessage.getName());

        // Content text, which appears in smaller text below the title
        String contentText = "";
        if (friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME && TextUtils.isEmpty(friendlyMessage.getText()))
            contentText = getString(R.string.one_time_photo_notification_title);
        else if (friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME && !TextUtils.isEmpty(friendlyMessage.getText()))
            contentText = getString(R.string.one_time_message_notification_title);
        else if (TextUtils.isEmpty(friendlyMessage.getText()))
            contentText = getString(R.string.photo);
        else
            contentText = friendlyMessage.getText() + "";
        builder.setContentText(contentText);
        // The subtext, which appears under the text on newer devices.
        // This will show-up in the devices with Android 4.2 and above only
        //builder.setSubText(getString(R.string.sent_via, c.getString(R.string.app_name)));

        checkIfIAcceptedThisPersonYet(friendlyMessage, builder.build(), contentText);

    }

    private int thumbSize() {
        return getResources().getDimensionPixelSize(R.dimen.user_thumb_pic_size);
    }

    private void incrementPrivateUnreadMessages(final FriendlyMessage friendlyMessage) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF());
        Map<String, Object> map = new HashMap<>(1);
        map.put("id", friendlyMessage.getId());
        ref.child(friendlyMessage.getUserid() + "").child(Constants.CHILD_UNREAD_MESSAGES).child(friendlyMessage.getId()).updateChildren(map);
    }

    private void checkIfIAcceptedThisPersonYet(final FriendlyMessage friendlyMessage, final Notification notification, final String notificationText) {

        final int myUserid = Preferences.getInstance().getUserId();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (friendlyMessage.getUserid() == myUserid) {
            /**
             * let your own messages go thru; self-messages mainly for debugging purposes
             */
            notificationManager.notify(friendlyMessage.getUserid(), notification);
            return;
        }
        /**
         * check if I accepted this person
         */
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).child(friendlyMessage.getUserid() + "").child(Constants.CHILD_ACCEPTED);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ref.removeEventListener(this);
                if (dataSnapshot.exists() && dataSnapshot.getValue(Boolean.class)) {
                    //I accepted this person

                    //Display the notification in the notification bar
                    notificationManager.notify(friendlyMessage.getUserid(), notification);
                } else {
                    //I have not accepted this person
                    PrivateChatSummary privateChatSummary = new PrivateChatSummary();
                    privateChatSummary.setDpid(friendlyMessage.getDpid());
                    privateChatSummary.setName(friendlyMessage.getName());
                    privateChatSummary.setLastMessage(notificationText);
                    if (friendlyMessage.getImageUrl() != null)
                        privateChatSummary.setImageUrl(friendlyMessage.getImageUrl());
                    privateChatSummary.setLastMessageSentTimestamp(System.currentTimeMillis());
                    privateChatSummary.setAccepted(false);
                    //Create PrivateChatSummary under private_requests
                    FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_REQUEST_STATUS_PARENT(friendlyMessage.getUserid(), myUserid)).setValue(privateChatSummary);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ref.removeEventListener(this);
            }
        });

    }
}
