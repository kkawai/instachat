package com.instachat.android.data.api;

/**
 * Created by kevin on 9/5/2016.
 */
public interface UploadListener {
    void onErrorReducingPhotoSize();

    void onPhotoUploadStarted();

    void onPhotoUploadProgress(int max, int current);

    void onPhotoUploadSuccess(String photoUrl, boolean isPossibleAdult, boolean isPossibleViolence);

    void onPhotoUploadError(Exception exception);
}
