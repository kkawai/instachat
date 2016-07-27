package com.initech.api;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

public class ApiGetRequest extends JsonObjectRequest {

    private static final String TAG = ApiGetRequest.class.getSimpleName();

    public ApiGetRequest(final String url, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        super(Request.Method.GET, url, null, listener, errorListener);

    }
}
