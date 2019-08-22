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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.BR;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.TheApp;
import com.instachat.android.app.activity.group.GroupChatActivity;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.ui.base.BaseActivity;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.databinding.ActivityVerifyPhoneBinding;
import com.instachat.android.util.MLog;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class VerifyPhoneActivity extends BaseActivity<ActivityVerifyPhoneBinding, VerifyPhoneViewModel> implements VerifyPhoneNavigator {

    private static final String TAG = "VerifyPhoneActivity";

    private String phone;
    private String verificationId;
    private ProgressDialog progressDialog;
    private VerifyPhoneViewModel verifyPhoneViewModel;
    private boolean isInProgress; //code has been sent

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
        verifyPhoneViewModel.setNavigator(this);
        if (savedInstanceState != null && savedInstanceState.containsKey("phone")) {
            phone = savedInstanceState.getString("phone");
            verificationId = savedInstanceState.getString("verificationId");
            isInProgress = savedInstanceState.getBoolean("isInProgress");
            try {
                ((TextInputEditText) findViewById(R.id.input_phone)).setText(phone);
            }catch (Exception e){}
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        if (phone != null)
            outState.putString("phone", phone);
        if (verificationId != null)
            outState.putString("verificationId", verificationId);
        if (isInProgress)
            outState.putBoolean("isInProgress", true);
        super.onSaveInstanceState(outState);
    }

    /**
     * Check if the given phone number is valid for the given user to use for verification.
     * If the phone number belongs to this user already or the user does not have a phone
     * number yet for this account AND this number isn't being used by another account
     * then it's OK to be used.
     *
     * @param userid
     * @param phone
     */
    private void checkPhoneNumber(final int userid, final String phone) {
        showProgressDialog();
        networkApi.isValidPhone(this, userid, phone, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    MLog.i(TAG, "isValidPhone() : " + response);
                    //status:OK
                    //exists:true|false
                    try {
                        if (!response.getString(NetworkApi.KEY_RESPONSE_STATUS).equalsIgnoreCase(NetworkApi.RESPONSE_OK)) {
                            showErrorToast("1");
                        } else if (response.getJSONObject(NetworkApi.RESPONSE_DATA).getBoolean(NetworkApi.KEY_EXISTS)) {
                            hideProgressDialog();
                            verifyPhoneNumber(phone);
                        } else {
                            new SweetAlertDialog(VerifyPhoneActivity.this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.phone_already_exists)).show();
                        }
                    } catch (final Exception e) {
                        showErrorToast("2");
                    }
                } catch (final Exception e) {
                    MLog.e(TAG, e);
                    showErrorToast("1");
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                MLog.e(TAG, "checkPhoneNumber() failed: " + error);
                showErrorToast("network 1");
            }
        });
    }

    private void verifyPhoneNumber(String phoneNum) {

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNum, 120L /*timeout*/, TimeUnit.SECONDS,
                this, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onCodeSent(String verificationId,
                                           PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        isInProgress = true;
                        VerifyPhoneActivity.this.verificationId = verificationId;
                        new SweetAlertDialog(VerifyPhoneActivity.this, SweetAlertDialog.NORMAL_TYPE).setContentText(getString(R.string.sms_code_sent)).show();
                    }

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        MLog.i(TAG,"PhoneAuthProvider onVerificationCompleted");
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        isInProgress = false;
                        new SweetAlertDialog(VerifyPhoneActivity.this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.sms_error) + " " + e).show();
                    }

                });
    }

    public void onSendSmsCode(View view) {
        phone = ((TextInputEditText)findViewById(R.id.input_phone)).toString();
        phone = StringUtil.onlyPhone(phone);
        if (StringUtil.isValidPhone(phone)) {
            checkPhoneNumber(UserPreferences.getInstance().getUserId(), phone);
        } else {
            new SweetAlertDialog(VerifyPhoneActivity.this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.fix_phone)).show();
        }
    }

    public void onVerifySmsCode(View view) {

        if (!isInProgress) {
            return;
        }

        String smsCode = findViewById(R.id.input_sms_code).toString().trim();
        if (StringUtil.isEmpty(smsCode)) {
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, smsCode);
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    isInProgress = false;
                    markFirebase(phone);
                    UserPreferences.getInstance().getUser().setPhone(phone);
                    networkApi.saveUser(VerifyPhoneActivity.this, UserPreferences.getInstance().getUser(), new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            MLog.i(TAG,"saveUser response: " + response);
                            startActivity(new Intent(VerifyPhoneActivity.this, GroupChatActivity.class));
                            FirebaseAnalytics.getInstance(VerifyPhoneActivity.this).logEvent(Events.PHONE_VERIFY_SUCCESS, null);
                            finish();
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            showErrorToast("");
                        }
                    });
                } else {
                    new SweetAlertDialog(VerifyPhoneActivity.this, SweetAlertDialog.ERROR_TYPE).setContentText(getString(R.string.invalid_sms_code)).show();
                }
            }
        });
    }

    private void markFirebase(String phoneNum) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("ph", phoneNum);
        FirebaseDatabase.getInstance()
                .getReference(Constants.USER_INFO_REF(UserPreferences.getInstance().getUserId()))
                .updateChildren(map);
    }

    private void signout() {
        TheApp.isSavedDeviceId = false;
        firebaseAuth.signOut();
        UserPreferences.getInstance().clearUser();
    }

    @Override
    public void onBackPressed() {
        signout();
        super.onBackPressed();
    }

    @Override
    public VerifyPhoneViewModel getViewModel() {
        return (verifyPhoneViewModel = ViewModelProviders.of(this, viewModelFactory).get(VerifyPhoneViewModel.class));
    }

    @Override
    public int getBindingVariable() {
        return BR.viewModel;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_verify_phone;
    }

    private void showProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.checking_number);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showErrorToast(final String distinctScreenCode) {
        try {
            Toast.makeText(this, getString(R.string.general_api_error) + " " + distinctScreenCode, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }
}
