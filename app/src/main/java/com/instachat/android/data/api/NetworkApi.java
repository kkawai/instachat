package com.instachat.android.data.api;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.TheApp;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.data.model.User;
import com.instachat.android.util.Base64;
import com.instachat.android.util.DeviceUtil;
import com.instachat.android.util.HttpMessage;
import com.instachat.android.util.MLog;
import com.instachat.android.util.SimpleRxWrapper;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.inject.Inject;

import androidx.core.util.Pair;

public class NetworkApi {

    private static Pair<String, String> sPair;
    private static String G_API_KEY;
    private static boolean isInitialized;

    private static final String TAG = "NetworkApi";

    private static final int REQUEST_TIMEOUT_MS = 10000;

    public static final String RESPONSE_OK = "OK";
    public static final String RESPONSE_DATA = "data";
    public static final String KEY_RESPONSE_STATUS = "status";
    public static final String KEY_EXISTS = "exists";

    final RequestQueue requestQueue;

    private static final DefaultRetryPolicy DEFAULT_RETRY_POLICY = new DefaultRetryPolicy(REQUEST_TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

    @Inject
    public NetworkApi(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public static void init() {
        if (!isInitialized) {
            SimpleRxWrapper.executeInWorkerThread(new Runnable() {
                @Override
                public void run() {
                    getRemoteSettings();
                }
            });
        }
    }

    public static String GKEY() {
        if (G_API_KEY == null) {
            SimpleRxWrapper.executeInWorkerThread(new Runnable() {
                @Override
                public void run() {
                    init();
                }
            });
            return "not_available";
        }
        return G_API_KEY;
    }

    public static Pair amazonPair() {
        if (sPair == null) {
            SimpleRxWrapper.executeInWorkerThread(new Runnable() {
                @Override
                public void run() {
                    init();
                }
            });
        }
        return sPair;
    }

    private static void getRemoteSettings() {
        try {
            final JSONObject r = new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/ih/settings").getString())
                    .getJSONObject(NetworkApi.RESPONSE_DATA);
            sPair = new Pair<>(r.getString("a"), r.getString("s"));
            G_API_KEY = r.getString("gkey");
            isInitialized = true;
        } catch (final Exception e) {
            MLog.e(TAG, "remote settings failed", e);
        }
    }

    public void getUserByEmail(final Object cancelTag, final String email, final Response.Listener<JSONObject>
            listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/ih/getubyem?em=" + Base64.encodeWebSafe(email.getBytes(), false);
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        requestQueue.add(request);
    }

    public void getUserByEmailOrUsernamePassword(final Object cancelTag, final String email, final String pw,
                                                        String ltuEmail, final Response.Listener<String>
                                                                listener, final Response.ErrorListener errorListener) {

        final HashMap<String, String> params = new HashMap<>(3);
        params.put("em", email+"");
        params.put("pd", Base64.encodeWebSafe(pw.getBytes(),false));
        if (ltuEmail != null)
            params.put("nem", ltuEmail);
        final ApiPostRequest request = new ApiPostRequest(params,
                Constants.API_BASE_URL + "/ih/getu",
                listener, errorListener);
        request.setTag(cancelTag);
        requestQueue.add(request);
    }

    public void isValidPhone(final Object cancelTag, final int userid, final String phone, final Response.Listener<JSONObject>
            listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/ih/exists?pn=" + phone+"&i="+userid;
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        requestQueue.add(request);
    }

    public void isExistsEmail(final Object cancelTag, final String email, final Response.Listener<JSONObject>
            listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/ih/exists?em=" + email;
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        requestQueue.add(request);
    }

    public void isExistsUsername(final Object cancelTag, final String username, final Response
            .Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        final String url = Constants.API_BASE_URL + "/ih/exists?un=" + username;
        final Request request = new ApiGetRequest(url, listener, errorListener);
        request.setShouldCache(false).setRetryPolicy(DEFAULT_RETRY_POLICY).setTag(cancelTag);
        requestQueue.add(request);
    }

    public void saveUser(final Object tag, final User user, final Response.Listener<String> responder,
                                Response.ErrorListener errorListener) {

        String appName = TheApp.getInstance().getString(R.string.app_name);
        if (TextUtils.isEmpty(user.getBio())) {
            user.setBio(TheApp.getInstance().getString(R.string.default_bio2));
        }

        final HashMap<String, String> params = new HashMap<>();
        params.put("user", user.toJSON().toString());
        final ApiPostRequest request = new ApiPostRequest(params, Constants.API_BASE_URL + "/ih/saveuser2",
                responder, errorListener);
        MLog.d(TAG, "saving user.  json: ", params.get("user"));
        request.setTag(tag);
        requestQueue.add(request);

        if (user.getId() != 0) {
            FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(user.getId())).updateChildren(user.toMap());
        }
    }

    public void forgotPassword(final Object tag, final String usernameOrEmail, final Response.Listener<String>
            responder, Response.ErrorListener errorListener) {
        final HashMap<String, String> params = new HashMap<>();
        params.put("emun", usernameOrEmail);
        final ApiPostRequest request = new ApiPostRequest(params, Constants.API_BASE_URL + "/ih/fgp", responder,
                errorListener);
        request.setTag(tag);
        requestQueue.add(request);
    }

    /*public static int gcmcount(final int userid) throws Exception {
        return new HttpMessage(Constants.API_BASE_URL + "/ih/gcmcount?iid=" + userid).getInt();
    }*/

    public void getOnlineStatus(Map<String, PrivateChatSummary> users) throws Exception {
        if (users.size() == 0) {
            return;
        }

        final StringBuilder sb = new StringBuilder(users.size() * 13);

        for (String key : users.keySet()) {
            sb.append(key).append(' ');
        }

        MLog.i(TAG, "Getting users who actually installed by looking at gcm records");
        final JSONObject response = new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/ih/whoinstalled").post
                ("list", sb.toString()));
        if (response.getString(KEY_RESPONSE_STATUS).equals(RESPONSE_OK)) {
            final String installed = response.optString("descr");
            if (StringUtil.isNotEmpty(installed)) {
                final StringTokenizer st = new StringTokenizer(installed);
                while (st.hasMoreTokens()) {
                    final String installedId = st.nextToken().trim();
                    PrivateChatSummary privateChatSummary = users.get(installedId);
                    if (privateChatSummary == null)
                        continue;
                    if (System.currentTimeMillis() - privateChatSummary.getLastOnline() > PrivateChatSummary
                            .TWELVE_HOURS) {
                        privateChatSummary.setOnlineStatus(PrivateChatSummary.USER_AWAY);
                        MLog.i(TAG, "getOnlineStatus() ", privateChatSummary.getName(), " is away");
                    } else {
                        privateChatSummary.setOnlineStatus(PrivateChatSummary.USER_ONLINE);
                        MLog.i(TAG, "getOnlineStatus() ", privateChatSummary.getName(), " is online(recent)");
                    }
                }
            }
        } else {
            MLog.e(TAG, "Error from server: ", response);
        }
    }

