package com.instachat.android.app.login.signup;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.instachat.android.R;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.model.User;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.FontUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;

import org.json.JSONObject;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import cn.pedant.SweetAlert.SweetAlertDialog;
import dagger.android.AndroidInjection;

/**
 * Created by kevin on 7/18/2016.
 */
public class SignUpActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener {
    private static final String TAG = "SignUpActivity";
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout usernameLayout;
    private String email, username, password;
    private FirebaseAuth mFirebaseAuth;

    @Inject
    NetworkApi networkApi;

    @Inject
    RequestQueue requestQueue;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        ActivityUtil.hideStatusBar(getWindow());
        DataBindingUtil.setContentView(this, R.layout.activity_sign_up);
        findViewById(R.id.create_account_button).setOnClickListener(this);
        emailLayout = findViewById(R.id.input_email_layout);
        passwordLayout = findViewById(R.id.input_password_layout);
        usernameLayout = findViewById(R.id.input_username_layout);
        mFirebaseAuth = FirebaseAuth.getInstance();

        FontUtil.setTextViewFont(emailLayout);
        FontUtil.setTextViewFont(passwordLayout);
        FontUtil.setTextViewFont(usernameLayout);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.create_account_button:
                validateAccount();
                break;
            default:
                return;
        }
    }

    @Override
    public void onFocusChange(final View v, final boolean hasFocus) {
        if (v.getId() == R.id.input_email && hasFocus) {
            emailLayout.setError("");
        }
        if (v.getId() == R.id.input_password && hasFocus) {
            passwordLayout.setError("");
        }
        if (v.getId() == R.id.input_username && hasFocus) {
            usernameLayout.setError("");
        }
    }

    private void validateAccount() {
        ScreenUtil.hideKeyboard(this);

        email = emailLayout.getEditText().getText().toString().trim();
        emailLayout.getEditText().setOnFocusChangeListener(this);

        password = passwordLayout.getEditText().getText().toString().trim();
        passwordLayout.getEditText().setOnFocusChangeListener(this);

        username = usernameLayout.getEditText().getText().toString().trim();
        usernameLayout.getEditText().setOnFocusChangeListener(this);

        if (!StringUtil.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.invalid_email));
        } else if (!StringUtil.isValidUsername(username)) {
            usernameLayout.setError(getString(R.string.invalid_username));
        } else if (!StringUtil.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.invalid_password));
        } else {
            emailLayout.setError("");
            usernameLayout.setError("");
            passwordLayout.setError("");
            remotelyValidateEmail();
        }
    }

    private void remotelyValidateEmail() {
        networkApi.isExistsEmail(this, email,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        //status:OK
                        //exists:true|false
                        try {
                            if (!response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                                showErrorToast("1");
                            } else if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK) && response.getJSONObject(NetworkApi.RESPONSE_DATA).getBoolean(NetworkApi.KEY_EXISTS)) {
                                emailLayout.setError(getString(R.string.email_exists) + " " + email);
                            } else {
                                remotelyValidateUsername();
                            }
                        } catch (final Exception e) {
                            showErrorToast("2");
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        showErrorToast("3");
                    }
                });
    }

    private void remotelyValidateUsername() {
        networkApi.isExistsUsername(this, username,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        //status:OK
                        //exists:true|false
                        try {
                            if (!response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                                showErrorToast("1");
                            } else if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK) && response.getJSONObject(NetworkApi.RESPONSE_DATA).getBoolean(NetworkApi.KEY_EXISTS)) {
                                usernameLayout.setError(getString(R.string.username_exists) + " " +username);
                            } else {
                                createAccount();
                            }
                        } catch (final Exception e) {
                            showErrorToast("2");
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        showErrorToast("3");
                    }
                });
    }

    private void showErrorToast(final String distinctScreenCode) {
        try {
            Toast.makeText(this, getString(R.string.general_api_error) + " " + distinctScreenCode, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    private void createAccount() {
        final User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setUsername(username);

        networkApi.saveUser(this, user, new Response.Listener<String>() {
            @Override
            public void onResponse(final String string) {
                try {
                    final JSONObject response = new JSONObject(string);
                    MLog.i(TAG, "savedUser: " + string);
                    if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        user.copyFrom(response.getJSONObject(NetworkApi.RESPONSE_DATA));
                        createFirebaseAccount(user);
                    } else {
                        Toast.makeText(SignUpActivity.this, "Error creating account (1): " + response.getString("status"), Toast.LENGTH_SHORT).show();
                    }
                } catch (final Exception e) {
                    Toast.makeText(SignUpActivity.this, "Error creating account (2).  Please try again: " + e, Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                Toast.makeText(SignUpActivity.this, "Error creating account (3).  Please try again: " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createFirebaseAccount(final User user) {
        mFirebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                MLog.d(TAG, "createUserWithEmailAndPassword:onComplete:" + task.isSuccessful());

                // If sign in fails, display a message to the user. If sign in succeeds
                // the auth state listener will be notified and logic to handle the
                // signed in user can be handled in the listener.
                if (!task.isSuccessful()) {
                    MLog.w(TAG, "createFirebaseAccount", task.getException());
                    showErrorToast("Account Create Error");
                } else {

                    mFirebaseAuth.getCurrentUser().sendEmailVerification();
                    SweetAlertDialog dialog = new SweetAlertDialog(SignUpActivity.this, SweetAlertDialog.NORMAL_TYPE).setContentText(getString(R.string.email_verification_sent) + " " + user.getEmail());
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            UserPreferences.getInstance().saveLastSignIn(user.getUsername());
                            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                            FirebaseAnalytics.getInstance(SignUpActivity.this).logEvent(Events.SIGNUP_SUCCESS, null);
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                    dialog.show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        requestQueue.cancelAll(this);
        super.onDestroy();
    }

}
