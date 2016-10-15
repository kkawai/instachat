package com.instachat.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by kevin on 9/19/2016.
 */
public class ChatTypingHelper extends BroadcastReceiver {

    private IntentFilter mFilter = new IntentFilter(Constants.ACTION_USER_TYPING);
    private UserTypingListener mListener;

    public interface UserTypingListener {
        void onRemoteUserTyping(int userid);
    }

    public void setListener(UserTypingListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int userid = intent.getIntExtra(Constants.KEY_USERID, 0);
        mListener.onRemoteUserTyping(userid);
    }

    public void register() {
        LocalBroadcastManager.getInstance(MyApp.getInstance()).registerReceiver(this, mFilter);
    }

    public void unregister() {
        LocalBroadcastManager.getInstance(MyApp.getInstance()).unregisterReceiver(this);
    }
}
