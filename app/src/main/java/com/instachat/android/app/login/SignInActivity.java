package com.instachat.android.app.login;

import android.app.ProgressDialog;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.group.GroupChatActivity;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.login.recovery.ForgotPasswordActivity;
import com.instachat.android.app.ui.base.BaseActivity;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.model.User;
import com.instachat.android.databinding.ActivitySignInBinding;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.FontUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;

import org.json.JSONObject;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class SignInActivity extends BaseActivity<ActivitySignInBinding, SignInViewModel> implements View.OnClickListener, View.OnFocusChangeListener, SignInNavigator {

    private static final String TAG = "SignInActivity";
    private TextInputLayout passwordLayout, emailLayout;
    private String email, password;
    private ProgressDialog progressDialog;
    private SignInViewModel signInViewModel;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    NetworkApi networkApi;

    @Inject
    RequestQueue requestQueue;

    @Inject
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signInViewModel.setNavigator(this);
        ActivityUtil.hideStatusBar(getWindow());
        passwordLayout = (TextInputLayout) findViewById(R.id.input_password_layout);
        emailLayout = (TextInputLayout) findViewById(R.id.input_email_layout);
        findViewById(R.id.sign_in_username_email).setOnClickListener(this);
        findViewById(R.id.forgot_password).setOnClickListener(this);
        FontUtil.setTextViewFont(passwordLayout);
        FontUtil.setTextViewFont(emailLayout);

        String lastSignIn = UserPreferences.getInstance().getLastSignIn();
        MLog.i(TAG, "lastSignIn: " + lastSignIn);
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
        signInWithEmailOrUsernamePassword(email, password);
    }

    private void signInWithEmailOrUsernamePassword(final String emailOrUsername, final String password) {
        showProgressDialog();
        networkApi.getUserByEmail(this, emailOrUsername, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    MLog.e(TAG, "getUserByEmail() : " + response);
                    final String status = response.getString(NetworkApi.KEY_RESPONSE_STATUS);
                    if (status.equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                        final User user = User.fromResponse(response);
                        signIntoFirebase(user, password);
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

    private void signIntoFirebase(final User user, final String password) {

        firebaseAuth.signInWithEmailAndPassword(user.getEmail(), password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                MLog.d(TAG, "firebaseAuth.signInWithEmailAndPassword(): " + task.isSuccessful(), " ", email, " ", password);
                if (!task.isSuccessful()) {
                    MLog.w(TAG, "firebaseAuth.signInWithEmailAndPassword(): ", task.getException());
                    //showErrorToast("VERIFY EMAIL");
                    showErrorToast(R.string.email_password_not_found);
                } else {
                    hideProgressDialog();
                    if (firebaseAuth.getCurrentUser().isEmailVerified()) {
                        UserPreferences.getInstance().saveUser(user);
                        UserPreferences.getInstance().saveLastSignIn(user.getUsername());
                        checkPhoneNumber();
                    } else {
                        firebaseAuth.getCurrentUser().sendEmailVerification();
                        new SweetAlertDialog(SignInActivity.this, SweetAlertDialog.NORMAL_TYPE).setContentText(getString(R.string.email_verification_sent) + " " + user.getEmail()).show();
                    }
                }
            }
        });
    }

    /**
     * Called after user has successfully signed in.  However, there is
     * a final step before user can chat:  Verify phone number!
     */
    private void checkPhoneNumber() {
        final DatabaseReference phoneNumberRef = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(UserPreferences.getInstance().getUserId()) + "/ph");
        phoneNumberRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                phoneNumberRef.removeEventListener(this);
                if (dataSnapshot.exists()) {
                    phoneNumberRef.removeEventListener(this);
                    finallyGoChat();
                } else {
                    goToVerifyPhoneActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                phoneNumberRef.removeEventListener(this);
                showErrorToast("unknown error");
            }
        });
    }

    private void goToVerifyPhoneActivity() {
        hideProgressDialog();
        startActivity(new Intent(this, VerifyPhoneActivity.class));
        FirebaseAnalytics.getInstance(this).logEvent(Events.GO_VERIFY_PHONE, null);
        setResult(RESULT_OK);
        finish();
    }

    private void finallyGoChat() {
        hideProgressDialog();
        startActivity(new Intent(this, GroupChatActivity.class));
        FirebaseAnalytics.getInstance(this).logEvent(Events.LOGIN_SUCCESS, null);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.sign_in_username_email:
                validateAccount();
                break;
            case R.id.forgot_password:
                startActivity(new Intent(this, ForgotPasswordActivity.class));
                break;
            default:
                return;
        }
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

    private void showErrorToast(final String distinctScreenCode) {
        hideProgressDialog();
        try {
            Toast.makeText(this, getString(R.string.general_api_error) + " " + distinctScreenCode, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    private void showErrorToast(final int stringResId) {
        hideProgressDialog();
        Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show();
    }

    private void showProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.loading);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public SignInViewModel getViewModel() {
        return (signInViewModel = ViewModelProviders.of(this, viewModelFactory).get(SignInViewModel.class));
    }

    @Override
    public int getBindingVariable() {
        return BR.viewModel;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_sign_in;
    }
}
