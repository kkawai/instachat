package com.google.firebase.codelab.friendlychat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.initech.Constants;
import com.initech.api.FileUploadApi;
import com.initech.api.NetworkApi;
import com.initech.api.UploadListener;
import com.initech.model.User;
import com.initech.util.ImageUtils;
import com.initech.util.LocalFileUtils;
import com.initech.util.MLog;
import com.initech.util.Preferences;
import com.initech.util.ThreadWrapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by kevin on 9/4/2016.
 * Helps upload a single file to a destination
 */
public class ProfilePicUploadHelper {
    private static final String TAG = "ProfilePicUploadHelper";
    private static final int RC_CHOOSE_PICTURE = 203;
    private static final int RC_TAKE_PICTURE = 201;
    private static final int RC_STORAGE_PERMS = 202;
    private Uri mFileUri = null;
    private File mFile = null;
    private Activity mActivity;
    private UploadListener mListener;

    ProfilePicUploadHelper(Activity activity) {
        mActivity = activity;
    }

    public void setPhotoUploadListener(UploadListener listener) {
        mListener = listener;
    }

    public void cleanup() {
        mActivity = null;
        mListener = null;
    }

    @AfterPermissionGranted(RC_STORAGE_PERMS)
    public void launchCamera(boolean isChoose) {
        Log.d(TAG, "launchCamera");

        // Check that we have permission to read images from external storage.
        if (!isChoose) {
            String perm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (!EasyPermissions.hasPermissions(mActivity, perm)) {
                EasyPermissions.requestPermissions(this, mActivity.getString(R.string.rationale_storage),
                        RC_STORAGE_PERMS, perm);
                return;
            }
        } else {
            String perm = android.Manifest.permission.READ_EXTERNAL_STORAGE;
            if (!EasyPermissions.hasPermissions(mActivity, perm)) {
                EasyPermissions.requestPermissions(this, mActivity.getString(R.string.rationale_storage),
                        RC_STORAGE_PERMS, perm);
                return;
            }
        }

        // Choose file storage location, must be listed in res/xml/file_paths.xml
        File dir = new File(Environment.getExternalStorageDirectory() + "/photos");
        mFile = new File(dir, "me.jpg");
        try {
            // Create directory if it does not exist.
            if (!dir.exists()) {
                dir.mkdir();
            }
            boolean created = mFile.createNewFile();
            Log.d(TAG, "file.createNewFile:" + mFile.getAbsolutePath() + ":" + created);
        } catch (IOException e) {
            Log.e(TAG, "file.createNewFile" + mFile.getAbsolutePath() + ":FAILED", e);
        }

        // Create content:// URI for file, required since Android N
        // See: https://developer.android.com/reference/android/support/v4/content/FileProvider.html
        mFileUri = FileProvider.getUriForFile(mActivity,
                "com.google.firebase.quickstart.firebasestorage.fileprovider", mFile);

        if (!isChoose) {
            // Create and launch the intent
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
            mActivity.startActivityForResult(takePictureIntent, RC_TAKE_PICTURE);
        } else {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            mActivity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), RC_CHOOSE_PICTURE);
        }
    }

    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == RC_TAKE_PICTURE) {
            launchCamera(false);
        } else if (requestCode == RC_CHOOSE_PICTURE) {
            launchCamera(true);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (mFileUri != null) {
                    reducePhotoSize(null);
                }
            }
        } else if (requestCode == RC_CHOOSE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (mFileUri != null) {
                    reducePhotoSize(data.getData());
                }
            }
        }
    }

    private void reducePhotoSize(final Uri uri) {
        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                try {

                    if (uri != null) {
                        LocalFileUtils.copyFile(mActivity, uri, mFile);
                    }

                    final Bitmap bitmap = ImageUtils.getBitmap(mActivity, mFileUri, Constants.MAX_PROFILE_PIC_SIZE_BYTES);
                    ImageUtils.writeBitmapToFile(bitmap, mFile);
                    uploadFromUri(mFile);
                } catch (final Exception e) {
                    MLog.e(TAG, "reducePhotoSize() failed", e);
                    mListener.onErrorReducingPhotoSize();
                }
            }
        });
    }

    private void uploadFromUri(final File file) {
        try {
            final String dpid = UUID.randomUUID().toString();
            final String dp = "dp_" + Preferences.getInstance().getUserId() + "_" + dpid;
            if (isActivityDestroyed())
                return;
            new FileUploadApi().postFileToS3(file, dp, Constants.AMAZON_BUCKET_DP_IC, mListener);
            final User user = Preferences.getInstance().getUser();
            user.setProfilePicUrl(dpid);
            Preferences.getInstance().saveUser(user);
            NetworkApi.saveUser(null, user, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    MLog.d(TAG, "saveUser() success via uploadFromUri(): " + response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    MLog.e(TAG, "saveUser() failed via uploadFromUri() ", error);
                }
            });
        } catch (Exception e) {
            MLog.e(TAG, "postFileToS3() failed", e);
            mListener.onPhotoUploadError(e);
        }
    }

    private boolean isActivityDestroyed() {
        return mActivity == null || mActivity.isFinishing();
    }
}
