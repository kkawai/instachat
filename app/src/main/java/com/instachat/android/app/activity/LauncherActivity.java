package com.instachat.android.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.instachat.android.R;
import com.instachat.android.app.activity.group.GroupChatActivity;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.app.login.signup.SignUpActivity;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.UserPreferences;

/**
 * Created by kevin on 8/9/2016.
 */
public final class LauncherActivity extends AppCompatActivity {

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setTheme(R.style.Theme_App);
      super.onCreate(savedInstanceState);
      if (UserPreferences.getInstance().getUser() != null) {
         startActivity(new Intent(this, GroupChatActivity.class));
         finish();
         return;
      }

      ActivityUtil.hideStatusBar(getWindow());

      setContentView(R.layout.activity_launcher);
      findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            startActivityForResult(new Intent(LauncherActivity.this, SignInActivity.class), 10);
         }
      });
      findViewById(R.id.sign_up_button).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            startActivityForResult(new Intent(LauncherActivity.this, SignUpActivity.class), 10);
         }
      });
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
         finish();
      }
   }
}
