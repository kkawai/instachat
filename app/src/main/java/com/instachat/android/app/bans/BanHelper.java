package com.instachat.android.app.bans;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.util.DeviceUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class BanHelper {

    public static final String TAG = "BanHelper";

    private boolean isBanned;
    private final FirebaseDatabase firebaseDatabase;
    private DatabaseReference bannedRef;

    @Inject
    public BanHelper(FirebaseDatabase firebaseDatabase) {
        this.firebaseDatabase = firebaseDatabase;
        MLog.d(TAG,"Created BanHelper");
        try {
            checkBan();
        }catch (Exception e) {
            MLog.e(TAG,"checkBan failed:",e);
        }
    }

    public static void ban(final FriendlyMessage friendlyMessage) {
        FirebaseDatabase.getInstance()
                .getReference(Constants.USER_INFO_REF(friendlyMessage.getUserid())+"/d")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String deviceId = (String)dataSnapshot.getValue();
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.BANS+deviceId);
                            Map<String, Object> map = new HashMap<>(10);
                            map.put("username", friendlyMessage.getName());
                            map.put("id", friendlyMessage.getUserid());
                            map.put("dpid", friendlyMessage.getDpid());
                            //map.put("banExpiration", System.currentTimeMillis() + (1000L*60*minutes));
                            map.put("admin", UserPreferences.getInstance().getUsername());
                            map.put("adminId", UserPreferences.getInstance().getUserId());
                            ref.updateChildren(map);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    public void ban(final FriendlyMessage friendlyMessage, int minutes) {

        if (!FirebaseAuth.getInstance().getCurrentUser().getEmail().equals(UserPreferences.getInstance().getEmail())) {
            return;
        }

        ban(friendlyMessage);
    }

    public static void unban(int userid, final OnCompleteListener onCompleteListener) {

        if (!FirebaseAuth.getInstance().getCurrentUser().getEmail().equals(UserPreferences.getInstance().getEmail())) {
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference(Constants.USER_INFO_REF(userid)+"/d")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String deviceId = (String)dataSnapshot.getValue();
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.BANS+deviceId);
                            ref.removeValue().addOnCompleteListener(onCompleteListener);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    /*private void getDeviceId(int userId) {
        FirebaseDatabase.getInstance()
                .getReference(Constants.USER_INFO_REF(userId)+"/d").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    MLog.d(TAG, "getDeviceId() " + dataSnapshot.getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }*/

    /**
     * Return if user is banned.  To conserve resources
     * check database every 6 inquires, not every every time.
     * @return
     */
    public boolean isBanned() {
        if (isBanned == false) {
            checkBan(); //check every time because I can get banned at any time
        }
        return isBanned;
    }

    private ValueEventListener singleValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            bannedRef.removeEventListener(this);
            if (dataSnapshot.exists()) {
//                    long banExpiration = (Long)dataSnapshot.child("banExpiration").getValue();
//                    isBanned = banExpiration > System.currentTimeMillis();
//                    if (!isBanned) {
//                        bannedRef.removeValue();
//                    }
                isBanned = true;
            } else {
                isBanned = false;
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            bannedRef.removeEventListener(this);
        }
    };

    /**
     * Check if I am banned
     */
    private void checkBan() {
        //getDeviceId(UserPreferences.getInstance().getUserId());
        if (bannedRef == null) {
            bannedRef = firebaseDatabase.getReference(Constants.BANS + DeviceUtil.getFirebaseUid());
        }
        bannedRef.addListenerForSingleValueEvent(singleValueEventListener);
    }
}
