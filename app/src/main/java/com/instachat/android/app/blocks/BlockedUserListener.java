package com.instachat.android.app.blocks;

/**
 * Created by kevin on 10/14/2016.
 */

public interface BlockedUserListener {
    void onUserBlocked(int userid);
    void onUserUnblocked(int userid);
}
