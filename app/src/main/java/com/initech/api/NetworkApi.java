package com.initech.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.bumptech.glide.Glide;
import com.google.common.io.Files;
import com.initech.Constants;
import com.initech.MyApp;
import com.initech.model.User;
import com.initech.util.Base64;
import com.initech.util.HttpMessage;
import com.initech.util.ImageUtils;
import com.initech.util.LocalFileUtils;
import com.initech.util.MLog;
import com.initech.util.Preferences;
import com.initech.util.ThreadWrapper;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by kevin on 7/22/2016.
 */
public final class NetworkApi {

    private static final String TAG = "NetworkApi";

    private static final int REQUEST_TIMEOUT_MS = 10000;

    private static final DefaultRetryPolicy DEFAULT_RETRY_POLICY = new DefaultRetryPolicy(
            REQUEST_TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

    public static JSONObject getRemoteSettings() {
        try {
            return new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/settings").getString()).getJSONObject("data");
        } catch (final Exception e) {
            MLog.e(TAG, "remote settings failed", e);
            return null;
        }
    }

    public static void getUserByEmail(final Object cancelTag, final String email, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/getubyem?em=" + Base64.encodeWebSafe(email.getBytes(),false);
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void getUserByUsernamePassword(final Object cancelTag, final String username, final String pw, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/getu?un=" + username+"&pd="+ Base64.encodeWebSafe(pw.getBytes(),false);
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void getUserByEmailPassword(final Object cancelTag, final String email, final String pw, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/getu?em=" + email+"&pd="+ Base64.encodeWebSafe(pw.getBytes(),false);
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void isExistsEmail(final Object cancelTag, final String email, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/exists?em=" + email;
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void isExistsUsername(final Object cancelTag, final String username, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/exists?un=" + username;
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void saveUser(final Object tag, final User user, final Response.Listener<String> responder, Response.ErrorListener errorListener) {

        try {
            user.setLat(MyApp.lat);
            user.setLon(MyApp.lon);
        } catch (final Exception e) {
            // TODO: log
        }

        final HashMap<String,String> params = new HashMap<>(1);
        params.put("user", user.toJSON().toString());
        final ApiPostRequest request = new ApiPostRequest(params, Constants.API_BASE_URL + "/saveuser2", responder, errorListener);
        request.setTag(tag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void forgotPassword(final Object tag, final String usernameOrEmail, final Response.Listener<String> responder, Response.ErrorListener errorListener) {
        final HashMap<String,String> params = new HashMap<>(1);
        params.put("emun", usernameOrEmail);
        final ApiPostRequest request = new ApiPostRequest(params, Constants.API_BASE_URL + "/fgp", responder, errorListener);
        request.setTag(tag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void uploadMyPhotoToS3(final User user) {

        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File file = new File(MyApp.getInstance().getCacheDir().getPath() + "/me.jpg");
                    LocalFileUtils.downloadFile(user.getProfilePicUrl(),file,null,null);
                    new FileUploadApi().postFileToS3(file, "dp_"+user.getId(), Constants.AMAZON_BUCKET_DP_IC, null);
                    user.setProfilePicUrl("http://"+Constants.AMAZON_BUCKET_DP_IC+".s3.amazonaws.com/dp_"+user.getId());
                    saveUser(null, user, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Preferences.getInstance(MyApp.getInstance()).saveUser(user);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });
                } catch (final Exception e) {
                    MLog.e(TAG,"Failed to upload photo to s3", e);
                }
            }
        });

    }
}
