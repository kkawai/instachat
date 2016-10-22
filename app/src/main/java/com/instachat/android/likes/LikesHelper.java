package com.instachat.android.likes;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;

/**
 * Created by kevin on 10/22/2016.
 */

public class LikesHelper {

    private static final String TAG = "LikesHelper";

    private static LikesHelper instance = new LikesHelper();

    public static LikesHelper getInstance() {
        return instance;
    }

    public void likeFriendlyMessage(final FriendlyMessage friendlyMessage) {
        final User me = Preferences.getInstance().getUser();
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MESSAGE_LIKES_REF(friendlyMessage.getId())).
                child(me.getId() + "");
        //check if I already liked it, if so increment my counter
        //otherwise set value
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ref.removeEventListener(this);
                if (dataSnapshot.getValue() == null) {
                    me.setLikes(1);
                    ref.updateChildren(me.getLikesMap());
                } else {
                    User user = dataSnapshot.getValue(User.class);
                    user.incrementLikes();
                    ref.updateChildren(me.getLikesMap());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                MLog.e(TAG, "likeFriendlyMessage() failed when trying to set user ", databaseError);
            }
        });

        //increment likes for both giver and receiver
        final DatabaseReference likesTotalGivenRef = FirebaseDatabase.getInstance().getReference(Constants.USER_TOTAL_GIVEN_LIKES_REF(me.getId()));
        likesTotalGivenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                likesTotalGivenRef.removeEventListener(this);
                if (dataSnapshot.getValue() == null) {
                    likesTotalGivenRef.setValue(1);
                } else {
                    likesTotalGivenRef.setValue(((Integer) dataSnapshot.getValue()) + 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                MLog.e(TAG, "likeFriendlyMessage() failed when trying to increment likes given ", databaseError);
            }
        });

        final DatabaseReference likesTotalReceivedRef = FirebaseDatabase.getInstance().getReference(Constants.USER_TOTAL_LIKES_RECEIVED_REF(friendlyMessage.getUserid()));
        likesTotalReceivedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                likesTotalReceivedRef.removeEventListener(this);
                if (dataSnapshot.getValue() == null) {
                    likesTotalReceivedRef.setValue(1);
                } else {
                    likesTotalReceivedRef.setValue(((Integer) dataSnapshot.getValue()) + 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                MLog.e(TAG, "likeFriendlyMessage() failed when trying to increment likes received ", databaseError);
            }
        });

        final DatabaseReference userReceivedLikesRef = FirebaseDatabase.getInstance().getReference(Constants.USER_RECEIVED_LIKES_REF(friendlyMessage.getUserid())).
                child(me.getId() + "");
        userReceivedLikesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userReceivedLikesRef.removeEventListener(this);
                if (dataSnapshot.getValue() == null) {
                    me.setLikes(1);
                    userReceivedLikesRef.updateChildren(me.getLikesMap());
                } else {
                    User user = dataSnapshot.getValue(User.class);
                    user.incrementLikes();
                    userReceivedLikesRef.updateChildren(user.getLikesMap());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                MLog.e(TAG, "likeFriendlyMessage() failed when trying USER_RECEIVED_LIKES_REF ", databaseError);
            }
        });

        final DatabaseReference userGivenLikesRef = FirebaseDatabase.getInstance().getReference(Constants.USER_GIVEN_LIKES_REF(me.getId())).
                child(friendlyMessage.getUserid() + "");
        userGivenLikesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userGivenLikesRef.removeEventListener(this);
                if (dataSnapshot.getValue() == null) {
                    userGivenLikesRef.updateChildren(friendlyMessage.getUserMap());
                } else {
                    User user = dataSnapshot.getValue(User.class);
                    user.incrementLikes();
                    userGivenLikesRef.updateChildren(me.getLikesMap());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                MLog.e(TAG, "likeFriendlyMessage() failed when trying USER_GIVEN_LIKES_REF ", databaseError);
            }
        });

    }
}
