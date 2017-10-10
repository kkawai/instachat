package com.instachat.android.app.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.instachat.android.app.BaseActivity;
import com.instachat.android.app.activity.GroupChatActivity;
import com.instachat.android.R;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.Preferences;

/**
 * Created by kevin on 8/9/2016.
 */
public final class LauncherActivity extends BaseActivity {

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setTheme(R.style.Theme_App);
      super.onCreate(savedInstanceState);
      if (Preferences.getInstance().getUser() != null) {
         startActivity(new Intent(this, GroupChatActivity.class));
         finish();
         return;
      }

      ActivityUtil.hideStatusBar(getWindow());

      setContentView(R.layout.activity_launcher);
      findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            startActivity(new Intent(LauncherActivity.this, SignInActivity.class));
            finish();
         }
      });
      findViewById(R.id.sign_up_button).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            startActivity(new Intent(LauncherActivity.this, ClaimRoomActivity.class));
            finish();
         }
      });
   }

}
