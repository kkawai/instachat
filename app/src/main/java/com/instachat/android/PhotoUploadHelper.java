package com.instachat.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.instachat.android.api.UploadListener;
import com.instachat.android.util.ImageUtils;
import com.instachat.android.util.LocalFileUtils;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ThreadWrapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by kevin on 9/4/2016.
 * Helps upload a single file to a destination
 */
public class PhotoUploadHelper {
    private static final String TAG = "PhotoUploadHelper";
    private static final int RC_CHOOSE_PICTURE = 103;
    private static final int RC_TAKE_PICTURE = 101;
    private Uri mTargetFileUri = null;
    private File mTargetFile = null;
    private Activity mActivity;
    private UploadListener mListener;
    private StorageReference mStorageRef;
    private String mStorageRefString;
    private PhotoType mPhotoType;
    private File mTempPhotoUploadDir;

    public enum PhotoType {
        chatRoomPhoto, userProfilePhoto
    }

    PhotoUploadHelper(Activity activity) {
        mActivity = activity;
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mTempPhotoUploadDir = new File(Environment.getExternalStorageDirectory() + "/photos");
        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                LocalFileUtils.deleteDirectoryAndContents(mTempPhotoUploadDir, true);
            }
        });
    }

    public void setPhotoType(PhotoType photoType) {
        mPhotoType = photoType;
    }

    public PhotoType getPhotoType() {
        return mPhotoType;
    }

    public void setStorageRefString(String ref) {
        mStorageRefString = ref;
    }

    public void setPhotoUploadListener(UploadListener listener) {
        mListener = listener;
    }

    public void cleanup() {
        mActivity = null;
        mListener = null;
    }

    public void launchCamera(boolean isChoose) {

        // Check that we have permission to read images from external storage.
        if (!isChoose) {
            String perm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (!EasyPermissions.hasPermissions(mActivity, perm)) {
                EasyPermissions.requestPermissions(mActivity, mActivity.getString(R.string.rationale_storage),
                        RC_TAKE_PICTURE, perm);
                return;
            }
        } else {
            String perm = android.Manifest.permission.READ_EXTERNAL_STORAGE;
            if (!EasyPermissions.hasPermissions(mActivity, perm)) {
                EasyPermissions.requestPermissions(mActivity, mActivity.getString(R.string.rationale_storage),
                        RC_CHOOSE_PICTURE, perm);
                return;
            }
        }

        setupTargetFile();

        if (!isChoose) {
            // Create and launch the intent
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mTargetFileUri);
            mActivity.startActivityForResult(takePictureIntent, RC_TAKE_PICTURE);
        } else {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            mActivity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), RC_CHOOSE_PICTURE);
        }
    }

    /**
     * Set up target file and target file Uri; The final image will be stored in the
     * target file and referenced via the Uri.
     */
    private void setupTargetFile() {
        // Choose file storage location, must be listed in res/xml/file_paths.xml
        mTargetFile = new File(mTempPhotoUploadDir, UUID.randomUUID().toString() + ".jpg");
        try {
            // Create directory if it does not exist.
            if (!mTempPhotoUploadDir.exists()) {
                mTempPhotoUploadDir.mkdir();
            }
            boolean created = mTargetFile.createNewFile();
            MLog.d(TAG, "file.createNewFile:" + mTargetFile.getAbsolutePath() + ":" + created);
        } catch (IOException e) {
            Log.e(TAG, "file.createNewFile" + mTargetFile.getAbsolutePath() + ":FAILED", e);
        }

        // Create content:// URI for file, required since Android N
        // See: https://developer.android.com/reference/android/support/v4/content/FileProvider.html
        mTargetFileUri = FileProvider.getUriForFile(mActivity,
                mActivity.getPackageName() + ".fileprovider", mTargetFile);
    }

    /**
     * Consume external ACTION_SEND intent.  In this case, we are receiving
     * some photo uri from outside the app.
     *
     * @param photoUri
     */
    public void consumeExternallySharedPhoto(final Uri photoUri) {
        setupTargetFile();
        mPhotoType = PhotoType.chatRoomPhoto;
        reducePhotoSize(photoUri);

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (mTargetFileUri != null) {
                    reducePhotoSize(null);
                }
            }
        } else if (requestCode == RC_CHOOSE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (mTargetFileUri != null) {
                    reducePhotoSize(data.getData());
                }
            }
        }
    }

    public void onPermissionsGranted(int requestCode, List<String> perms) {
        MLog.i(TAG, "onPermissionsGranted() requestCode: " + requestCode);
        for (int i = 0; perms != null && i < perms.size(); i++) {
            MLog.i(TAG, "onPermissionsGranted() requestCode: " + requestCode, " perm: ", perms.get(i));
        }
        if (requestCode == RC_TAKE_PICTURE) {
            launchCamera(false);
        } else if (requestCode == RC_CHOOSE_PICTURE) {
            launchCamera(true);
        }
    }

    private void reducePhotoSize(final Uri uri) {
        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                try {

                    if (uri != null) {
                        LocalFileUtils.copyFile(mActivity, uri, mTargetFile);
                    }

                    int maxSizeBytes = Constants.MAX_PIC_SIZE_BYTES;
                    if (mPhotoType == PhotoType.chatRoomPhoto) {
                        maxSizeBytes = Constants.MAX_PIC_SIZE_BYTES;
                    } else if (mPhotoType == PhotoType.userProfilePhoto) {
                        maxSizeBytes = Constants.MAX_PROFILE_PIC_SIZE_BYTES;
                    }
                    final Bitmap bitmap = ImageUtils.getBitmap(mActivity, mTargetFileUri, maxSizeBytes);
                    ImageUtils.writeBitmapToFile(bitmap, mTargetFile);
                    if (isActivityDestroyed())
                        return;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isActivityDestroyed())
                                return;
                            uploadFromUri(mTargetFileUri);
                        }
                    });
                } catch (final Exception e) {
                    MLog.e(TAG, "reducePhotoSize() failed", e);
                    mListener.onErrorReducingPhotoSize();
                }
            }
        });
    }

    private void uploadFromUri(Uri fileUri) {
        MLog.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        final StorageReference photoRef = mStorageRef.child(mStorageRefString)
                .child(mTargetFileUri.getLastPathSegment());

        mListener.onPhotoUploadStarted();

        MLog.d(TAG, "uploadFromUri:dst:" + photoRef.getPath());
        photoRef.putFile(fileUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        if (isActivityDestroyed())
                            return;
                        mListener.onPhotoUploadProgress((int) taskSnapshot.getTotalByteCount() / 1024,
                                (int) taskSnapshot.getBytesTransferred() / 1024);
                    }
                })
                .addOnSuccessListener(mActivity, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (isActivityDestroyed())
                            return;
                        mListener.onPhotoUploadSuccess(mTargetFileUri.getLastPathSegment(), taskSnapshot.getDownloadUrl().toString());
                    }
                })
                .addOnFailureListener(mActivity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        if (isActivityDestroyed())
                            return;
                        mListener.onPhotoUploadError(exception);
                    }
                });
    }

    private boolean isActivityDestroyed() {
        return mActivity == null || mActivity.isFinishing();
    }
}
