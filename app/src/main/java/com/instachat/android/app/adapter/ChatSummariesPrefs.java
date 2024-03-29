package com.instachat.android.app.adapter;

import android.content.Context;
import android.content.SharedPreferences;

import com.instachat.android.TheApp;

/**
 * Created by kevin on 11/22/2016.
 */

public final class ChatSummariesPrefs {

    private ChatSummariesPrefs() {
    }

    private static final String PREFS = "chat_summaries";
    private static SharedPreferences chatSummariesPrefs;
    private static final long THIRTY_MINUTES = 1000 * 60 * 30L;

    public static boolean isNotifiedRecently(String userid) {
        if (chatSummariesPrefs == null) {
            chatSummariesPrefs = TheApp.getInstance().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
        long last = chatSummariesPrefs.getLong(userid, 0);
        return System.currentTimeMillis() - last < THIRTY_MINUTES;
    }

    public static void updateLastNotifiedTime(String userid) {
        if (chatSummariesPrefs == null) {
            chatSummariesPrefs = TheApp.getInstance().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
        chatSummariesPrefs.edit().putLong(userid, System.currentTimeMillis()).apply();
    }
}
