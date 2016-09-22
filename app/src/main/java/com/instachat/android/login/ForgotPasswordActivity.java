/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.instachat.android.login;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.instachat.android.BaseActivity;
import com.instachat.android.R;
import com.instachat.android.MyApp;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.StringUtil;

import org.json.JSONObject;

public class ForgotPasswordActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "ForgotPasswordActivity";
    private TextInputLayout emailLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtil.hideStatusBar(getWindow());
        DataBindingUtil.setContentView(this, R.layout.activity_forgot_password);
        emailLayout = (TextInputLayout) findViewById(R.id.input_email_layout);
        findViewById(R.id.input_username_or_email).setOnClickListener(this);

        // Set click listeners
        findViewById(R.id.find_account_button).setOnClickListener(this);

        String lastSignIn = Preferences.getInstance().getLastSignIn();
        MLog.i(TAG, "lastSignIn ", lastSignIn);
        if (lastSignIn != null) {
            emailLayout.getEditText().setText(lastSignIn);
        }
    }

    private void find() {
        final String emailOrUsername = emailLayout.getEditText().getText().toString();
        if (StringUtil.isEmpty(emailOrUsername)) {
            Toast.makeText(this, R.string.please_enter_username_or_email, Toast.LENGTH_SHORT).show();
            return;
        }
        NetworkApi.forgotPassword(this, emailOrUsername, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                try {
                    final JSONObject object = new JSONObject(response);
                    final String status = object.getString("status");
                    if (status.equalsIgnoreCase("OK")) {
                        Toast.makeText(ForgotPasswordActivity.this, R.string.information_emailed, Toast.LENGTH_LONG).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (isActivityDestroyed())
                                    return;
                                finish();
                            }
                        },2000);
                    } else {
                        showErrorToast(R.string.email_password_not_found);
                    }
                } catch (final Exception e) {
                    MLog.e(TAG, "forgotPassword() ", e);
                    showErrorToast(R.string.email_password_not_found);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                MLog.e(TAG, "forgotPassword() failed: " + error);
                showErrorToast(R.string.email_password_not_found);
            }
        });
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.find_account_button:
                find();
                break;
            default:
                return;
        }
    }

    @Override
    protected void onDestroy() {
        MyApp.getInstance().getRequestQueue().cancelAll(this);
        super.onDestroy();
    }

    private void showErrorToast(final int stringResId) {
        if (isActivityDestroyed())
            return;
        Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show();
    }

}
