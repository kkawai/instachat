package com.google.firebase.codelab.friendlychat;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.initech.Constants;
import com.initech.util.ImageUtils;
import com.initech.util.LocalFileUtils;
import com.initech.util.MLog;
import com.initech.util.ThreadWrapper;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by kevin on 9/4/2016.
 * Helps upload a single file to a destination
 */
public class PhotoUploadHelper {
    private static final String TAG = "PhotoUploadHelper";
    private static final int RC_CHOOSE_PICTURE = 103;
    private static final int RC_TAKE_PICTURE = 101;
    private static final int RC_STORAGE_PERMS = 102;
    private Uri mFileUri = null;
    private File mFile = null;
    private Activity mActivity;
    private PhotoUploadListener mListener;
    private StorageReference mStorageRef;
    private DatabaseReference mFirebaseDatabaseReference;
    private String mStorageRefString;

    PhotoUploadHelper(Activity context) {
        mActivity = context;
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public interface PhotoUploadListener {
        void onErrorReducingPhotoSize();
        void onPhotoUploadStarted();
        void onPhotoUploadProgress(int max, int current);
        void onPhotoUploadSuccess(Uri imageUrl);
        void onPhotoUploadError(Exception exception);
    }

    public void setStorageRefString(String ref) {
        mStorageRefString = ref;
    }

    public void setPhotoUploadListener(PhotoUploadListener listener) {
        mListener = listener;
    }

    public void cleanup() {
        mActivity = null;
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
        mFile = new File(dir, UUID.randomUUID().toString() + ".jpg");
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

                    final Bitmap bitmap = ImageUtils.getBitmap(mActivity, mFileUri, Constants.MAX_PIC_SIZE_BYTES);
                    ImageUtils.writeBitmapToFile(bitmap, mFile);
                    if (isActivityDestroyed())
                        return;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isActivityDestroyed())
                                return;
                            uploadFromUri(mFileUri);
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
                .child(fileUri.getLastPathSegment());

        mListener.onPhotoUploadStarted();

        MLog.d(TAG, "uploadFromUri:dst:" + photoRef.getPath());
        photoRef.putFile(fileUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        if (isActivityDestroyed())
                            return;
                        mListener.onPhotoUploadProgress((int)taskSnapshot.getTotalByteCount()/1024,
                                (int)taskSnapshot.getBytesTransferred()/1024);
                    }
                })
                .addOnSuccessListener(mActivity, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (isActivityDestroyed())
                            return;
                        mListener.onPhotoUploadSuccess(taskSnapshot.getMetadata().getDownloadUrl());
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
