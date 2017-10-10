package com.instachat.android.app.adapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.app.activity.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kevin on 12/13/2016.
 * <p>
 * Since firebase is callback based, we don't know when all the data will be returned.  Data comes
 * back callback by callback, so we don't have a list of data.
 * <p>
 * Manage a list of data and act on it in time chunks to avoid OOM and over taxing the cpu
 * especially if you have a lot of contacts (>200).
 */

public class UserPresenceManager {

    public static final String TAG = "UserPresenceManager";
    private Map<String, PrivateChatSummary> mWorkMap = new HashMap<>(200);
    private Thread mTimer;
    private boolean isTimerRunning;
    private boolean isNotifyOthers;
    private ActivityState mActivityState;

    public UserPresenceManager(ActivityState activityState, boolean isNotifyOthers) {
        this.isNotifyOthers = isNotifyOthers;
        mActivityState = activityState;
    }

    public void queue(PrivateChatSummary privateChatSummary) {

        /**
         * when the first item is added, start a timer.
         * If 80 items are added or 3 seconds has elapsed (whichever comes first)
         * then process all the items in batches of 80.  Stop the timer when processing
         * has begun.
         * When all items have been processed, remove them from the queue and
         * reset the timer back to zero.
         *
         * If new items come in while processing, check if they are already being
         * processed. If not, add them to a holding queue.  When processing has finished
         * add the holding queue items to the main queue and start the timer.
         */
        synchronized (UserPresenceManager.this) {
            if (mWorkMap.containsKey(privateChatSummary.getId())) {
                return;
            }
            mWorkMap.put(privateChatSummary.getId(), privateChatSummary);
        }

        if (isTimerRunning)
            return;

        isTimerRunning = true;

        if (mTimer != null) //kill off any existing process
            mTimer.interrupt();

        mTimer = new Thread() {
            @Override
            public void run() {
                while (isTimerRunning) {
                    try {
                        Thread.sleep(3000);
                        process();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
        mTimer.start();
    }

    private void process() {

        if (mActivityState == null || mActivityState.isActivityDestroyed())
            return;

        Map<String, PrivateChatSummary> mCopy = new HashMap<>(200);
        synchronized (UserPresenceManager.this) {
            mCopy.putAll(mWorkMap);
        }

        //get online status for the queue
        try {
            NetworkApi.getOnlineStatus(mCopy);
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }

        //update the UI and notify (if applicable)
        for (String key : mCopy.keySet()) {

            if (mActivityState == null || mActivityState.isActivityDestroyed())
                return;

            final PrivateChatSummary privateChatSummary = mCopy.get(key);

            FirebaseDatabase.getInstance().
                    getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).
                    child(privateChatSummary.getId()).
                    updateChildren(privateChatSummary.toMap());

            //Now, see if this user accepted me. If yes, then notify them that
            //I just got online
            if (!isNotifyOthers)
                continue;

            if (privateChatSummary.getOnlineStatus() != PrivateChatSummary.USER_OFFLINE && Integer.parseInt(privateChatSummary.getId()) != Preferences.getInstance().getUserId()) {

                if (!ChatSummariesPrefs.isNotifiedRecently(privateChatSummary.getId())) {

                    FirebaseDatabase.getInstance().getReference("/users/" + privateChatSummary.getId() + "/private_summaries/" + Preferences.getInstance().getUserId() + "/accepted").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getValue(Boolean.class)) {
                                try {
                                    JSONObject msg = new JSONObject();
                                    msg.put(Constants.KEY_USERNAME, Preferences.getInstance().getUsername());
                                    NetworkApi.gcmsend(Integer.parseInt(privateChatSummary.getId()), Constants.GcmMessageType.notify_friend_in, msg);
                                    ChatSummariesPrefs.updateLastNotifiedTime(privateChatSummary.getId());
                                } catch (Exception e) {
                                    MLog.e(TAG, "", e);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
        }//for
        MLog.d(TAG, "process() processed ", mCopy.size(), " users.  mWorkMap.size() ", mWorkMap.size());
        synchronized (UserPresenceManager.this) {
            for (String key : mCopy.keySet()) {
                mWorkMap.remove(key);
            }
            if (mWorkMap.size() == 0)
                isTimerRunning = false;
        }
    }

    public void cleanup() {
        if (mTimer != null)
            mTimer.interrupt();
        isTimerRunning = false;
    }

}
