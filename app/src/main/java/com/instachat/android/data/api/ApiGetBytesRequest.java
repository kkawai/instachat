package com.instachat.android.data.api;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.instachat.android.util.MLog;

/**
 * Encapsulates a volley request; for bytes.  Useful for
 * downloading small files and images.  DO NOT USE
 * FOR LARGE FILES ABOVE 1 meg
 *
 * @author kkawai
 */
public final class ApiGetBytesRequest extends Request<byte[]> {

   /**
    * hasBeenDelivered() or markDelivered() from the base
    * class don't conveniently suit our needs at this time.
    */
   private boolean mIsResponseDelivered;

   private final Listener<byte[]> mListener;

   /**
    * Creates a new request with the given method.
    *
    * @param method        the request {@link com.android.volley.Request.Method} to use
    * @param url           URL to fetch the string at
    * @param listener      Listener to receive the String response
    * @param errorListener Error listener, or null to ignore errors
    */
   public ApiGetBytesRequest(final int method, final String url, final Listener<byte[]> listener, final ErrorListener errorListener) {
      super(method, url, errorListener);
      mListener = listener;
      //setRetryPolicy(new VevoDefaultRetryPolicy());
   }

   /**
    * Creates a new GET request.
    *
    * @param url           URL to fetch the string at
    * @param listener      Listener to receive the String response
    * @param errorListener Error listener, or null to ignore errors
    */
   public ApiGetBytesRequest(final String url, final Listener<byte[]> listener, final ErrorListener errorListener) {
      this(Method.GET, url, listener, errorListener);
   }

   @Override
   protected void deliverResponse(final byte[] response) {
      if (shouldCache() && mIsResponseDelivered) {
         MLog.w("ApiGetBytesRequest", "bbbb networkResponse: cache response consumed already. network response ignored but cached for next call: " +
               "."+getUrl());
         return;
      }
      mListener.onResponse(response);
      if (shouldCache()) {
         mIsResponseDelivered = true;
      }
   }

   @Override
   protected Response<byte[]> parseNetworkResponse(final NetworkResponse response) {
      return Response.success(response.data, ApiGetRequest.parseIgnoreCacheHeaders(response));
   }

}
