package com.google.firebase.codelab.friendlychat.messaging;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.codelab.friendlychat.PrivateChatActivity;
import com.google.firebase.codelab.friendlychat.R;
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.initech.Constants;
import com.initech.MyApp;
import com.initech.util.MLog;
import com.initech.util.ThreadWrapper;

import org.json.JSONObject;

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
            if (object.has("msg")) {
                JSONObject msg = new JSONObject(object.getString("msg"));
                final FriendlyMessage friendlyMessage = FriendlyMessage.fromJSONObject(msg);

                if (PrivateChatActivity.isActive() && PrivateChatActivity.getActiveUserid() == friendlyMessage.getUserid()) {
                    //already actively chatting with this person in the PrivateChatActivity
                    //so no need to put up a notification in the system tray
                    return;
                }

                Constants.DP_URL(friendlyMessage.getUserid(), friendlyMessage.getDpid(), new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(final @NonNull Task<Uri> task) {
                        try {
                            if (!task.isSuccessful()) {
                                showNotification(friendlyMessage, null);
                                return;
                            }
                            ThreadWrapper.executeInWorkerThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Bitmap bitmap = Glide.
                                                with(MyApp.getInstance()).
                                                load(task.getResult().toString()).
                                                asBitmap().
                                                into(thumbSize(), thumbSize()). // Width and height
                                                get();
                                        showNotification(friendlyMessage, bitmap);
                                    } catch (Exception e) {
                                        MLog.e(TAG, "", e); //todo better error handling/message
                                        showNotification(friendlyMessage, null);
                                    }
                                }
                            });

                        } catch (Exception e) {
                            MLog.e(TAG, "", e); //todo better error handling/message
                            showNotification(friendlyMessage, null);
                        }
                    }
                });
            }
        } catch (Exception e) {
            MLog.e(TAG, "", e); //todo better error handling/message
        }

        //Glide.with(MyApp.getInstance()).loa
    }

    private void showNotification(FriendlyMessage friendlyMessage, Bitmap bitmap) {

        // Use NotificationCompat.Builder to set up our notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        //icon appears in device notification bar and right hand corner of notification
        builder.setSmallIcon(R.drawable.ic_stat_ic_message_white_18dp);

        // This intent is fired when notification is clicked
        Intent intent = PrivateChatActivity.createPrivateChatActivityIntent(MyApp.getInstance(), friendlyMessage.getUserid());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(pendingIntent);

        // Large icon appears on the left of the notification
        if (bitmap == null) {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_account_circle_black_36dp));
        } else {
            builder.setLargeIcon(bitmap);
        }

        Uri customSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getPackageName() + "/raw/sound_yourturn");

        builder.setSound(customSound);

        // Content title, which appears in large type at the top of the notification
        builder.setContentTitle(friendlyMessage.getName());

        // Content text, which appears in smaller text below the title
        builder.setContentText(friendlyMessage.getText() + "");

        // The subtext, which appears under the text on newer devices.
        // This will show-up in the devices with Android 4.2 and above only
        Context c = MyApp.getInstance();
        builder.setSubText(c.getString(R.string.sent_via, c.getString(R.string.app_name)));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Will display the notification in the notification bar
        notificationManager.notify(friendlyMessage.getUserid(), builder.build());
    }

    private int thumbSize() {
        return MyApp.getInstance().getResources().getDimensionPixelSize(R.dimen.message_thumb_pic_size);
    }
}
