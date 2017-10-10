package com.instachat.android.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.model.Url;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.instachat.android.Constants;
import com.instachat.android.MyApp;
import com.instachat.android.R;
import com.instachat.android.data.api.CloudVisionApi;
import com.instachat.android.data.api.UploadListener;
import com.instachat.android.util.ImageUtils;
import com.instachat.android.util.LocalFileUtils;
import com.instachat.android.util.MLog;
import com.instachat.android.util.SimpleRxWrapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import cn.pedant.SweetAlert.SweetAlertDialog;
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
    private ActivityState mActivityState;
    private UploadListener mListener;
    private StorageReference mStorageRef;
    private String mStorageRefString;
    private PhotoType mPhotoType;
    private File mTempPhotoUploadDir;

    public enum PhotoType {
        chatRoomPhoto, userProfilePhoto
    }

    public PhotoUploadHelper(@NonNull Activity activity, @NonNull ActivityState activityState) {
        mActivity = activity;
        mActivityState = activityState;
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mTempPhotoUploadDir = new File(Environment.getExternalStorageDirectory() + "/photos");
        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
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
        mActivityState = null;
        mActivity = null;
        mListener = null;
        mCloudVisionApiListener = null;
    }

    public void launchCamera(boolean isChoose) {

        // Check that we have permission to read images from external storage.
        if (!isChoose) {
            String read = android.Manifest.permission.READ_EXTERNAL_STORAGE;
            String write = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            String camera = android.Manifest.permission.CAMERA;
            if (!EasyPermissions.hasPermissions(mActivity, read, write, camera)) {
                EasyPermissions.requestPermissions(mActivity, mActivity.getString(R.string.rationale_storage),
                        RC_TAKE_PICTURE, read, write, camera);
                return;
            }
        } else {
            String read = android.Manifest.permission.READ_EXTERNAL_STORAGE;
            String write = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (!EasyPermissions.hasPermissions(mActivity, read, write)) {
                EasyPermissions.requestPermissions(mActivity,
                        mActivity.getString(R.string.rationale_storage), RC_CHOOSE_PICTURE, read, write);
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
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        mTargetFileUri = FileProvider.getUriForFile(mActivity, mActivity.getPackageName() + ".fileprovider",
                mTargetFile);
        List<ResolveInfo> resInfoList = MyApp.getInstance().getPackageManager().queryIntentActivities(cameraIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            MyApp.getInstance().grantUriPermission(packageName, mTargetFileUri, Intent
                    .FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            MLog.d(TAG, "granted app ", packageName, " permission to internal file uri: ", mTargetFileUri);
        }
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

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private void reducePhotoSize(final Uri uri) {
        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
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
                    try {
                        new CloudVisionApi(mCloudVisionApiListener).checkForAdultOrViolence(bitmap);
                    } catch (final Exception e) {
                        MLog.e(TAG, "cloud vision getApi failed ", e);
                    }
                } catch (final Exception e) {
                    MLog.e(TAG, "reducePhotoSize() failed", e);
                    mListener.onErrorReducingPhotoSize();
                }
            }
        });
    }

    private CloudVisionApi.CloudVisionApiListener mCloudVisionApiListener = new CloudVisionApi.CloudVisionApiListener
            () {
        @Override
        public void onImageInspectionCompleted(boolean isCallFailed, final boolean isPossiblyAdult, final boolean
                isPossiblyViolent) {
            //fixRotate(mTargetFile);
            if (mActivityState == null || mActivityState.isActivityDestroyed())
                return;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mActivityState == null || mActivityState.isActivityDestroyed())
                        return;
                    uploadFromUri(mTargetFileUri, isPossiblyAdult, isPossiblyViolent);
                }
            });
        }
    };

    private void fixRotate(final File file) {

        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            Bitmap bm = BitmapFactory.decodeFile(file.getPath(), opts);
            ExifInterface exif = new ExifInterface(file.getPath());
            String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;

            int rotationAngle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
                rotationAngle = 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
                rotationAngle = 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
                rotationAngle = 270;

            Matrix matrix = new Matrix();
            matrix.setRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
            ImageUtils.writeBitmapToFile(rotatedBitmap, mTargetFile);
        } catch (Exception e) {
            MLog.e(TAG, "fixRotate() failed ", e);
        }
    }

    private void showIllegalProfilePicDialog() {
        new SweetAlertDialog(mActivity, SweetAlertDialog.WARNING_TYPE).
                setTitleText(mActivity.getString(R.string.warning)).
                setContentText(mActivity.getString(R.string.possible_explicit_content_warning_pls_choose_another)).
                setConfirmText(mActivity.getString(android.R.string.ok)).
                showCancelButton(false).
                setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
    }

    private void uploadFromUri(Uri fileUri, final boolean isPossibleAdult, final boolean isPossibleViolence) {
        MLog.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        if (mPhotoType == PhotoType.userProfilePhoto && (isPossibleAdult || isPossibleViolence)) {
            showIllegalProfilePicDialog();
            return;
        }

        final StorageReference photoRef = mStorageRef.child(mStorageRefString).child(mTargetFileUri
                .getLastPathSegment());

        mListener.onPhotoUploadStarted();

        MLog.d(TAG, "uploadFromUri:dst:" + photoRef.getPath());
        photoRef.putFile(fileUri).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;
                mListener.onPhotoUploadProgress((int) taskSnapshot.getTotalByteCount() / 1024, (int) taskSnapshot
                        .getBytesTransferred() / 1024);
            }
        }).addOnSuccessListener(mActivity, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;
                postProcessPhoto(taskSnapshot.getDownloadUrl().toString(), isPossibleAdult, isPossibleViolence);
            }
        }).addOnFailureListener(mActivity, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;
                mListener.onPhotoUploadError(exception);
            }
        });
    }

    private void postProcessPhoto(final String photoUrl, final boolean isPossibleAdult, final boolean
            isPossibleViolence) {

        if (!FirebaseRemoteConfig.getInstance().getBoolean(Constants.KEY_DO_SHORTEN_IMAGE_URLS)) {
            mListener.onPhotoUploadSuccess(photoUrl, isPossibleAdult, isPossibleViolence);
            return;
        }

        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                Urlshortener.Builder builder = new Urlshortener.Builder(AndroidHttp.newCompatibleTransport(),
                        AndroidJsonFactory.getDefaultInstance(), null);
                Urlshortener urlshortener = builder.build();

                com.google.api.services.urlshortener.model.Url url = new Url();
                url.setLongUrl(photoUrl);
                try {
                    url = urlshortener.url().insert(url).setKey(Constants.GOOGLE_API_KEY).execute();
                } catch (Exception e) {
                    MLog.e(TAG, "shorten url error: ", e);
                    SimpleRxWrapper.executeInUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onPhotoUploadSuccess(photoUrl, isPossibleAdult, isPossibleViolence);
                        }
                    });
                    return;
                }
                final String newUrl = url != null && url.getId() != null ? url.getId() : photoUrl;
                MLog.d(TAG, "shorten url success: " + newUrl);
                SimpleRxWrapper.executeInUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onPhotoUploadSuccess(newUrl, isPossibleAdult, isPossibleViolence);
                    }
                });
            }
        });

    }
}
