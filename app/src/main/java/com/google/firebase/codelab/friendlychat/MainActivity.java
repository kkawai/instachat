/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.friendlychat;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ath.fuel.FuelInjector;
import com.ath.fuel.Lazy;
import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.initech.Constants;
import com.initech.model.User;
import com.initech.util.ImageUtils;
import com.initech.util.MLog;
import com.initech.util.Preferences;
import com.initech.util.StringUtil;
import com.initech.util.ThreadWrapper;
import com.initech.view.ThemedAlertDialog;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, FriendlyMessageContainer,
        EasyPermissions.PermissionCallbacks {

    private static final int MAX_PIC_SIZE_BYTES = 512000;

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageTextView;
        public TextView messengerTextView;
        public TextView messageTimeTextView;
        public ImageView messagePhotoView;
        public CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
            messageTimeTextView = (TextView) itemView.findViewById(R.id.messageTimeTextView);
            messagePhotoView = (ImageView)itemView.findViewById(R.id.messagePhotoView);
        }
    }

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 140;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;

    private View mSendButton, mAttachButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    //private FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText mMessageEditText;
    private AdView mAdView;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    //private GoogleApiClient mGoogleApiClient;
    private User mUser;
    private DrawerLayout mDrawerLayout;
    private final Lazy<PagerAdapterHelper> mPagerAdapterHelper = Lazy.attain(this, PagerAdapterHelper.class);
    private static final int RC_TAKE_PICTURE = 101;
    private static final int RC_STORAGE_PERMS = 102;

    private static final String KEY_FILE_PATH = "key_file_path";
    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String KEY_DOWNLOAD_URL = "key_download_url";

    private BroadcastReceiver mDownloadReceiver;
    private ProgressDialog mProgressDialog;

    private Uri mDownloadUrl = null;
    private Uri mFileUri = null;
    private File mFile = null;

    // [START declare_ref]
    private StorageReference mStorageRef;

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (mFileUri != null)
            out.putParcelable(KEY_FILE_URI, mFileUri);
        if (mDownloadUrl != null)
            out.putParcelable(KEY_DOWNLOAD_URL, mDownloadUrl);
        if (mFile != null)
            out.putString(KEY_FILE_PATH, mFile.getAbsolutePath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) { // Android support FragmentManager v4 erroneously remembers every Fragment, even when "retain instance" is
            // false; remove the saved Fragments so we don't get into a dueling layout issue between the new Fragments we're trying to make and the
            // old ones being restored.
            savedInstanceState.remove("android:support:fragments");
        }
        super.onCreate(savedInstanceState);

        DataBindingUtil.setContentView(this, R.layout.activity_main);
        setupDrawer();
        setupToolbar();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = ANONYMOUS;

        // Restore instance state
        if (savedInstanceState != null) {
            mFileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
            mDownloadUrl = savedInstanceState.getParcelable(KEY_DOWNLOAD_URL);
            if (savedInstanceState.containsKey(KEY_FILE_PATH)) {
                mFile = new File(savedInstanceState.getString(KEY_FILE_PATH));
            }
        }

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mUser = Preferences.getInstance(this).getUser();
        if (mUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            final String photo = Preferences.getInstance(this).getUser().getProfilePicUrl();
            if (photo != null) {
                mPhotoUrl = photo;
                MLog.i(TAG, "photo url: " + photo);
            }
            mUsername = Preferences.getInstance(this).getUsername();
        }

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        initFirebaseAdapter();

        FuelInjector.ignite(this, this);

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        // Initialize and request AdMob ad.
        /*
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        */

        // Initialize Firebase Measurement.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize Firebase Remote Config.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", DEFAULT_MSG_LENGTH_LIMIT);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        // Fetch remote config.
        fetchConfig();

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        initDownloadReceiver();
        initButtons();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register download receiver
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mDownloadReceiver, MyDownloadService.getIntentFilter());
    }

    private void beginDownload() {
        // Get path
        String path = "photos/" + mFileUri.getLastPathSegment();

        // Kick off download service
        Intent intent = new Intent(this, MyDownloadService.class);
        intent.setAction(MyDownloadService.ACTION_DOWNLOAD);
        intent.putExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH, path);
        startService(intent);

        // Show loading spinner
        showProgressDialog();
    }

    private void showMessageDialog(String title, String message) {
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .create();
        ad.show();
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void initDownloadReceiver() {
        mDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "downloadReceiver:onReceive:" + intent);
                hideProgressDialog();

                if (MyDownloadService.ACTION_COMPLETED.equals(intent.getAction())) {
                    String path = intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH);
                    long numBytes = intent.getLongExtra(MyDownloadService.EXTRA_BYTES_DOWNLOADED, 0);

                    // Alert success
                    showMessageDialog(getString(R.string.success), String.format(Locale.getDefault(),
                            "%d bytes downloaded from %s", numBytes, path));
                }

                if (MyDownloadService.ACTION_ERROR.equals(intent.getAction())) {
                    String path = intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH);

                    // Alert failure
                    showMessageDialog("Error", String.format(Locale.getDefault(),
                            "Failed to download from %s", path));
                }
            }
        };
    }

    private void initButtons() {
        mSendButton = findViewById(R.id.sendButton);
        mAttachButton = findViewById(R.id.attachButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String text = mMessageEditText.getText().toString();
                if (StringUtil.isEmpty(text)) {
                    return;
                }
                FriendlyMessage friendlyMessage = new FriendlyMessage(text, mUsername,
                        userid(), null, null, System.currentTimeMillis());
                mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(friendlyMessage);
                mMessageEditText.setText("");
                mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
            }
        });
        mAttachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCamera();
            }
        });
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    private String limitString(final String s, final int limit) {
        if (s == null || s.length() <= limit) {
            return s;
        }
        return s.substring(0, limit) + "...";
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister download receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDownloadReceiver);
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
        mPagerAdapterHelper.get().setListener(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.invite_menu:
                sendInvitation();
                return true;
            case R.id.crash_menu:
                FirebaseCrash.logcat(Log.ERROR, TAG, "crash caused");
                causeCrash();
                return true;
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                //Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                //mFirebaseUser = null;
                Preferences.getInstance(this).saveUser(null);
                mUsername = ANONYMOUS;
                mPhotoUrl = null;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            case R.id.full_screen_texts_menu:
                openFullScreenTextView(-1);
                return true;
            case R.id.download:
                beginDownload();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void causeCrash() {
        throw new NullPointerException("Fake null pointer exception");
    }

    private void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    // Fetch the config to determine the allowed length of messages.
    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // There has been an error fetching the config
                        Log.w(TAG, "Error fetching config: " + e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        } else if (requestCode == RC_TAKE_PICTURE) {
            if (resultCode == RESULT_OK) {
                if (mFileUri != null) {
                    reducePhotoSize();
                } else {
                    Log.w(TAG, "File URI is null");
                }
            }
        }
    }

    private void reducePhotoSize() {
        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bitmap = ImageUtils.getBitmap(MainActivity.this,mFileUri,MAX_PIC_SIZE_BYTES);
                    ImageUtils.writeBitmapToFile(bitmap, mFile);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            uploadFromUri(mFileUri);
                        }
                    });
                }catch(final Exception e) {
                    showPhotoReduceError();
                }
            }
        });
    }

    private void showPhotoReduceError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,"Could not read photo",Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong("friendly_msg_length");
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
        Log.d(TAG, "FML is: " + friendly_msg_length);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);
    }

    private void setupDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
            return;
        }
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            return;
        }
        super.onBackPressed();
    }

    private void openFullScreenTextView(final int startingPos) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
        if (fragment != null) {
            return;
        }
        fragment = new FullScreenTextFragment();
        final Bundle args = new Bundle();
        args.putInt(Constants.KEY_STARTING_POS, startingPos);
        fragment.setArguments(args);
        ((FullScreenTextFragment) fragment).setFriendlyMessageContainer(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_content, fragment, FullScreenTextFragment.TAG).commit();
    }

    private void notifyPagerAdapterDataSetChanged() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FullScreenTextFragment.TAG);
        if (fragment == null) {
            return;
        }
        ((FullScreenTextFragment) fragment).notifyDataSetChanged();
    }

    @Override
    public FriendlyMessage getFriendlyMessage(int position) {
        return mFirebaseAdapter.getItem(position);
    }

    @Override
    public int getFriendlyMessageCount() {
        return mFirebaseAdapter.getItemCount();
    }

    @Override
    public void setCurrentPosition(int position) {
        mMessageRecyclerView.scrollToPosition(position + 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    private void uploadFromUri(Uri fileUri) {
        MLog.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // [START get_child_ref]
        // Get a reference to store file at photos/<FILENAME>.jpg
        final StorageReference photoRef = mStorageRef.child("photos")
                .child(fileUri.getLastPathSegment());
        // [END get_child_ref]

        // Upload file to Firebase Storage
        // [START_EXCLUDE]
        showProgressDialog();
        // [END_EXCLUDE]
        MLog.d(TAG, "uploadFromUri:dst:" + photoRef.getPath());
        photoRef.putFile(fileUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Upload succeeded

                        // Get the public download URL
                        mDownloadUrl = taskSnapshot.getMetadata().getDownloadUrl();
                        final String photoId = mFileUri.getLastPathSegment();
                        FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername,
                                userid(), mDownloadUrl.toString(), photoId, System.currentTimeMillis());
                        MLog.d(TAG, "uploadFromUri:onSuccess photoId: "+photoId);
                        mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(friendlyMessage);

                        // [START_EXCLUDE]
                        hideProgressDialog();
                        //updateUI(mAuth.getCurrentUser());
                        // [END_EXCLUDE]
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Upload failed
                        MLog.w(TAG, "uploadFromUri:onFailure", exception);

                        mDownloadUrl = null;

                        // [START_EXCLUDE]
                        hideProgressDialog();
                        Toast.makeText(MainActivity.this, "Error: upload failed",
                                Toast.LENGTH_SHORT).show();
                        //updateUI(mAuth.getCurrentUser());
                        // [END_EXCLUDE]
                    }
                });
    }

    @AfterPermissionGranted(RC_STORAGE_PERMS)
    private void launchCamera() {
        Log.d(TAG, "launchCamera");

        // Check that we have permission to read images from external storage.
        String perm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (!EasyPermissions.hasPermissions(this, perm)) {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_storage),
                    RC_STORAGE_PERMS, perm);
            return;
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
        mFileUri = FileProvider.getUriForFile(this,
                "com.google.firebase.quickstart.firebasestorage.fileprovider", mFile);

        // Create and launch the intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);

        startActivityForResult(takePictureIntent, RC_TAKE_PICTURE);
    }

    private void initFirebaseAdapter() {
        mFirebaseAdapter = new MyFirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(
                FriendlyMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(MESSAGES_CHILD)) {

            @Override
            public MessageViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
                final MessageViewHolder holder = super.onCreateViewHolder(parent, viewType);
                holder.messengerImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        final FriendlyMessage msg = mFirebaseAdapter.getItem(holder.getAdapterPosition());
                        Toast.makeText(v.getContext(), "clicked on profile: " + msg.getName() + " said: " + msg.getText(), Toast.LENGTH_SHORT).show();
                    }
                });
                final View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openFullScreenTextView(holder.getAdapterPosition());
                    }
                };
                holder.messageTimeTextView.setOnClickListener(onClickListener);
                holder.messageTextView.setOnClickListener(onClickListener);
                holder.messengerTextView.setOnClickListener(onClickListener);

                View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        new ThemedAlertDialog.Builder(view.getContext())
                                .setMessage(getString(R.string.delete_message_question) + " (" + limitString(mFirebaseAdapter.getItem(holder.getAdapterPosition()).getText(), 15) + ")")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                                        final FriendlyMessage msg = mFirebaseAdapter.getItem(holder.getAdapterPosition());
                                        MLog.d(TAG," msg.getImageUrl(): "+msg.getImageUrl() + " " + msg.getImageId());
                                        if (msg.getImageUrl() != null && msg.getImageId() != null) {
                                            final StorageReference photoRef = mStorageRef.child("photos")
                                                    .child(msg.getImageId());
                                            photoRef.delete();
                                            MLog.d(TAG,"deleted photo "+msg.getImageId());
                                        }
                                        DatabaseReference ref = mFirebaseDatabaseReference.child(MESSAGES_CHILD + "/" + msg.getId());
                                        if (ref != null)
                                            ref.removeValue();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .setCancelable(true).setOnCancelListener(null)
                                .create().show();
                        return false;
                    }
                };

                holder.messageTimeTextView.setOnLongClickListener(onLongClickListener);
                holder.messageTextView.setOnLongClickListener(onLongClickListener);
                holder.messengerTextView.setOnLongClickListener(onLongClickListener);
                return holder;
            }

            @Override
            protected void populateViewHolder(MessageViewHolder viewHolder, FriendlyMessage friendlyMessage, int position) {

                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyMessage.getText() != null)
                    viewHolder.messageTextView.setText(friendlyMessage.getText());
                viewHolder.messengerTextView.setText(friendlyMessage.getName());
                if (friendlyMessage.getTime() != 0) {
                    viewHolder.messageTimeTextView.setVisibility(View.VISIBLE);
                    viewHolder.messageTimeTextView.setText(StringUtil.getHour(friendlyMessage.getTime()));
                } else {
                    viewHolder.messageTimeTextView.setVisibility(View.INVISIBLE);
                }
//                if (friendlyMessage.getPhotoUrl() == null) {
//                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
//                            R.drawable.ic_account_circle_black_36dp));
//                } else {
//
//                }
                if (friendlyMessage.getImageUrl() != null) {
                    Glide.with(MainActivity.this)
                            .load(friendlyMessage.getImageUrl())
                            .crossFade()
                            .into(viewHolder.messagePhotoView);
                    viewHolder.messagePhotoView.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.messagePhotoView.setVisibility(View.GONE);
                }

                Glide.with(MainActivity.this)
                        .load(Constants.DP_URL(userid()))
                        .error(R.drawable.ic_account_circle_black_36dp)
                        .into(viewHolder.messengerImageView);

            }

            @Override
            protected FriendlyMessage parseSnapshot(DataSnapshot snapshot) {
                FriendlyMessage friendlyMessage = super.parseSnapshot(snapshot);
                friendlyMessage.setId(snapshot.getKey());
                return friendlyMessage;
            }
        };
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {

                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
                notifyPagerAdapterDataSetChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                notifyPagerAdapterDataSetChanged();
            }
        });
    }

    private Integer userid() {
        return Preferences.getInstance(MainActivity.this).getUserId();
    }
}