    public void gcmreg(final Context context, final String regid) throws Exception {

        final String androidId = DeviceUtil.getAndroidId(context);
        final HashMap<String, String> params = new HashMap<>();
        params.put("iid", UserPreferences.getInstance().getUserId() + "");
        params.put("deviceid", androidId);
        params.put("regid", regid);
        final JSONObject response = new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/ih/gcmreg").post(params));
        if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
            MLog.i(TAG, "debugx registered successfully at server ");
        } else {
            MLog.e(TAG, "Error from server: ", response);
        }
    }

    public void gcmunreg(final Context context, final String userid, final String regid) throws Exception {

        final String androidId = DeviceUtil.getAndroidId(context);
        final HashMap<String, String> params = new HashMap<>();
        params.put("iid", userid);
        params.put("deviceid", androidId);
        params.put("regid", regid);
        final JSONObject response = new JSONObject(new HttpMessage(Constants.API_BASE_URL + "/ih/gcmunreg").post
                (params));
        if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
            MLog.i(TAG, "unregistered successfully at server ");
        } else {
            MLog.e(TAG, "Error from server: ", response);
        }
    }

    public void gcmsend(final int toid, final Constants.GcmMessageType messageType, final JSONObject msg) {

        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                try {
                    msg.put(Constants.KEY_GCM_MSG_TYPE, messageType.name());
                    final HashMap<String, String> params = new HashMap<>();
                    params.put(Constants.KEY_TO_USERID, toid + ""); //DON'T CHANGE KEY, SERVER DEPENDENT
                    params.put(Constants.KEY_GCM_MESSAGE, msg.toString()); //DON'T CHANGE KEY, SERVER
                    final JSONObject response = new JSONObject(new HttpMessage(Constants.API_BASE_URL +
                            "/ih/gcmsend").post(params));
                    if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        MLog.i(TAG, "sent gcm message to server: " + response.optString("descr"));
                    } else {
                        MLog.e(TAG, "gcmsend() failed. Error from server: ", response, " toid: ", toid, " msg: ", msg
                                .toString());
                    }
                } catch (Exception e) {
                    MLog.e(TAG, "NetworkApi.gcmsend() failed", e);
                }
            }
        });

    }
}
