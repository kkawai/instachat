package com.instachat.android.messaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.instachat.android.Constants;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.R;
import com.instachat.android.app.activity.LauncherActivity;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.gcm.GCMHelper;
import com.instachat.android.gcm.GCMRegistrationManager;
import com.instachat.android.util.MLog;
import com.instachat.android.util.SimpleRxWrapper;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.view.CircleTransform;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class InstachatMessagingService extends FirebaseMessagingService {

    private static final String TAG = "InstachatMessagingService";

    @Inject
    protected GCMHelper gcmHelper;

    //all pending request notifications will have the same id
    //2 will not conflict with any user id
    public static final int NOTIFICATION_ID_PENDING_REQUESTS = 2;
    public static final int NOTIFICATION_ID_FRIEND_JUMPED_IN = 3;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Handle data payload of FCM messages.
        MLog.d(TAG, "FCM Message Id: " + remoteMessage.getMessageId());
        MLog.d(TAG, "FCM Notification Message: " + remoteMessage.getNotification());
        MLog.d(TAG, "FCM Data Message: " + remoteMessage.getData());
        /*
         * {msg={"text":"ok talk to me","name":"kkawai","time":1474173076069,"userid":3733523,
         * "dpid":"ea34ff82-066a-413f-9efe-a816d59863a7.jpg"}}
         */
        try {
            JSONObject object = new JSONObject(remoteMessage.getData());
            if (object.has(Constants.KEY_GCM_MESSAGE)) {
                JSONObject msg = new JSONObject(object.getString(Constants.KEY_GCM_MESSAGE));
                if (msg.has(Constants.KEY_GCM_MSG_TYPE) && msg.getString(Constants.KEY_GCM_MSG_TYPE).equals(Constants
                        .GcmMessageType.notify_friend_in.name())) {
                    notifyFriendJumpedIn(msg.getString(Constants.KEY_USERNAME));
                } else if (msg.has(Constants.KEY_GCM_MSG_TYPE) && msg.getString(Constants.KEY_GCM_MSG_TYPE).equals
                        (Constants.GcmMessageType.msg.name())) {
                    final FriendlyMessage friendlyMessage = FriendlyMessage.fromJSONObject(msg);

                    if (PrivateChatActivity.getActiveUserid() == friendlyMessage.getUserid()) {
                    /* Already actively chatting with this person in the PrivateChatActivity
                     * so no need to put up a notification in the system tray.
                     * For debugging purposes, however, if it's myself, then it's ok.
                     */
                        if (PrivateChatActivity.getActiveUserid() != UserPreferences.getInstance().getUserId()) {
                            MLog.d(TAG, "already actively chatting, do NOT notify");
                            return;
                        }
                    }

                    final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF
                            () + friendlyMessage.getUserid());
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            MLog.d(TAG, "onDataChange() snapshot: " + dataSnapshot, " ref: ", ref);
                            if (dataSnapshot.getValue() == null) {
                                MLog.d(TAG, "user ", friendlyMessage.getName(), " is not blocked.  consume message " +
                                        "now. ");
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

        //Glide.with(TheApp.getInstance()).loa
    }

    private void consumeFriendlyMessage(final FriendlyMessage friendlyMessage) {
        incrementPrivateUnreadMessages(friendlyMessage);
        Observable<Bitmap> observable = Observable.fromCallable(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return Glide.
                        with(InstachatMessagingService.this).
                        asBitmap().
                        load(friendlyMessage.getDpid()).
                        transform(new CircleTransform(InstachatMessagingService.this)).
                        into(thumbSize(), thumbSize()). // Width and height
                        get();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());

        observable.subscribe(new Consumer<Bitmap>() {
            @Override
            public void accept(Bitmap bitmap) throws Exception {
                showNotification(friendlyMessage, bitmap);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                showNotification(friendlyMessage, null);
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
        Intent intent = DirectReplyActivity.newIntent(this, friendlyMessage.getUserid(), friendlyMessage.getName(),
                friendlyMessage.getDpid());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, friendlyMessage.getUserid(), intent, 0);

        Uri customSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getPackageName() + "/raw/sound_yourturn");

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_dialog_email,
                        getString(R.string.reply), pendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        String contentText = "";
        if (friendlyMessage.getMT() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME && TextUtils.isEmpty
                (friendlyMessage.getText()))
            contentText = getString(R.string.one_time_photo_notification_title);
        else if (friendlyMessage.getMT() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME && !TextUtils.isEmpty
                (friendlyMessage.getText()))
            contentText = getString(R.string.one_time_message_notification_title);
        else if (TextUtils.isEmpty(friendlyMessage.getText()))
            contentText = getString(R.string.photo);
        else
            contentText = friendlyMessage.getText() + "";

        MLog.d(TAG, "contentText: " + contentText);

        // Use NotificationCompat.Builder to set up our notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID_NEW_MSGS);
        builder.addAction(replyAction)
                .setContentTitle(friendlyMessage.getName() + " " + getString(R.string.said))
                .setSmallIcon(R.drawable.ic_stat_ic_message_white_18dp)
                .setLargeIcon(bitmap == null ? BitmapFactory.decodeResource(getResources(), R.drawable
                        .ic_anon_person_36dp) : bitmap)
                .setContentIntent(pendingIntent)
                .setSound(customSound)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setContentText(contentText)
                .setSubText(friendlyMessage.getName());


        //icon appears in device notification bar and right hand corner of notification
        //builder.setSmallIcon(R.drawable.ic_stat_ic_message_white_18dp);

        // Set the intent that will fire when the user taps the notification.
        //builder.setContentIntent(pendingIntent);

        // Large icon appears on the left of the notification

        // Content title, which appears in large type at the top of the notification
        //builder.setContentTitle(friendlyMessage.getName());

        // Content text, which appears in smaller text below the title

        //builder.setContentText(contentText);
        // The subtext, which appears under the text on newer devices.
        // This will show-up in the devices with Android 4.2 and above only
        //builder.setSubText(getString(R.string.sent_via, c.getString(R.string.app_name)));

        checkIfIAcceptedThisPersonYet(friendlyMessage, builder.build(), contentText);

    }

    private int thumbSize() {
        return getResources().getDimensionPixelSize(R.dimen.user_thumb_pic_size);
    }

    private void incrementPrivateUnreadMessages(final FriendlyMessage friendlyMessage) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants
                .MY_PRIVATE_CHATS_SUMMARY_PARENT_REF());
        Map<String, Object> map = new HashMap<>(1);
        map.put("id", friendlyMessage.getId());
        ref.child(friendlyMessage.getUserid() + "").child(Constants.CHILD_UNREAD_MESSAGES).child(friendlyMessage
                .getId()).updateChildren(map);
    }

    private void checkIfIAcceptedThisPersonYet(final FriendlyMessage friendlyMessage, final Notification
            notification, final String notificationText) {

        final int myUserid = UserPreferences.getInstance().getUserId();
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
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants
                .MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).child(friendlyMessage.getUserid() + "").child(Constants
                .CHILD_ACCEPTED);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ref.removeEventListener(this);
                if (dataSnapshot.exists() && dataSnapshot.getValue(Boolean.class)) {
                    //I accepted this person

                    //Display the notification in the notification bar
                    notificationManager.notify(friendlyMessage.getUserid(), notification);
                    MLog.d(TAG, "this should have notified and I'm in a UI thread too user id? " +
                            friendlyMessage.getUserid());
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
                    FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_REQUEST_STATUS_PARENT_REF
                            (friendlyMessage.getUserid(), myUserid))
                            .setValue(privateChatSummary)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    notifyPendingRequests();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ref.removeEventListener(this);
            }
        });

    }

    private void notifyFriendJumpedIn(String username) {

        /**
         * fix bug from older version
         */
        try {
            if (username.equals(UserPreferences.getInstance().getUsername())) {
                return;
            }
        } catch (Exception e) {
        }

        Uri customSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getPackageName() + "/raw/arpeggio");
        // This intent is fired when notification is clicked
        Intent intent = new Intent(this, LauncherActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_FRIEND_JUMPED_IN, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Use NotificationCompat.Builder to set up our notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID_ACTIVITY);
        builder
                .setStyle(new NotificationCompat.BigTextStyle())
                .setContentTitle(username)
                .setContentText(getString(R.string.friend_just_jumped_in))
                .setSmallIcon(R.drawable.ic_stat_ic_message_white_18dp)
                .setContentIntent(pendingIntent)
                .setSound(customSound)
                .setSubText(getString(R.string.sent_via) + " " + getString(R.string.app_name));

        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_FRIEND_JUMPED_IN, builder.build());
    }

    /**
     * Simply notify user that there are pending requests from other users wanting
     * to chat with him.  Do not use HIGH priority
     * User can open the app and decide whether to accept or not
     */
    private void notifyPendingRequests() {

        Uri customSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getPackageName() + "/raw/sound_i_demand_attention");

        // This intent is fired when notification is clicked
        Intent intent = new Intent(this, LauncherActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_FRIEND_JUMPED_IN, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Use NotificationCompat.Builder to set up our notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID_ACTIVITY);
        builder.setStyle(new NotificationCompat.BigTextStyle())
                .setContentTitle(getString(R.string.pending_requests_title))
                .setContentText(getString(R.string.pending_requests_text))
                .setSmallIcon(R.drawable.ic_stat_ic_message_white_18dp)
                .setContentIntent(pendingIntent)
                .setSound(customSound)
                .setSubText(getString(R.string.sent_via) + " " + getString(R.string.app_name));
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_PENDING_REQUESTS, builder.build());
        FirebaseAnalytics.getInstance(this).logEvent(Events.PENDING_REQUEST_ISSUED, null);
    }

    @Override
    public void onNewToken(final String token) {
        MLog.d(TAG, "kevinFirbaseMessaging. onNewToken: " + token);
        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                GCMRegistrationManager.saveRegistrationId(InstachatMessagingService.this, token);
            }
        });
    }
}
