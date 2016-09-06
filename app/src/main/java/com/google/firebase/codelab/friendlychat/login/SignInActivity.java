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
package com.google.firebase.codelab.friendlychat.login;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.codelab.friendlychat.MainActivity;
import com.google.firebase.codelab.friendlychat.R;
import com.initech.MyApp;
import com.initech.api.NetworkApi;
import com.initech.model.User;
import com.initech.util.ActivityUtil;
import com.initech.util.DeviceUtil;
import com.initech.util.EmailUtil;
import com.initech.util.MLog;
import com.initech.util.Preferences;
import com.initech.util.StringUtil;

import org.json.JSONObject;

public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, View.OnFocusChangeListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SIGN_UP = 9002;
    private TextInputLayout passwordLayout, emailLayout;
    private String email, password;


    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtil.hideStatusBar(getWindow());
        DataBindingUtil.setContentView(this, R.layout.activity_sign_in);
        passwordLayout = (TextInputLayout) findViewById(R.id.input_password_layout);
        emailLayout = (TextInputLayout) findViewById(R.id.input_email_layout);
        findViewById(R.id.sign_in_with_email_button).setOnClickListener(this);
        findViewById(R.id.forgot_password).setOnClickListener(this);

        // Assign fields
        findViewById(R.id.sign_in_with_google_textview).setOnClickListener(this);

        // Set click listeners
        findViewById(R.id.sign_up_button).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();

        String lastSignIn = Preferences.getInstance(this).getLastSignIn();
        MLog.i(TAG,"lastSignIn ",lastSignIn);
        if (lastSignIn != null) {
            emailLayout.getEditText().setText(lastSignIn);
        }
    }

    private void validateAccount() {
        DeviceUtil.hideKeyboard(this);

        email = emailLayout.getEditText().getText().toString().trim();
        emailLayout.getEditText().setOnFocusChangeListener(this);
        password = passwordLayout.getEditText().getText().toString().trim();
        passwordLayout.getEditText().setOnFocusChangeListener(this);

        if (email.contains("@") && !EmailUtil.isValidEmail(email)) {
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
        signInWithEmailOrUsernamePassword(email, password);
    }

    private void signInWithEmailOrUsernamePassword(final String email, final String password) {
        NetworkApi.getUserByEmailPassword(this, email, password, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    final String status = response.getString("status");
                    if (status.equalsIgnoreCase("OK")) {
                        final JSONObject data = response.getJSONObject("data");
                        final User user = new User();
                        user.copyFrom(data, null);
                        Preferences.getInstance(SignInActivity.this).saveUser(user);
                        Preferences.getInstance(SignInActivity.this).saveLastSignIn(email);
                        signIntoFirebase(user.getEmail(), password);
                    } else {
                        showErrorToast(R.string.email_password_not_found);
                    }
                } catch (final Exception e) {
                    MLog.e(TAG, e);
                    showErrorToast("1");
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                MLog.e(TAG, "signInWithEmailOrUsernamePassword() failed: " + error);
                showErrorToast("2");
            }
        });
    }

    private void signIntoFirebase(final String email, final String password) {
        mFirebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                MLog.d(TAG, "signIntoFirebase:onComplete:" + task.isSuccessful());
                if (!task.isSuccessful()) {
                    MLog.w(TAG, "signIntoFirebase", task.getException());
                    showErrorToast("Firebase Account Creation Error");
                } else {
                    startActivity(new Intent(SignInActivity.this, MainActivity.class));
                    finish();
                }
            }
        });
    }

    private void handleFirebaseAuthResult(AuthResult authResult) {
        if (authResult != null) {
            // Welcome the user
            FirebaseUser user = authResult.getUser();
            Toast.makeText(this, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();

            // Go back to the main activity
            startActivity(new Intent(this, MainActivity.class));
        }
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
                startActivity(new Intent(this,ForgotPasswordActivity.class));
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
     * If exists already, then just enter MainActivity.
     * If not exists, we must complete registration,
     * by asking user to enter password.
     *
     * @param acct
     */
    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
        NetworkApi.getUserByEmail(this, acct.getEmail(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    if (response.getString("status").equalsIgnoreCase("OK")) {
                        final User user = new User();
                        user.copyFrom(response.getJSONObject("data"), null);

                        /*
                         * Found user in our system.  Copy google photo
                         * to our system, update user with new url.
                         */
                        if (StringUtil.isEmpty(user.getProfilePicUrl())) {
                            if (acct.getPhotoUrl() != null) {
                                user.setProfilePicUrl(acct.getPhotoUrl().toString());
                                NetworkApi.uploadMyPhotoToS3(user);
                            }
                        }
                        Preferences.getInstance(SignInActivity.this).saveUser(user);
                        Preferences.getInstance(SignInActivity.this).saveLastSignIn(user.getUsername());
                        signIntoFirebase(user.getEmail(), user.getPassword());
                    } else { //user does not exist
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

    @Override
    protected void onDestroy() {
        MyApp.getInstance().getRequestQueue().cancelAll(this);
        super.onDestroy();
    }

    private String errorMessage(final int stringResId, final String str) {
        return getString(stringResId, str);
    }

    private void showErrorToast(final String distinctScreenCode) {
        Toast.makeText(this, getString(R.string.general_api_error, distinctScreenCode), Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(final int stringResId) {
        Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show();
    }

}
