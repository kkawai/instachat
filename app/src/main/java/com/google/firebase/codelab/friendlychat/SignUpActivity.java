package com.google.firebase.codelab.friendlychat;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.initech.api.NetworkApi;
import com.initech.model.User;
import com.initech.util.EmailUtil;
import com.initech.util.MLog;

import org.json.JSONObject;

/**
 * Created by kevin on 7/18/2016.
 */
public class SignUpActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout usernameLayout;
    private String email, username, password;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        findViewById(R.id.create_account_button).setOnClickListener(this);
        emailLayout = (TextInputLayout) findViewById(R.id.input_email_layout);
        passwordLayout = (TextInputLayout) findViewById(R.id.input_password_layout);
        usernameLayout = (TextInputLayout) findViewById(R.id.input_username_layout);
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
            emailLayout.setError("");
        }
        if (v.getId() == R.id.input_password && hasFocus) {
            passwordLayout.setError("");
        }
        if (v.getId() == R.id.input_username && hasFocus) {
            usernameLayout.setError("");
        }
    }

    private void createAccount() {
        hideKeyboard();

        email = emailLayout.getEditText().getText().toString().trim();
        emailLayout.getEditText().setOnFocusChangeListener(this);

        password = passwordLayout.getEditText().getText().toString().trim();
        passwordLayout.getEditText().setOnFocusChangeListener(this);

        username = usernameLayout.getEditText().getText().toString().trim();
        usernameLayout.getEditText().setOnFocusChangeListener(this);

        if (!EmailUtil.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.invalid_email));
        } else if (!isValidUsername(username)) {
            usernameLayout.setError(getString(R.string.invalid_username));
        } else if (!EmailUtil.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.invalid_password));
        } else {
            emailLayout.setError("");
            usernameLayout.setError("");
            passwordLayout.setError("");
            doCreateAccount();
        }
    }

    private boolean isValidUsername(final String username) {
        if (username == null || username.trim().length() <= 1 || username.trim().length() > 50) {
            return false;
        }
        return true;
    }

    private void remotelyValidateEmail() {
        NetworkApi.isExistsEmail(this, email,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        //status:OK
                        //exists:true|false
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {

                    }
                });
    }

    private void remotelyValidateUsername() {
        NetworkApi.isExistsUsername(this, username,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        //status:OK
                        //exists:true|false
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {

                    }
                });
    }

    private void doCreateAccount() {
        final User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setUsername(username);
        user.setInstagramId(email);
        NetworkApi.saveUser(this, user, new Response.Listener<String>() {
            @Override
            public void onResponse(final String string) {
                try {
                    final JSONObject response = new JSONObject(string);
                    MLog.i("test", "savedUser: " + string);
                    if (response.getString("status").equals("OK")) {
                        user.copyFrom(response.getJSONObject("data"), null);
                        Toast.makeText(SignUpActivity.this, "Account created!  USER id: " + user.getId(), Toast.LENGTH_SHORT).show();
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

    private void hideKeyboard() {
        final View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
