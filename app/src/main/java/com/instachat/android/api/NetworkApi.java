package com.instachat.android.api;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v4.util.Pair;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.instachat.android.Constants;
import com.instachat.android.MyApp;
import com.instachat.android.model.User;
import com.instachat.android.util.Base64;
import com.instachat.android.util.DeviceUtil;
import com.instachat.android.util.HttpMessage;
import com.instachat.android.util.LocalFileUtils;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.ThreadWrapper;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by kevin on 7/22/2016.
 */
public final class NetworkApi {

    private static Pair<String, String> sPair = null;

    private static final String TAG = "NetworkApi";

    private static final int REQUEST_TIMEOUT_MS = 10000;

    public static final String RESPONSE_OK = "OK";
    public static final String KEY_RESPONSE_STATUS = "status";

    private static final DefaultRetryPolicy DEFAULT_RETRY_POLICY = new DefaultRetryPolicy(
            REQUEST_TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

    public static Pair pair() {
        if (sPair == null) {
            getRemoteSettings();
        }
        return sPair;
    }

    public static JSONObject getRemoteSettings() {
        try {
            final JSONObject r = new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/settings").getString()).getJSONObject("data");
            sPair = new Pair<>(r.getString("a"), r.getString("s"));
            return r;
        } catch (final Exception e) {
            MLog.e(TAG, "remote settings failed", e);
            return null;
        }
    }

    public static void getUserByEmail(final Object cancelTag, final String email, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/getubyem?em=" + Base64.encodeWebSafe(email.getBytes(), false);
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void getUserById(final Object cancelTag, final int userid, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/getubid?i=" + userid;
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void getUserByEmailPassword(final Object cancelTag, final String email, final String pw, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/getu?em=" + email + "&pd=" + Base64.encodeWebSafe(pw.getBytes(), false);
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

        final HashMap<String, String> params = new HashMap<>(1);
        params.put("user", user.toJSON().toString());
        final ApiPostRequest request = new ApiPostRequest(params, Constants.API_BASE_URL + "/saveuser2", responder, errorListener);
        request.setTag(tag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void forgotPassword(final Object tag, final String usernameOrEmail, final Response.Listener<String> responder, Response.ErrorListener errorListener) {
        final HashMap<String, String> params = new HashMap<>(1);
        params.put("emun", usernameOrEmail);
        final ApiPostRequest request = new ApiPostRequest(params, Constants.API_BASE_URL + "/fgp", responder, errorListener);
        request.setTag(tag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void saveThirdPartyPhoto(final String thirdPartyProfilePicUrl) {

        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(Environment.getExternalStorageDirectory() + "/photos");
                    File file = new File(dir, UUID.randomUUID().toString() + ".jpg");
                    try {
                        // Create directory if it does not exist.
                        if (!dir.exists()) {
                            dir.mkdir();
                        }
                        boolean created = file.createNewFile();
                        MLog.d(TAG, "file.createNewFile:" + file.getAbsolutePath() + ":" + created);
                    } catch (IOException e) {
                        Log.e(TAG, "file.createNewFile" + file.getAbsolutePath() + ":FAILED", e);
                    }

                    // Create content:// URI for file, required since Android N
                    // See: https://developer.android.com/reference/android/support/v4/content/FileProvider.html
                    final Uri fileUri = FileProvider.getUriForFile(MyApp.getInstance(),
                            MyApp.getInstance().getPackageName()+".fileprovider", file);

                    LocalFileUtils.downloadFile(thirdPartyProfilePicUrl, file, null, null);

                    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                    String childRef = Constants.DP_STORAGE_BASE(Preferences.getInstance().getUserId());
                    storageReference = storageReference.child(childRef).child(fileUri.getLastPathSegment());
                    storageReference.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            final User user = Preferences.getInstance().getUser();
                            user.setProfilePicUrl(fileUri.getLastPathSegment());
                            NetworkApi.saveUser(null, user, new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    user.setProfilePicUrl(fileUri.getLastPathSegment());
                                    Preferences.getInstance().saveUser(user);
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    MLog.e(TAG, "NetworkApi.saveUser() failed inside saveThirdPartyPhoto()", error);
                                }
                            });
                        }
                    });
                } catch (final Exception e) {
                    MLog.e(TAG, "Failed to upload photo to google cloud storage", e);
                }
            }
        });

    }

    public static void gcmreg(final Context context, final String regid) throws Exception {

        final String androidId = DeviceUtil.getAndroidId(context);
        final HashMap<String, String> params = new HashMap<>();
        params.put("iid", Preferences.getInstance().getUserId() + "");
        params.put("deviceid", androidId);
        params.put("regid", regid);
        final JSONObject response = new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/gcmreg").post(params));
        if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
            MLog.i(TAG, "debugx registered successfully at server ");
        } else {
            MLog.e(TAG, "Error from server: ", response);
        }
    }

    public static void gcmunreg(final Context context, final String userid, final String regid) throws Exception {

        final String androidId = DeviceUtil.getAndroidId(context);
        final HashMap<String, String> params = new HashMap<>();
        params.put("iid", userid);
        params.put("deviceid", androidId);
        params.put("regid", regid);
        final JSONObject response = new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/gcmunreg").post(params));
        if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
            MLog.i(TAG, "unregistered successfully at server ");
        } else {
            MLog.e(TAG, "Error from server: ", response);
        }
    }

    public static void gcmsend(final String toid, final Constants.GcmMessageType messageType, final JSONObject msg) {

        ThreadWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                try {
                    msg.put(Constants.KEY_GCM_MSG_TYPE, messageType.name());
                    final HashMap<String, String> params = new HashMap<>();
                    params.put(Constants.KEY_TO_USERID, toid);
                    params.put(Constants.KEY_GCM_MESSAGE, msg.toString());
                    final JSONObject response = new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/gcmsend").post(params));
                    if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        MLog.i(TAG, "sent gcm message to server: " + response.optString("descr"));
                    } else {
                        MLog.e(TAG, "gcmsend() failed. Error from server: ", response, " toid: ", toid, " msg: ", msg.toString());
                    }
                } catch (Exception e) {
                    MLog.e(TAG, "NetworkApi.gcmsend() failed", e);
                }
            }
        });

    }
}
