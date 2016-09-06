package com.initech.api;

import java.io.File;

/**
 * Created by kevin on 9/5/2016.
 */
public interface UploadListener {
    void onErrorReducingPhotoSize();

    void onPhotoUploadStarted();

    void onPhotoUploadProgress(int max, int current);

    void onPhotoUploadSuccess(String photoId, String photoUrl);

    void onPhotoUploadError(Exception exception);
}
