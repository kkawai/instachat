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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.instachat.android.BaseActivity;
import com.instachat.android.Events;
import com.instachat.android.GroupChatActivity;
import com.instachat.android.MyApp;
import com.instachat.android.R;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.font.FontUtil;
import com.instachat.android.model.User;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.view.ThemedAlertDialog;

import org.json.JSONObject;

public class SignInActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, View.OnFocusChangeListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SIGN_UP = 9002;
    private TextInputLayout passwordLayout, emailLayout;
    private String email, password;
    private String thirdPartyProfilePicUrl;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtil.hideStatusBar(getWindow());
        DataBindingUtil.setContentView(this, R.layout.activity_sign_in);
        passwordLayout = (TextInputLayout) findViewById(R.id.input_password_layout);
        emailLayout = (TextInputLayout) findViewById(R.id.input_email_layout);
        findViewById(R.id.sign_in_with_email_button).setOnClickListener(this);
        findViewById(R.id.forgot_password).setOnClickListener(this);
        FontUtil.setTextViewFont(passwordLayout);
        FontUtil.setTextViewFont(emailLayout);

        // Assign fields
        findViewById(R.id.sign_in_with_google_textview).setOnClickListener(this);

        // Set click listeners
        findViewById(R.id.sign_up_button).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();

        String lastSignIn = Preferences.getInstance().getLastSignIn();
        MLog.i(TAG, "lastSignIn ", lastSignIn);
        if (lastSignIn != null) {
            emailLayout.getEditText().setText(lastSignIn);
        }
    }

    private void validateAccount() {
        ScreenUtil.hideKeyboard(this);

        email = emailLayout.getEditText().getText().toString().trim();
        emailLayout.getEditText().setOnFocusChangeListener(this);
        password = passwordLayout.getEditText().getText().toString().trim();
        passwordLayout.getEditText().setOnFocusChangeListener(this);

        if (email.contains("@") && !StringUtil.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.invalid_email));
            return;
        } else if (!email.contains("@") && !StringUtil.isValidUsername(email)) {
            emailLayout.setError(getString(R.string.invalid_username));
            return;
        }

        if (!StringUtil.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.invalid_password));
            return;
        }

        emailLayout.setError("");
        passwordLayout.setError("");
        signInWithEmailOrUsernamePassword(email, password, null);
    }

    private void signInWithEmailOrUsernamePassword(final String emailOrUsername, final String password, final String ltuEmail) {
        showProgressDialog();
        NetworkApi.getUserByEmailOrUsernamePassword(this, emailOrUsername, password, ltuEmail, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    final String status = response.getString(NetworkApi.KEY_RESPONSE_STATUS);
                    if (status.equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        final JSONObject data = response.getJSONObject(NetworkApi.RESPONSE_DATA);
                        if (data.has("needsNewEmail")) {
                            /**
                             * Welcome the user back and prompt him to enter an email address
                             * If he enters an email address make sure it's unique.
                             * If the email is valid and unique, save the user and signIntoFirebase
                             *
                             */
                            hideProgressDialog();
                            showNewEmailDialog(SignInActivity.this, emailOrUsername, password);
                            return;
                        }
                        final User user = User.fromResponse(response);
                        Preferences.getInstance().saveUser(user);
                        Preferences.getInstance().saveLastSignIn(emailOrUsername);
                        signIntoFirebase(user.getEmail(), password);
                    } else {
                        showErrorToast(R.string.email_password_not_found);
                    }
                } catch (final Exception e) {
                    MLog.e(TAG, e);
                    showErrorToast("bad response 1");
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                MLog.e(TAG, "signInWithEmailOrUsernamePassword() failed: " + error);
                showErrorToast("network 1");
            }
        });
    }

    private void signIntoFirebase(final String email, final String password) {

        mFirebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                MLog.d(TAG, "mFirebaseAuth.signInWithEmailAndPassword(): " + task.isSuccessful(), " ", email, " ", password);
                if (!task.isSuccessful()) {
                    if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                        createFirebaseAccount(email, password);
                        return;
                    }
                    MLog.w(TAG, "mFirebaseAuth.signInWithEmailAndPassword(): ", task.getException());
                    showErrorToast("Sign In Error");
                } else {
                    finallyGoChat();
                }
            }
        });
    }

    private void finallyGoChat() {
        hideProgressDialog();
        User user = Preferences.getInstance().getUser();
        if (StringUtil.isEmpty(user.getProfilePicUrl())) {
            if (!TextUtils.isEmpty(thirdPartyProfilePicUrl)) {
                NetworkApi.saveThirdPartyPhoto(thirdPartyProfilePicUrl);
            }
        }
        startActivity(new Intent(SignInActivity.this, GroupChatActivity.class));
        FirebaseAnalytics.getInstance(this).logEvent(Events.LOGIN_SUCCESS, null);
        finish();
    }

    private void createFirebaseAccount(final String email, final String password) {

        mFirebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                MLog.d(TAG, "mFirebaseAuth.createFirebaseAccount(): " + task.isSuccessful(), " ", email, " ", password);
                if (!task.isSuccessful()) {
                    MLog.w(TAG, "mFirebaseAuth.createUserWithEmailAndPassword(): ", task.getException());
                    showErrorToast("Sign In Error");
                } else {
                    finallyGoChat();
                }
            }
        });
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.sign_in_with_email_button:
                validateAccount();
                break;
            case R.id.sign_in_with_google_textview:
                signInWithGoogle();
                break;
            case R.id.sign_up_button:
                signUp();
                break;
            case R.id.forgot_password:
                startActivity(new Intent(this, ForgotPasswordActivity.class));
                break;
            default:
                return;
        }
    }

    private void signInWithGoogle() {
        startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient), RC_SIGN_IN);
    }

    private void signUp() {
        startActivityForResult(new Intent(this, SignUpActivity.class), RC_SIGN_UP);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                final GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed
                MLog.e(TAG, "Google Sign In failed: " + result.toString());
                showErrorToast("Google Sign In Failed");
            }
        }
    }

    /**
     * Successfully signing in via google means that we must
     * check if the user exists in our system.
     * If exists already, then just enter GroupChatActivity.
     * If not exists, we must complete registration,
     * by asking user to enter password.
     *
     * @param acct
     */
    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
        if (acct.getPhotoUrl() != null && !TextUtils.isEmpty(acct.getPhotoUrl().toString())) {
            thirdPartyProfilePicUrl = acct.getPhotoUrl().toString();
        }
        showProgressDialog();
        NetworkApi.getUserByEmail(this, acct.getEmail(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        final User user = User.fromResponse(response);
                        Preferences.getInstance().saveUser(user);
                        Preferences.getInstance().saveLastSignIn(user.getUsername());
                        signIntoFirebase(user.getEmail(), user.getPassword());
                    } else { //user does not exist go to sign up activity
                        hideProgressDialog();
                        final Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                        if (acct.getPhotoUrl() != null)
                            intent.putExtra("photo", acct.getPhotoUrl().toString());
                        intent.putExtra("email", acct.getEmail());
                        MLog.i(TAG, "user photo: " + acct.getPhotoUrl() + " displayname " + acct.getDisplayName());
                        startActivity(intent);
                    }
                } catch (final Exception e) {
                    MLog.e(TAG, "checkUserExists() failed", e);
                    showErrorToast("1");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                MLog.e(TAG, "checkUserExists() error response: " + error);
                showErrorToast("2");
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        MLog.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFocusChange(final View v, final boolean hasFocus) {
        if (v.getId() == R.id.input_password && hasFocus) {
            passwordLayout.setError("");
        }
        if (v.getId() == R.id.input_username && hasFocus) {
            emailLayout.setError("");
        }
    }

    private String errorMessage(final int stringResId, final String str) {
        return getString(stringResId, str);
    }

    private void showErrorToast(final String distinctScreenCode) {
        hideProgressDialog();
        try {
            Toast.makeText(this, getString(R.string.general_api_error, distinctScreenCode), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    private void showErrorToast(final int stringResId) {
        hideProgressDialog();
        Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show();
    }

    private AlertDialog showNewEmailDialog(@NonNull final Context context, final String username, final String password) {
        final Object tag = new Object();
        final View view = getLayoutInflater().inflate(R.layout.dialog_input_email, null);
        final TextView textView = (TextView) view.findViewById(R.id.input_email);
        final TextInputLayout textInputLayout = (TextInputLayout) view.findViewById(R.id.input_email_layout);
        FontUtil.setTextViewFont(textView);
        final AlertDialog dialog = new ThemedAlertDialog.Builder(context).
                setView(view).
                setCancelable(false).
                setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final String ltuEmail = textView.getText().toString();
                        if (!StringUtil.isValidEmail(ltuEmail)) {
                            textInputLayout.setError(getString(R.string.invalid_email));
                            Toast.makeText(SignInActivity.this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showProgressDialog();
                        NetworkApi.isExistsEmail(tag, ltuEmail, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                if (isActivityDestroyed())
                                    return;
                                try {
                                    if (response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK) && response.getJSONObject(NetworkApi.RESPONSE_DATA).getBoolean(NetworkApi.KEY_EXISTS)) {
                                        textInputLayout.setError(errorMessage(R.string.email_exists, ltuEmail));
                                        hideProgressDialog();
                                        Toast.makeText(SignInActivity.this, errorMessage(R.string.email_exists, ltuEmail), Toast.LENGTH_SHORT).show();
                                    } else {
                                        signInWithEmailOrUsernamePassword(username, password, ltuEmail);
                                    }
                                } catch (Exception e) {
                                    showErrorToast("bad response");
                                    MLog.e(TAG, "response: ", response);
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                showErrorToast("bad network");
                                MLog.e(TAG, "", error);
                            }
                        });
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        MyApp.getInstance().getRequestQueue().cancelAll(tag);
                    }
                }).show();
        return dialog;
    }

    private void showProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle(R.string.loading);
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

}
