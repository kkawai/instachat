package com.instachat.android.gcm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.instachat.android.Constants;
import com.instachat.android.Events;
import com.instachat.android.util.AnalyticsHelper;
import com.instachat.android.util.MLog;

import org.json.JSONObject;

/**
 * https://developer.android.com/google/gcm/client.html#sample-registerIfNecessary
 *
 * @author kkawai
 */
public final class GCMIntentService extends IntentService {

    private static final String TAG = GCMIntentService.class.getSimpleName();

    public GCMIntentService() {
        super(TAG);
    }

    private void onMessage(final Context context, final Intent data) throws Exception {

        final JSONObject msg = new JSONObject(data.getStringExtra(Constants.KEY_MESSAGE));
        MLog.i(TAG, "onMessage: ", msg.toString());
        //final org.jivesoftware.smack.packet.Message message = MessageUtils.toXMPPMessage(msg);
        //XMPPService.consumeMessage(context, message);
        //TODO
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        try {
            final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            final String messageType = gcm.getMessageType(intent);
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                onMessage(this, intent);
            }
        } catch (final Exception e) {
            MLog.e(TAG, "onHandleIntent() failed to receive messages ", e);
            AnalyticsHelper.logException(Events.GCM_INCOMING_MSG_FAIL, e);
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GCMBroadcastReceiver.completeWakefulIntent(intent);
    }
}
