package com.instachat.android;

/**
 * Created by kkawai on 12/5/16.
 */

public interface UsersInGroupListener {
   void onNumUsersUpdated(long groupId, String groupName, int numUsers);
}
