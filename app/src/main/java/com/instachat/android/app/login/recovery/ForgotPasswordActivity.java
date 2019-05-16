package com.instachat.android.app.login.recovery;

import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.firebase.auth.FirebaseAuth;
import com.instachat.android.R;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.di.component.DaggerAppComponent;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.util.StringUtil;

import org.json.JSONObject;

import javax.inject.Inject;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ForgotPasswordActivity";

    @Inject
    NetworkApi networkApi;

    @Inject
    RequestQueue requestQueue;

    private TextInputLayout emailLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject();
        ActivityUtil.hideStatusBar(getWindow());
        DataBindingUtil.setContentView(this, R.layout.activity_forgot_password);
        emailLayout = (TextInputLayout) findViewById(R.id.input_email_layout);
        findViewById(R.id.input_username_or_email).setOnClickListener(this);

        // Set click listeners
        findViewById(R.id.find_account_button).setOnClickListener(this);

        String lastSignIn = UserPreferences.getInstance().getLastSignIn();
        MLog.i(TAG, "lastSignIn ", lastSignIn);
        if (lastSignIn != null) {
            emailLayout.getEditText().setText(lastSignIn);
        }
    }

    private void find() {
        final String emailOrUsername = emailLayout.getEditText().getText().toString();
        if (StringUtil.isEmpty(emailOrUsername)) {
            new SweetAlertDialog(ForgotPasswordActivity.this, SweetAlertDialog.NORMAL_TYPE)
                    .setContentText(getString(R.string.please_enter_username_or_email))
                    .show();
            return;
        }
        networkApi.forgotPassword(this, emailOrUsername, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                try {
                    final JSONObject object = new JSONObject(response);
                    final String status = object.getString(NetworkApi.KEY_RESPONSE_STATUS);
                    if (status.equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(emailOrUsername);
                        SweetAlertDialog sweetAlertDialog =
                                new SweetAlertDialog(ForgotPasswordActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                                .setContentText(getString(R.string.information_emailed));
                        sweetAlertDialog.show();
                        sweetAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                finish();
                            }
                        });
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
        requestQueue.cancelAll(this);
        super.onDestroy();
    }

    private void showErrorToast(final int stringResId) {
        if (isFinishing())
            return;
        new SweetAlertDialog(ForgotPasswordActivity.this, SweetAlertDialog.ERROR_TYPE)
                .setContentText(getString(stringResId))
                .show();
    }

    private void inject() {
        DaggerAppComponent.builder()
                .application(getApplication())
                .build()
                .inject(this);
    }

}
