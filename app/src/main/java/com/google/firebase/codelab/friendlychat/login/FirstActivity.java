package com.google.firebase.codelab.friendlychat.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.firebase.codelab.friendlychat.MainActivity;
import com.google.firebase.codelab.friendlychat.R;
import com.initech.util.ActivityUtil;
import com.initech.util.Preferences;

/**
 * Created by kevin on 8/9/2016.
 */
public final class FirstActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Preferences.getInstance(this).getUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        ActivityUtil.hideStatusBar(getWindow());

        setContentView(R.layout.activity_first);
        findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FirstActivity.this, SignInActivity.class));
                finish();
            }
        });
        findViewById(R.id.sign_up_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FirstActivity.this, SignUpActivity.class));
                finish();
            }
        });
    }

}
