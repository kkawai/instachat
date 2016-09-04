package com.google.firebase.codelab.friendlychat;

import android.app.Activity;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.initech.Constants;
import com.initech.api.NetworkApi;
import com.initech.model.User;
import com.initech.util.Preferences;
import com.initech.util.StringUtil;

import org.json.JSONObject;

/**
 * Created by kevin on 9/4/2016.
 */
public class DrawerHelper {
    private Activity mActivity;
    private DrawerLayout mDrawerLayout;

    public DrawerHelper(Activity activity, DrawerLayout drawerLayout) {
        mActivity = activity;
        mDrawerLayout = drawerLayout;
    }
    public void setup(NavigationView navigationView) {
        View headerLayout = navigationView.getHeaderView(0); // 0-index header
        final TextView email = (TextView)headerLayout.findViewById(R.id.nav_email);
        final TextView username = (TextView)headerLayout.findViewById(R.id.nav_username);
        final ImageView navpic = (ImageView)headerLayout.findViewById(R.id.nav_pic);
        if (email != null) {
            email.setText(Preferences.getInstance(mActivity).getEmail());
            username.setText(Preferences.getInstance(mActivity).getUsername());
            Glide.with(mActivity)
                    .load(Constants.DP_URL(Preferences.getInstance(mActivity).getUserId()))
                    .error(R.drawable.ic_account_circle_black_36dp)
                    .crossFade()
                    .into(navpic);
        }

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                final String existing = Preferences.getInstance(mActivity).getUsername();
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
                        try {
                            if (!response.getJSONObject("data").getBoolean("exists")) {

                                final User user = Preferences.getInstance(mActivity).getUser();
                                user.setUsername(newUsername);
                                NetworkApi.saveUser(mActivity, user, new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            JSONObject object = new JSONObject(response);
                                            if (object.getString("status").equals("OK")) {
                                                String toast = mActivity.getString(R.string.is_your_new_username,newUsername);
                                                Toast.makeText(mActivity, toast, Toast.LENGTH_SHORT).show();
                                                user.setUsername(newUsername);
                                                Preferences.getInstance(mActivity).saveUser(user);
                                                username.setText(newUsername);
                                                Preferences.getInstance(mActivity).saveLastSignIn(newUsername);
                                            }
                                        }catch(Exception e) {
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

                        }catch(Exception e) {
                            username.setText(existing);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        username.setText(existing);
                    }
                });
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    public void cleanup() {
        mActivity = null;
        mDrawerLayout = null;
    }
}
