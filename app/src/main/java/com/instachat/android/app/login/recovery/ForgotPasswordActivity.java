package com.instachat.android.app.login.recovery;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.firebase.auth.FirebaseAuth;
import com.instachat.android.R;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.model.User;
import com.instachat.android.databinding.ActivityForgotPasswordBinding;
import com.instachat.android.di.component.DaggerAppComponent;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;

import org.json.JSONObject;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    @Inject
    NetworkApi networkApi;

    @Inject
    RequestQueue requestQueue;

    private ActivityForgotPasswordBinding activityForgotPasswordBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject();
        ActivityUtil.hideStatusBar(getWindow());
        activityForgotPasswordBinding = DataBindingUtil.setContentView(this, R.layout.activity_forgot_password);


        String lastSignIn = UserPreferences.getInstance().getLastSignIn();
        MLog.i(TAG, "lastSignIn ", lastSignIn);
        if (lastSignIn != null) {
            activityForgotPasswordBinding.inputEmailLayout.getEditText().setText(lastSignIn);

        }
    }

    private void find() {
        final String emailOrUsername = activityForgotPasswordBinding.inputEmailLayout.getEditText().getText().toString();
        if (StringUtil.isEmpty(emailOrUsername)) {
            new SweetAlertDialog(ForgotPasswordActivity.this, SweetAlertDialog.NORMAL_TYPE)
                    .setContentText(getString(R.string.please_enter_username_or_email))
                    .show();
            return;
        }

        networkApi.getUserByEmail(this, emailOrUsername, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    MLog.e(TAG, "getUserByEmail() : " + response);
                    final String status = response.getString(NetworkApi.KEY_RESPONSE_STATUS);
                    if (status.equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        final User user = User.fromResponse(response);
                        FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail());
                        SweetAlertDialog sweetAlertDialog =
                                new SweetAlertDialog(ForgotPasswordActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                                        .setContentText(getString(R.string.password_reset_link_sent) + " " + user.getEmail());
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
                    MLog.e(TAG, e);
                    showErrorToast(R.string.general_api_error);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                MLog.e(TAG, "failed: " + error);
                showErrorToast(R.string.general_api_error);
            }
        });

    }

    public void onFindAccount(View v) {
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
