package com.instachat.android.messaging;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.NonNull;

import com.instachat.android.Constants;
import com.instachat.android.R;

/**
 * Created by kevin on 9/18/2017.
 */

public class NotificationHelper {


    public static void createNotificationChannels(@NonNull Context context) {
        createNewMessageChannel(context);
        createActivityChannel(context);
    }

    @TargetApi(26)
    private static void createNewMessageChannel(@NonNull Context context) {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // The id of the channel.
        String id = Constants.CHANNEL_ID_NEW_MSGS;
        // The user-visible name of the channel.
        CharSequence name = context.getString(R.string.channel_new_messages);
        // The user-visible description of the channel.
        String description = "";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        channel.setDescription(description);
        // channel.enableLights(true);
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        //channel.setLightColor(Color.BLUE);
        //channel.enableVibration(true);
        //channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        notificationManager.createNotificationChannel(channel);
    }

    @TargetApi(26)
    private static void createActivityChannel(@NonNull Context context) {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // The id of the channel.
        String id = Constants.CHANNEL_ID_ACTIVITY;
        // The user-visible name of the channel.
        CharSequence name = context.getString(R.string.channel_activity);
        // The user-visible description of the channel.
        String description = "";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        channel.setDescription(description);
        // channel.enableLights(true);
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.

        /*channel.setLightColor(Color.RED);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});*/
        notificationManager.createNotificationChannel(channel);
    }

}
