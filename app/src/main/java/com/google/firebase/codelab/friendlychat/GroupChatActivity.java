package com.google.firebase.codelab.friendlychat;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ath.fuel.FuelInjector;
import com.bhargavms.dotloader.DotLoader;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetMenuDialog;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;
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
import com.google.firebase.codelab.friendlychat.adapter.AdapterPopulateHolderListener;
import com.google.firebase.codelab.friendlychat.adapter.MessageTextClickedListener;
import com.google.firebase.codelab.friendlychat.adapter.MessageViewHolder;
import com.google.firebase.codelab.friendlychat.adapter.MyFirebaseRecyclerAdapter;
import com.google.firebase.codelab.friendlychat.adapter.UserThumbClickedListener;
import com.google.firebase.codelab.friendlychat.fullscreen.FriendlyMessageContainer;
import com.google.firebase.codelab.friendlychat.fullscreen.FullScreenTextFragment;
import com.google.firebase.codelab.friendlychat.login.SignInActivity;
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.initech.Constants;
import com.initech.api.NetworkApi;
import com.initech.api.UploadListener;
import com.initech.gcm.GCMHelper;
import com.initech.model.User;
import com.initech.profile.FragmentProfile;
import com.initech.util.AnimationUtil;
import com.initech.util.MLog;
import com.initech.util.Preferences;
import com.initech.util.ScreenUtil;
import com.initech.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

