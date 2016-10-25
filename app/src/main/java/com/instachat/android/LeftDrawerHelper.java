package com.instachat.android;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.font.FontUtil;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.tooltip.OnDismissListener;
import com.tooltip.Tooltip;

import org.json.JSONObject;

import java.util.concurrent.RejectedExecutionException;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 9/4/2016.
 */
public class LeftDrawerHelper {
    private static final String TAG = "LeftDrawerHelper";
    private Activity mActivity;
    private DrawerLayout mDrawerLayout;
    private View mHeaderLayout;
    private MyProfilePicListener mMyProfilePicListener;
    private Tooltip mTooltip;
    private ActivityState mActivityState;

    private EditText mBioEditText, mUsernameEditText;
    private ImageView mProfilePic;

    public LeftDrawerHelper(@NonNull Activity activity, @NonNull ActivityState activityState, @NonNull DrawerLayout drawerLayout, @NonNull MyProfilePicListener listener) {
        mActivity = activity;
        mActivityState = activityState;
        mDrawerLayout = drawerLayout;
        mMyProfilePicListener = listener;
    }

    public void updateProfilePic(String dpid) {
        if (mActivityState == null || mActivityState.isActivityDestroyed())
            return;
        Glide.clear(mProfilePic);
        final User user = Preferences.getInstance().getUser();
        try {
            Constants.DP_URL(user.getId(), dpid, new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (mActivityState == null || mActivityState.isActivityDestroyed()) {
                        return;
                    }
                    try {
                        if (!task.isSuccessful()) {
                            mProfilePic.setImageResource(R.drawable.ic_anon_person_36dp);
                            return;
                        }
                        Glide.with(mActivity).load(task.getResult().toString()).error(R.drawable.ic_anon_person_36dp).crossFade().into(mProfilePic);
                    } catch (Exception e) {
                        MLog.e(TAG, "DP_URL failed", e);
                        mProfilePic.setImageResource(R.drawable.ic_anon_person_36dp);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
        }
    }

    private void populateNavHeader() {
        final User user = Preferences.getInstance().getUser();
        mUsernameEditText.setText(Preferences.getInstance().getUsername());
        try {
            Constants.DP_URL(user.getId(), user.getProfilePicUrl(), new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (mActivityState == null || mActivityState.isActivityDestroyed())
                        return;
                    if (!task.isSuccessful()) {
                        mProfilePic.setImageResource(R.drawable.ic_anon_person_36dp);
                        return;
                    }
                    try {
                        Glide.with(mActivity).load(task.getResult().toString()).error(R.drawable.ic_anon_person_36dp).crossFade().into(mProfilePic);
                    } catch (Exception e) {
                        MLog.e(TAG, "onDrawerOpened() could not find user photo in google cloud storage", e);
                        mProfilePic.setImageResource(R.drawable.ic_anon_person_36dp);
                    }
                    checkForRemoteUpdatesToMyDP();
                }
            });
        } catch (RejectedExecutionException e) {
        }
        mUsernameEditText.setFocusableInTouchMode(true);
        mUsernameEditText.clearFocus();
        MLog.d(TAG, "clearFocus called");

        mHeaderLayout.findViewById(R.id.save_username).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * simply closing the drawer will trigger the process of saving
                 * the username
                 */
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        if (TextUtils.isEmpty(user.getProfilePicUrl())) {
            mTooltip = new Tooltip.Builder(mProfilePic, R.style.drawer_tooltip).setText(mActivity.getString(R.string.display_photo_tooltip)).show();
            mTooltip.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss() {
                    checkIfShownUsernameTooltip(mUsernameEditText);
                }
            });
        } else {
            checkIfShownUsernameTooltip(mUsernameEditText);
        }

        if (TextUtils.isEmpty(user.getBio())) {
            mBioEditText.setHint(R.string.hint_write_something_about_yourself);
        } else {
            mBioEditText.setText(user.getBio());
        }
    }

    private void checkIfShownUsernameTooltip(View anchor) {
        if (!Preferences.getInstance().hasShownUsernameTooltip()) {
            mTooltip = new Tooltip.Builder(anchor, R.style.drawer_tooltip).setText(mActivity.getString(R.string.change_username_tooltip)).show();
            Preferences.getInstance().setShownUsernameTooltip(true);
        }
    }

    private int mWhichDrawerLastOpened;

    public void setup(NavigationView navigationView) {
        mHeaderLayout = navigationView.getHeaderView(0); // 0-index header
        mBioEditText = (EditText) mHeaderLayout.findViewById(R.id.input_bio);
        mUsernameEditText = (EditText) mHeaderLayout.findViewById(R.id.nav_username);
        FontUtil.setTextViewFont(mUsernameEditText);
        FontUtil.setTextViewFont(mBioEditText);
        mUsernameEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.MAX_USERNAME_LENGTH)});
        mBioEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.MAX_BIO_LENGTH)});

        mProfilePic = (ImageView) mHeaderLayout.findViewById(R.id.nav_pic);
        mProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ScreenUtil.hideKeyboard(mActivity);
                showChooseDialog();
            }
        });

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {

                if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
                    mWhichDrawerLastOpened = GravityCompat.START;
                else
                    mWhichDrawerLastOpened = GravityCompat.END;

                if (mWhichDrawerLastOpened != GravityCompat.START)
                    return; //only handle left drawer logic

                MLog.d(TAG, "onDrawerOpened() LEFT drawer");

                populateNavHeader();
                ScreenUtil.hideKeyboard(mActivity);
            }

            @Override
            public void onDrawerClosed(View drawerView) {

                if (mWhichDrawerLastOpened != GravityCompat.START)
                    return; //only handle left drawer stuff in this module

                if (mActivityState == null || mActivityState.isActivityDestroyed())
                    return;

                MLog.d(TAG, "onDrawerClosed() LEFT drawer");

                ScreenUtil.hideKeyboard(mActivity);
                /**
                 * don't bother setting visibility to invisible on any view
                 * in the navigation drawer after the first drawer open
                 * it won't work.  android caches it and I'm not sure
                 * how to fix it right now.  no big deal.
                 */
                /*
                mHeaderLayout.findViewById(R.id.save_username).setVisibility(View.INVISIBLE);
                mHeaderLayout.findViewById(R.id.edit_bio).setVisibility(View.INVISIBLE);
                */
                if (mTooltip != null && mTooltip.isShowing())
                    mTooltip.dismiss();

                final User user = Preferences.getInstance().getUser();
                final String existingUsername = user.getUsername() + "";
                final String newUsername = mUsernameEditText.getText().toString();

                final String existingBio = user.getBio() + "";
                final String newBio = mBioEditText.getText().toString();

                /**
                 * check if username or bio has changed.  save if necessary.
                 */
                boolean usernameChanged = false, bioChanged = false;

                if (!existingUsername.equals(newUsername))
                    usernameChanged = true;
                if (!existingBio.equals(newBio))
                    bioChanged = true;

                if (usernameChanged) {
                    if (!StringUtil.isValidUsername(newUsername)) {
                        mUsernameEditText.setText(existingUsername);
                        new SweetAlertDialog(mActivity, SweetAlertDialog.ERROR_TYPE).setContentText(mActivity.getString(R.string.invalid_username)).show();
                        return;
                    }
                }

                saveUser(user, newUsername, newBio, bioChanged, usernameChanged);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

    }

    private void checkForRemoteUpdatesToMyDP() {
        NetworkApi.getUserById(null, Preferences.getInstance().getUserId(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    if (mActivityState == null || mActivityState.isActivityDestroyed())
                        return;
                    final String status = response.getString(NetworkApi.KEY_RESPONSE_STATUS);
                    if (status.equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        final User remote = User.fromResponse(response);
                        if (!TextUtils.isEmpty(remote.getProfilePicUrl())) {
                            User local = Preferences.getInstance().getUser();
                            if (TextUtils.isEmpty(local.getProfilePicUrl()) || !remote.getProfilePicUrl().equals(local.getProfilePicUrl())) {
                                try {
                                    Constants.DP_URL(remote.getId(), remote.getProfilePicUrl(), new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Uri> task) {
                                            if (!task.isSuccessful() || mActivityState.isActivityDestroyed()) {
                                                return;
                                            }
                                            User user = Preferences.getInstance().getUser();
                                            user.setProfilePicUrl(remote.getProfilePicUrl());
                                            Preferences.getInstance().saveUser(user);

                                            MLog.i(TAG, "checkForRemoteUpdatesToMyDP() my pic changed remotely. attempt to update");
                                            try {
                                                Glide.with(mActivity).load(task.getResult().toString()).error(R.drawable.ic_anon_person_36dp).crossFade().into(mProfilePic);
                                            } catch (Exception e) {
                                                MLog.e(TAG, "onDrawerOpened() could not find my photo in google cloud storage", e);
                                            }
                                        }
                                    });
                                } catch (RejectedExecutionException e) {
                                }
                            } else {
                                MLog.i(TAG, "checkForRemoteUpdatesToMyDP() my pic did not change remotely. do not update.");
                            }
                        }
                    }
                } catch (final Exception e) {
                    MLog.e(TAG, "checkIfOtherDeviceUpdatedProfilePic(1) failed", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                MLog.e(TAG, "checkForRemoteUpdatesToMyDP(2) failed: " + error);
            }
        });
    }

    public void cleanup() {
        mActivityState = null;
        mActivity = null;
        mDrawerLayout = null;
        mMyProfilePicListener = null;
    }

    private void showChooseDialog() {
        if (mActivityState == null || mActivityState.isActivityDestroyed())
            return;
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        // Get the layout inflater
        final LayoutInflater inflater = mActivity.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.dialog_picture_choose, null);
        builder.setView(view);
        builder.setCancelable(true);
        final AlertDialog dialog = builder.create();

        view.findViewById(R.id.menu_choose_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                mMyProfilePicListener.onProfilePicChangeRequest(true);
            }
        });
        view.findViewById(R.id.menu_take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                mMyProfilePicListener.onProfilePicChangeRequest(false);
            }
        });
        dialog.show();

    }

    private void showProfileUpdatedDialog() {
        //new SweetAlertDialog(mActivity, SweetAlertDialog.SUCCESS_TYPE).setTitleText(mActivity.getString(R.string.username_changed_dialog_title)).setContentText(mActivity.getString(R.string.is_your_new_username, newUsername)).show();
        new SweetAlertDialog(mActivity, SweetAlertDialog.SUCCESS_TYPE).setTitleText(mActivity.getString(R.string.your_profile_has_been_updated_title)).setContentText(mActivity.getString(R.string.your_profile_has_been_updated_msg)).show();
    }

    private void saveUser(final User user, final String newUsername, final String newBio, final boolean needToSaveBio, final boolean needToSaveUsername) {
        if (needToSaveBio && !needToSaveUsername) {
            user.setBio(newBio);
            NetworkApi.saveUser(mActivity, user, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    MLog.d(TAG, "response: ", response);
                    if (mActivityState == null || mActivityState.isActivityDestroyed())
                        return;
                    try {
                        JSONObject object = new JSONObject(response);
                        if (object.getString(NetworkApi.KEY_RESPONSE_STATUS).equals(NetworkApi.RESPONSE_OK)) {
                            Preferences.getInstance().saveUser(user);
                            showProfileUpdatedDialog();
                        }
                    } catch (Exception e) {
                        MLog.e(TAG, "", e);
                        Toast.makeText(mActivity, mActivity.getString(R.string.general_api_error, "(user1)"), Toast.LENGTH_SHORT).show();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    MLog.d(TAG, "error response: ", error);
                    Toast.makeText(mActivity, mActivity.getString(R.string.general_api_error, "(user2)"), Toast.LENGTH_SHORT).show();
                }
            });

        } else if (needToSaveUsername) {
            if (needToSaveBio)
                user.setBio(newBio);
            NetworkApi.isExistsUsername(mActivity, newUsername, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    if (mActivityState == null || mActivityState.isActivityDestroyed())
                        return;
                    try {
                        if (!response.getJSONObject("data").getBoolean("exists")) {
                            user.setUsername(newUsername);
                            NetworkApi.saveUser(mActivity, user, new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    MLog.d(TAG, "response: ", response);
                                    if (mActivityState == null || mActivityState.isActivityDestroyed())
                                        return;
                                    try {
                                        JSONObject object = new JSONObject(response);
                                        if (object.getString(NetworkApi.KEY_RESPONSE_STATUS).equals(NetworkApi.RESPONSE_OK)) {
                                            user.setUsername(newUsername);
                                            Preferences.getInstance().saveUser(user);
                                            Preferences.getInstance().saveLastSignIn(newUsername);
                                            showProfileUpdatedDialog();
                                        }
                                    } catch (Exception e) {
                                        mUsernameEditText.setText(Preferences.getInstance().getUsername());
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    MLog.d(TAG, "error response: ", error);
                                    mUsernameEditText.setText(Preferences.getInstance().getUsername());
                                }
                            });

                        } else {
                            new SweetAlertDialog(mActivity, SweetAlertDialog.ERROR_TYPE).setContentText(mActivity.getString(R.string.username_exists, newUsername)).show();
                            mUsernameEditText.setText(Preferences.getInstance().getUsername());
                        }

                    } catch (Exception e) {
                        mUsernameEditText.setText(Preferences.getInstance().getUsername());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (mActivityState == null || mActivityState.isActivityDestroyed())
                        return;
                    mUsernameEditText.setText(Preferences.getInstance().getUsername());
                }
            });
        }

    }
}
