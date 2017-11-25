package com.instachat.android.app.activity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.data.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;

import javax.inject.Inject;

/**
 * Created by kevin on 10/14/2016.
 */

public class PresenceHelper {

    private static final String TAG = "PresenceHelper";

    private DatabaseReference connectedRef;
    private ValueEventListener listener;
    private final FirebaseDatabase firebaseDatabase;

    @Inject
    public PresenceHelper(FirebaseDatabase firebaseDatabase) {
        this.firebaseDatabase = firebaseDatabase;
    }

    public void updateLastActiveTimestamp() {
        // since I can connect from multiple devices, we store each connection instance separately
        // any time that connectionsRef's value is null (i.e. has no children) I am offline
        // stores the timestamp of my last disconnect (the last time I was seen online)
        final User me = UserPreferences.getInstance().getUser();
        final DatabaseReference userInfoRef = firebaseDatabase.getReference(Constants.USER_INFO_REF(me.getId()));

        connectedRef = firebaseDatabase.getReference(".info/connected");
        connectedRef.addValueEventListener(listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                MLog.d(TAG, "onDataChange() snapshot: ", snapshot);
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    // add this device to my connections list
                    // this value could contain info about the device or a timestamp too
                    //myConnectionsRef.setValue(Boolean.TRUE);

                    //myConnectionsRef.onDisconnect().removeValue();
                    //userInfoRef.updateChildren(me.toMap(true));
                    userInfoRef.child(Constants.CHILD_LAST_ONLINE).setValue(ServerValue.TIMESTAMP);
                    // when I disconnect, update the last time I was seen online
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                MLog.d(TAG, "onCancelled() error: ", error);
            }
        });
    }

    public void cleanup() {
        if (connectedRef != null && listener != null) {
            connectedRef.removeEventListener(listener);
        }
    }
}