public class GroupChatActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener, FriendlyMessageContainer,
        EasyPermissions.PermissionCallbacks, UploadListener, UserThumbClickedListener,
        ChatTypingHelper.UserTypingListener {

    private static final String TAG = "GroupChatActivity";

    private static final int REQUEST_INVITE = 1;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 140;
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private SharedPreferences mSharedPreferences;

    private View mSendButton, mAttachButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private MyFirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    //private FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText mMessageEditText;
    private AdView mAdView;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    //private GoogleApiClient mGoogleApiClient;
    private DrawerLayout mDrawerLayout;

    private BroadcastReceiver mDownloadReceiver;
    private ProgressDialog mProgressDialog;

    // [START declare_ref]
    private PhotoUploadHelper mPhotoUploadHelper;
    private DrawerHelper mDrawerHelper;
    private String mDatabaseChild;
    private boolean mIsStartedAnimation;
    private DotLoader mDotsLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) { // Android support FragmentManager v4 erroneously remembers every Fragment, even when "retain instance" is
            // false; remove the saved Fragments so we don't get into a dueling layout issue between the new Fragments we're trying to make and the
            // old ones being restored.
            savedInstanceState.remove("android:support:fragments");
        }
        super.onCreate(savedInstanceState);

        initDatabaseRef();

        DataBindingUtil.setContentView(this, R.layout.activity_main);
        initPhotoHelper(savedInstanceState);
        setupDrawer();
        setupToolbar();
        mDotsLoader = (DotLoader) findViewById(R.id.text_dot_loader);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        if (!Preferences.getInstance().isLoggedIn()) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        GCMHelper.onCreate(this);

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
                    setEnableSendButton(true);
                    onMeEnteringText();
                } else {
                    setEnableSendButton(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        //initDownloadReceiver();
        initButtons();
    }

    void onMeEnteringText() {

    }

    @Override
    public void onStart() {
        super.onStart();
        // Register download receiver
        //LocalBroadcastManager.getInstance(this)
        //        .registerReceiver(mDownloadReceiver, MyDownloadService.getIntentFilter());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
        mSendButton.setEnabled(mMessageEditText.getText().toString().trim().length() > 0);
        if (Preferences.getInstance().isLoggedIn()) {
            GCMHelper.onResume(this);
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mPhotoUploadHelper.getPhotoType() != null) {
            outState.putString(Constants.KEY_PHOTO_TYPE, mPhotoUploadHelper.getPhotoType().name());
            MLog.d(TAG, "onSaveInstanceState() saving photo type: " + mPhotoUploadHelper.getPhotoType().name());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unregister download receiver
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mDownloadReceiver);
    }

    @Override
    public void onDestroy() {
        mPhotoUploadHelper.cleanup();
        mFirebaseAdapter.cleanup();
        mDrawerHelper.cleanup();
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    void initDatabaseRef() {
        String databaseRef;
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_DATABASE_CHILD)) {
            databaseRef = getIntent().getStringExtra(Constants.KEY_DATABASE_CHILD);
        } else {
            databaseRef = Constants.DEFAULT_MESSAGES_CHILD;
        }
        setDatabaseRef(databaseRef);
    }

    final void setDatabaseRef(final String databaseRef) {
        mDatabaseChild = databaseRef;
    }

    private void setEnableSendButton(final boolean isEnable) {

        if (isEnable && mSendButton.isEnabled() || !isEnable && !mSendButton.isEnabled() || mIsStartedAnimation)
            return; //already set

        final Animation hideAnimation = AnimationUtils.loadAnimation(GroupChatActivity.this, R.anim.fab_scale_down);
        final Animation showAnimation = AnimationUtils.loadAnimation(GroupChatActivity.this, R.anim.fab_scale_up);

        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                showAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mIsStartedAnimation = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                mSendButton.startAnimation(showAnimation);
                mSendButton.setEnabled(isEnable);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mIsStartedAnimation = true;
        mSendButton.startAnimation(hideAnimation);
    }

    private void initPhotoHelper(Bundle savedInstanceState) {
        mPhotoUploadHelper = new PhotoUploadHelper(this);
        mPhotoUploadHelper.setPhotoUploadListener(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY_PHOTO_TYPE)) {
            PhotoUploadHelper.PhotoType photoType = PhotoUploadHelper.PhotoType.valueOf(savedInstanceState.getString(Constants.KEY_PHOTO_TYPE));
            mPhotoUploadHelper.setPhotoType(photoType);
            MLog.d(TAG, "initPhotoHelper: retrieved from saved instance state: " + photoType);
        }
    }

    /*private void beginDownload() {
        // Get path
        String path = "photos/" + mFileUri.getLastPathSegment();

        // Kick off download service
        Intent intent = new Intent(this, MyDownloadService.class);
        intent.setAction(MyDownloadService.ACTION_DOWNLOAD);
        intent.putExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH, path);
        startService(intent);

        // Show loading spinner
        showProgressDialog();
    }*/

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
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgressNumberFormat("%1dk / %2dk");
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /*private void initDownloadReceiver() {
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
    }*/

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
                final FriendlyMessage friendlyMessage = new FriendlyMessage(text, myUsername(),
                        myUserid(), myDpid(), null, null, System.currentTimeMillis());
                mFirebaseDatabaseReference.child(mDatabaseChild).push().setValue(friendlyMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        onFriendlyMessageSent(friendlyMessage);
                    }
                });

                mMessageEditText.setText("");
                mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
            }
        });
        mAttachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileOptions();
            }
        });
    }

    void onFriendlyMessageSent(FriendlyMessage friendlyMessage) {

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
                GCMHelper.unregister(Preferences.getInstance().getUserId() + "");
                Preferences.getInstance().saveUser(null);
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
                //beginDownload();
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
                        MLog.w(TAG, "Error fetching config: " + e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MLog.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        mPhotoUploadHelper.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                MLog.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                MLog.d(TAG, "Failed to send invitation.");
            }
        }

    }

    private void showPhotoReduceError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                Toast.makeText(GroupChatActivity.this, "Could not read photo", Toast.LENGTH_SHORT).show();
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
        MLog.d(TAG, "FML is: " + friendly_msg_length);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        MLog.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);
    }

    private void setupDrawer() {
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
        mDrawerHelper = new DrawerHelper(this, mDrawerLayout, mPhotoUploadHelper);
        mDrawerHelper.setup(navigationView);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
            return;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
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
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_content, fragment, FullScreenTextFragment.TAG).addToBackStack(null).commit();
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
    public void setCurrentFriendlyMessage(int position) {
        mMessageRecyclerView.scrollToPosition(position + 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        mPhotoUploadHelper.onPermissionsGranted(requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    private void initFirebaseAdapter() {
        mFirebaseAdapter = new MyFirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(
                FriendlyMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(mDatabaseChild));
        mFirebaseAdapter.setDatabaseChild(mDatabaseChild);
        mFirebaseAdapter.setActivity(this);
        mFirebaseAdapter.setAdapterPopulateHolderListener(new AdapterPopulateHolderListener() {
            @Override
            public void onViewHolderPopulated() {
                mProgressBar.setVisibility(ProgressBar.GONE);
            }
        });
        mFirebaseAdapter.setMessageTextClickedListener(new MessageTextClickedListener() {
            @Override
            public void onMessageClicked(final int position) {
                openFullScreenTextView(position);
            }
        });
        mFirebaseAdapter.setUserThumbClickedListener(this);
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {

                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 || positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1)) {
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
        //don't show initial indeterminate progress more than 5 seconds
        //no matter how long it takes to pull messages from firebase
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed())
                    return;
                if (mProgressBar.getVisibility() == View.VISIBLE) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        }, 5000);
    }

    Integer myUserid() {
        return Preferences.getInstance().getUserId();
    }

    String myDpid() {
        return Preferences.getInstance().getUser().getProfilePicUrl();
    }

    String myUsername() {
        return Preferences.getInstance().getUsername();
    }

    private void showFileOptions() {

        /**
         * if the keyboard is open, close it first before showing
         * the bottom dialog otherwise there is flicker.
         * The delay is bad, but it works for now.
         */
        if (mMessageEditText.hasFocus()) {
            ScreenUtil.hideVirtualKeyboard(mMessageEditText);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isActivityDestroyed())
                        return;
                    showBottomDialog();
                }
            }, 175);
        } else {
            showBottomDialog();
        }
    }

    private void showBottomDialog() {
        final BottomSheetMenuDialog dialog = new BottomSheetBuilder(this, R.style.AppTheme_BottomSheetDialog)
                .setMode(BottomSheetBuilder.MODE_LIST)
                .setMenu(R.menu.file_upload_options)
                .setItemClickListener(new BottomSheetItemClickListener() {
                    @Override
                    public void onBottomSheetItemClick(final MenuItem item) {
                        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.chatRoomPhoto);
                        mPhotoUploadHelper.setStorageRefString(Constants.PHOTOS_CHILD);
                        if (item.getItemId() == R.id.menu_take_photo) {
                            mPhotoUploadHelper.launchCamera(false);
                        } else if (item.getItemId() == R.id.menu_choose_photo) {
                            mPhotoUploadHelper.launchCamera(true);
                        }
                    }
                })
                .createDialog();

        dialog.show();
    }

    @Override
    public void onErrorReducingPhotoSize() {
        MLog.i(TAG, "onErrorReducingPhotoSize()");
        if (isActivityDestroyed())
            return;
        showPhotoReduceError();
    }

    @Override
    public void onPhotoUploadStarted() {
        MLog.i(TAG, "onPhotoUploadStarted()");
        if (isActivityDestroyed())
            return;
        showProgressDialog();
    }

    @Override
    public void onPhotoUploadProgress(int max, int current) {
        MLog.i(TAG, "onPhotoUploadProgress() " + current + " / " + max);
        if (isActivityDestroyed())
            return;
        if (mProgressDialog != null) {
            try {
                mProgressDialog.setMax(max);
                mProgressDialog.setProgress(current);
            } catch (Exception e) {
                MLog.e(TAG, "set photo upload progress failed ", e);
            }
        }
    }

    @Override
    public void onPhotoUploadSuccess(String photoId, String photoUrl) {
        if (isActivityDestroyed()) {
            return;
        }
        hideProgressDialog();

        if (mPhotoUploadHelper.getPhotoType() == PhotoUploadHelper.PhotoType.chatRoomPhoto) {

            final FriendlyMessage friendlyMessage = new FriendlyMessage("", myUsername(),
                    myUserid(), myDpid(), photoUrl, photoId, System.currentTimeMillis());
            MLog.d(TAG, "uploadFromUri:onSuccess photoId: " + photoId);
            mFirebaseDatabaseReference.child(mDatabaseChild).push().setValue(friendlyMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    onFriendlyMessageSent(friendlyMessage);
                }
            });

        } else if (mPhotoUploadHelper.getPhotoType() == PhotoUploadHelper.PhotoType.userProfilePhoto) {
            final User user = Preferences.getInstance().getUser();
            user.setProfilePicUrl(photoId);
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
            mDrawerHelper.updateProfilePic(photoId);
        }
    }

    @Override
    public void onPhotoUploadError(Exception exception) {
        MLog.i(TAG, "onPhotoUploadError() ", exception);
        if (isActivityDestroyed())
            return;
        hideProgressDialog();
        Toast.makeText(GroupChatActivity.this, R.string.error_send_photo,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUserThumbClicked(ImageView imageView, FriendlyMessage message) {
        Fragment fragment = FragmentProfile.newInstance(message);
        getSupportFragmentManager().beginTransaction()
                .addSharedElement(imageView, "image")
                .replace(R.id.fragment_content, fragment, FragmentProfile.TAG).addToBackStack(null).commit();
    }

    @Override
    public void onRemoteUserTyping(int userid) {
        if (isActivityDestroyed()) {
            return;
        }
        showTypingDots();
    }

    private Handler mDotsHandler = new Handler();
    private Runnable mDotsHideRunner = new Runnable() {
        @Override
        public void run() {
            if (isActivityDestroyed())
                return;
            AnimationUtil.fadeOutAnimation(mDotsLoader);
        }
    };

    void showTypingDots() {

        if (mDotsLoader.getVisibility() != View.VISIBLE) {
            AnimationUtil.fadeInAnimation(mDotsLoader);
        }
        mDotsHandler.removeCallbacks(mDotsHideRunner);
        mDotsHandler.postDelayed(mDotsHideRunner, 3500);
    }
}
