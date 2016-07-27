package com.initech.api;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.initech.Constants;
import com.initech.MyApp;
import com.initech.model.User;
import com.initech.util.Base64;
import com.initech.util.HttpMessage;
import com.initech.util.MLog;

import org.json.JSONObject;

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

    public static void getIHUserV2(final Object cancelTag, final String iid, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/getuser?iid=" + iid;
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void getIHUserByUsername(final Object cancelTag, final String username, final String pw, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/getu?un=" + username+"&pd="+ Base64.encodeWebSafe(pw.getBytes(),false);
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        MyApp.getInstance().getRequestQueue().add(request);
    }

    public static void getIHUserByEmail(final Object cancelTag, final String email, final String pw, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

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

    public static User saveIHUser(final User user) throws Exception {

        try {
            user.setLat(MyApp.lat);
            user.setLon(MyApp.lon);
        } catch (final Exception e) {
            // TODO: log
        }

        MLog.i(TAG, "Sending info to server for user: ", user.getInstagramId() + " : " + user.getUsername());
        final HttpMessage httpMessage = new HttpMessage(Constants.API_BASE_URL + "/saveuser");
        final JSONObject response = new JSONObject(httpMessage.post("user", user.toJSON().toString()));
        if (response.getString("status").equals("OK")) {

			/*
			 * First, PRESERVE any user data that we know is only stored at IH
			 * such as gender
			 */
            final String iid = user.getInstagramId();
            final String gender = user.getGender();
            user.copyFrom(response.getJSONObject("data"));
            user.setInstagramId(iid);
            user.setGender(gender);
            return user;
        } else {
            // failed to save
            MLog.e(TAG, "Error from server: ", response);
            return null;
        }
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
        final ApiPostRequest request = new ApiPostRequest(params, Constants.API_BASE_URL + "/saveuser", responder, errorListener);
        request.setTag(tag);
        MyApp.getInstance().getRequestQueue().add(request);
    }
}
