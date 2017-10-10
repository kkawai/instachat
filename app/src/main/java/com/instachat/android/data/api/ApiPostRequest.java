package com.instachat.android.data.api;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.Map;

/**
 * POST request with URL-encoded form parameters
 * <p>
 * Usage:
 * note that this class, due to a Volley peculiarity, extends from StringRequest NOT JsonObjectRequest, responses are Strings, convert to JSONObject
 * <p>
 * Example -- making a POST request from the app
 * ApiV1.verify( name, age, new Response.Listener<String>() {
 *
 * @Override public void onResponse(String response) {
 * String message = ApiV1.ParseHelper.parseVerify(response);
 * // etc.
 * }
 * });
 * <p>
 * Example -- parsing response:
 * public static String parseVerify(final String response) {
 * <p>
 * String message = null;
 * <p>
 * try {
 * JSONObject json = new JSONObject(response);
 * message = json.optString(Constants.KEY_GCM_MESSAGE);
 * <p>
 * } catch (JSONException e) {
 * e.printStackTrace();
 * }
 * return message;
 * }
 */

public class ApiPostRequest extends StringRequest {

    private Map<String, String> mParams;
    //private byte[] mBody;

    public ApiPostRequest(Map<String, String> params, String url, Response.Listener<String> responder, Response.ErrorListener errorListener) {
        super(Request.Method.POST, url, responder, errorListener);
        mParams = params;
    }

   /*
   public ApiPostRequest(Map<String, String> params, String url, Response.Listener<String> responder, Response.ErrorListener errorListener, byte[] body) {
      this(params, url, responder, errorListener);
      mBody = body;
   }*/

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }


   /*
   @Override
   public HashMap<String, String> getHeaders() {
      HashMap<String, String> params = new HashMap<>();
      params.put(ApiV2.AUTH_HEADER, ApiV2.AUTH_HEADER_BEARER + User.getSessionToken());
      params.put(V4Constants.KEY_VERSION_NAME, VevoApplication.versionName);
      return params;
   }
   */

   /*
   @Override
   public byte[] getBody() throws AuthFailureError {

      if (mBody != null) {
         return mBody;
      }

      return super.getBody();
   }*/

   /*
   @Override
   public String getBodyContentType() {

      if (mBody != null) {
         return "application/json";
      } else {
         return super.getBodyContentType();
      }

   }
   */
}
