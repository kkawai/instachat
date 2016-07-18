package com.google.firebase.codelab.friendlychat;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.initech.util.EmailUtil;

/**
 * Created by kevin on 7/18/2016.
 */
public class SignUpActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        findViewById(R.id.create_account_button).setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.create_account_button:
                createAccount();
                break;
            default:
                return;
        }
    }

    @Override
    public void onFocusChange(final View v, final boolean hasFocus) {
        if (v.getId() == R.id.input_email && hasFocus) {
            final TextInputLayout emailLayout = (TextInputLayout) findViewById(R.id.input_email_layout);
            emailLayout.setError("");
        }
        if (v.getId() == R.id.input_password && hasFocus) {
            final TextInputLayout passwordLayout = (TextInputLayout) findViewById(R.id.input_password_layout);
            passwordLayout.setError("");
        }
    }

    private void createAccount() {
        hideKeyboard();

        final TextInputLayout emailLayout = (TextInputLayout) findViewById(R.id.input_email_layout);
        final String email = emailLayout.getEditText().getText().toString();
        emailLayout.getEditText().setOnFocusChangeListener(this);
        final TextInputLayout passwordLayout = (TextInputLayout) findViewById(R.id.input_password_layout);
        final String password = passwordLayout.getEditText().getText().toString();
        passwordLayout.getEditText().setOnFocusChangeListener(this);
        if (!EmailUtil.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.invalid_email));
        } else if (!EmailUtil.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.invalid_password));
        } else {
            emailLayout.setError("");
            passwordLayout.setError("");
            doCreateAccount();
        }
    }

    private void doCreateAccount() {
        Toast.makeText(this, "Create account now!", Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        final View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
