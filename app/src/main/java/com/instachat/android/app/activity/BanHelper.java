package com.instachat.android.app.activity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.util.UserPreferences;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class BanHelper {

    private int inquiries;
    private boolean isBanned;
    private final FirebaseDatabase firebaseDatabase;
    private DatabaseReference bannedRef;

    @Inject
    public BanHelper(FirebaseDatabase firebaseDatabase) {
        this.firebaseDatabase = firebaseDatabase;
    }

    public void ban(FriendlyMessage friendlyMessage, int minutes) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.BANS+friendlyMessage.getUserid());
        Map<String, Object> map = new HashMap<>(10);
        map.put("username", friendlyMessage.getName());
        map.put("banExpiration", System.currentTimeMillis() + (1000L*60*minutes));
        map.put("nice", new Date(System.currentTimeMillis() + (1000L*60*minutes)).toString());
        ref.updateChildren(map);
    }

    /**
     * Return if user is banned.  To conserve resources
     * check database every 6 inquires, not every every time.
     * @return
     */
    public boolean isBanned() {
        if (inquiries % 6 == 0) {
            inquiries = 1;
            checkBan();
        } else {
            inquiries++;
        }
        return isBanned;
    }

    /**
     * Check if I am banned
     */
    private void checkBan() {
        if (bannedRef == null) {
            bannedRef = firebaseDatabase.getReference(Constants.BANS + UserPreferences.getInstance().getUserId());
        }
        bannedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                bannedRef.removeEventListener(this);
                if (dataSnapshot.exists()) {
                    long banExpiration = (Long)dataSnapshot.child("banExpiration").getValue();
                    isBanned = banExpiration > System.currentTimeMillis();
                    if (!isBanned) {
                        bannedRef.removeValue();
                    }
                } else {
                    isBanned = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                bannedRef.removeEventListener(this);
            }
        });

    }
}
