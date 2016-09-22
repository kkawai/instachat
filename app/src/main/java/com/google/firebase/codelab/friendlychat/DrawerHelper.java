package com.google.firebase.codelab.friendlychat;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.initech.Constants;
import com.initech.api.NetworkApi;
import com.initech.model.User;
import com.initech.util.MLog;
import com.initech.util.Preferences;
import com.initech.util.StringUtil;

import org.json.JSONObject;

/**
 * Created by kevin on 9/4/2016.
 */
public class DrawerHelper {
    private static final String TAG = "DrawerHelper";
    private Activity mActivity;
    private DrawerLayout mDrawerLayout;
    private PhotoUploadHelper mPhotoUploadHelper;
    private View mHeaderLayout;

    public DrawerHelper(Activity activity, DrawerLayout drawerLayout, PhotoUploadHelper photoUploadHelper) {
        mActivity = activity;
        mDrawerLayout = drawerLayout;
        mPhotoUploadHelper = photoUploadHelper;
    }

    public void updateProfilePic(String dpid) {
        if (isActivityDestroyed())
            return;
        final ImageView navpic = (ImageView) mHeaderLayout.findViewById(R.id.nav_pic);
        Glide.clear(navpic);
        final User user = Preferences.getInstance().getUser();
        Constants.DP_URL(user.getId(), dpid, new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (isActivityDestroyed()) {
                    return;
                }
                try {
                    if (!task.isSuccessful()) {
                        navpic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                        return;
                    }
                    Glide.with(mActivity)
                            .load(task.getResult().toString())
                            .error(R.drawable.ic_account_circle_black_36dp)
                            .crossFade()
                            .into(navpic);
                } catch (Exception e) {
                    MLog.e(TAG, "DP_URL failed", e);
                    navpic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                }
            }
        });
    }

    public void setup(NavigationView navigationView) {
        mHeaderLayout = navigationView.getHeaderView(0); // 0-index header
        final TextView email = (TextView) mHeaderLayout.findViewById(R.id.nav_email);
        final TextView username = (TextView) mHeaderLayout.findViewById(R.id.nav_username);
        final ImageView navpic = (ImageView) mHeaderLayout.findViewById(R.id.nav_pic);
        navpic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChooseDialog();
            }
        });

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                final User user = Preferences.getInstance().getUser();
                email.setText(Preferences.getInstance().getEmail());
                username.setText(Preferences.getInstance().getUsername());
                Constants.DP_URL(user.getId(), user.getProfilePicUrl(), new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (isActivityDestroyed())
                            return;
                        if (!task.isSuccessful()) {
                            navpic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                            return;
                        }
                        try {
                            Glide.with(mActivity)
                                    .load(task.getResult().toString())
                                    .error(R.drawable.ic_account_circle_black_36dp)
                                    .crossFade()
                                    .into(navpic);
                        } catch (Exception e) {
                            MLog.e(TAG, "onDrawerOpened() could not find user photo in google cloud storage", e);
                            navpic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                        }
                        checkForRemoteUpdatesToMyDP();
                    }
                });

            }

            @Override
            public void onDrawerClosed(View drawerView) {

                if (isActivityDestroyed())
                    return;

                final String existing = Preferences.getInstance().getUsername();
                final String newUsername = username.getText().toString();
                if (existing.equals(newUsername)) {
                    return;
                }
                if (!StringUtil.isValidUsername(newUsername)) {
                    username.setText(existing);
                    return;
                }
                NetworkApi.isExistsUsername(mActivity, newUsername, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        if (isActivityDestroyed())
                            return;
                        try {
                            if (!response.getJSONObject("data").getBoolean("exists")) {
                                final User user = Preferences.getInstance().getUser();
                                user.setUsername(newUsername);
                                NetworkApi.saveUser(mActivity, user, new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        if (isActivityDestroyed())
                                            return;
                                        try {
                                            JSONObject object = new JSONObject(response);
                                            if (object.getString("status").equals("OK")) {
                                                String toast = mActivity.getString(R.string.is_your_new_username, newUsername);
                                                Toast.makeText(mActivity, toast, Toast.LENGTH_SHORT).show();
                                                user.setUsername(newUsername);
                                                Preferences.getInstance().saveUser(user);
                                                username.setText(newUsername);
                                                Preferences.getInstance().saveLastSignIn(newUsername);
                                            }
                                        } catch (Exception e) {
                                            username.setText(existing);
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        username.setText(existing);
                                    }
                                });

                            } else {
                                username.setText(existing);
                            }

                        } catch (Exception e) {
                            username.setText(existing);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isActivityDestroyed())
                            return;
                        username.setText(existing);
                    }
                });
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
                    if (isActivityDestroyed())
                        return;
                    final String status = response.getString("status");
                    if (status.equalsIgnoreCase("OK")) {
                        final User remote = User.fromResponse(response);
                        if (!TextUtils.isEmpty(remote.getProfilePicUrl())) {
                            User local = Preferences.getInstance().getUser();
                            final ImageView navpic = (ImageView) mHeaderLayout.findViewById(R.id.nav_pic);
                            if (TextUtils.isEmpty(local.getProfilePicUrl()) || !remote.getProfilePicUrl().equals(local.getProfilePicUrl())) {
                                Constants.DP_URL(remote.getId(), remote.getProfilePicUrl(), new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (!task.isSuccessful() || isActivityDestroyed()) {
                                            return;
                                        }
                                        User user = Preferences.getInstance().getUser();
                                        user.setProfilePicUrl(remote.getProfilePicUrl());
                                        Preferences.getInstance().saveUser(user);

                                        MLog.i(TAG, "checkForRemoteUpdatesToMyDP() my pic changed remotely. attempt to update");
                                        try {
                                            Glide.with(mActivity)
                                                    .load(task.getResult().toString())
                                                    .error(R.drawable.ic_account_circle_black_36dp)
                                                    .crossFade()
                                                    .into(navpic);
                                        } catch (Exception e) {
                                            MLog.e(TAG, "onDrawerOpened() could not find my photo in google cloud storage", e);
                                        }
                                    }
                                });
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
        mActivity = null;
        mDrawerLayout = null;
        mPhotoUploadHelper = null;
    }

    private boolean isActivityDestroyed() {
        return mActivity == null || mActivity.isFinishing();
    }

    private void showChooseDialog() {
        if (isActivityDestroyed())
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
        mPhotoUploadHelper.setPhotoType(PhotoUploadHelper.PhotoType.userProfilePhoto);
        mPhotoUploadHelper.setStorageRefString(Constants.DP_STORAGE_BASE(Preferences.getInstance().getUserId()));
        view.findViewById(R.id.menu_choose_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                mPhotoUploadHelper.launchCamera(true);
            }
        });
        view.findViewById(R.id.menu_take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                mPhotoUploadHelper.launchCamera(false);
            }
        });
        dialog.show();

    }
}