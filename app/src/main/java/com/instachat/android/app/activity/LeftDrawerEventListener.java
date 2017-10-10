package com.instachat.android.app.activity;

/**
 * Created by kevin on 10/10/2016.
 */

public interface LeftDrawerEventListener {
    void onProfilePicChangeRequest(boolean isLaunchCamera);
    void onPendingRequestsClicked();
    void onPendingRequestsAvailable();
    void onPendingRequestsCleared();
}
