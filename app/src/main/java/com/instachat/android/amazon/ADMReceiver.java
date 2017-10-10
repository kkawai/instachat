package com.instachat.android.amazon;

import android.content.Intent;
import android.os.Bundle;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;
import com.instachat.android.Constants;
import com.instachat.android.MyApp;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;

import org.json.JSONObject;

public class ADMReceiver extends ADMMessageHandlerBase {

    private static final String TAG = ADMReceiver.class.getSimpleName();

    public ADMReceiver() {
        /** {@inheritDoc} */
        super(ADMReceiver.class.getSimpleName());
    }

    public ADMReceiver(String arg) {
        super(arg);
    }

    public static class Receiver extends ADMMessageReceiver {
        public Receiver() {
            super(ADMReceiver.class);
        }

        // Nothing else is required here; your broadcast receiver automatically
        // forwards intents to your service for processing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRegistered(final String registrationId) {
        MLog.i(TAG, "onRegistered() " + registrationId);
        new Thread() {
            public void run() {
                try {
                    /*
                     * VERY Important: the prepended ' ' character signifies to
					 * server that this is for ADM, not GCM 
					 */
                    NetworkApi
                            .gcmreg(MyApp.getInstance(), " " + registrationId);
                    MLog.i(TAG, "onRegistered: " + registrationId);
                } catch (final Exception e) {
                    MLog.e(TAG, "adm error", e);
                }
            }
        }.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUnregistered(final String registrationId) {
        MLog.i(TAG, "onUnregistered()");
        // If your app is unregistered on this device, inform your server that
        // this app instance is no longer a valid target for messages.

        new Thread() {
            public void run() {
                try {
                    /*
					 * VERY Important: the prepended ' ' character signifies to
					 * server that this is for ADM, not GCM 
					 */
                    NetworkApi.gcmunreg(MyApp.getInstance(), Preferences.getInstance().getUserId() + "", " " + registrationId);
                    MLog.i(TAG, "onUnregistered: " + registrationId);
                } catch (final Exception e) {
                    MLog.e(TAG, "adm error", e);
                }
            }
        }.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRegistrationError(final String errorId) {
        MLog.i(TAG, "onRegistrationError()");
        // You should consider a registration error fatal. In response, your app
        // may
        // degrade gracefully, or you may wish to notify the user that this part
        // of
        // your app's functionality is not available.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(final Intent intent) {
        MLog.i(TAG, "onMessage()");
        // Extract the message content from the set of extras attached to
        // the com.amazon.device.messaging.intent.RECEIVE intent.

        // Create strings to access the message and timeStamp fields from the
        // JSON data.
        //final String msgKey = getString(R.string.json_data_msg_key);
        //final String timeKey = getString(R.string.json_data_time_key);


        // Obtain the intent action that will be triggered in onMessage()
        // callback.
        //final String intentAction = getString(R.string.intent_msg_action);

        // Obtain the extras that were included in the intent.
        final Bundle extras = intent.getExtras();

        // Extract the message and time from the extras in the intent.
        // ADM makes no guarantees about delivery or the order of messages.
        // Due to varying network conditions, messages may be delivered more
        // than once.
        // Your app must be able to handle instances of duplicate messages.
        final String msg = extras.getString(Constants.KEY_GCM_MESSAGE);
        //final String data = extras.getString("data");
        //final String time = extras.getString(timeKey);

        //MLog.i(TAG, "msg: " + msg + " data: " + data);

        //if (1==1) return;

        try {
            final JSONObject msgJ = new JSONObject(msg);
            //MLog.i(TAG, "onMessage: ", msg.toString());
            //final org.jivesoftware.smack.packet.Message message = MessageUtils.toXMPPMessage(msgJ);

            //XMPPService.consumeMessage(InstachatApp.instance, message);
            //TODO
        } catch (final Exception e) {
            MLog.e(TAG, "onMessage() error", e);
        }
    }

}
