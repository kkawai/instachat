package com.instachat.android.app.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.group.GroupChatActivity;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.app.login.VerifyPhoneActivity;
import com.instachat.android.app.login.signup.SignUpActivity;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.SimpleRxWrapper;
import com.instachat.android.util.UserPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by kevin on 8/9/2016.
 */
public final class LauncherActivity extends AppCompatActivity {

   final private int REQ_CODE = 10;
   private ProgressDialog progressDialog;
   private View loginButton, signupButton;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setTheme(R.style.Theme_App);
      super.onCreate(savedInstanceState);

      progressDialog = ProgressDialog.show(this, "", getString(R.string.loading), true);
      SimpleRxWrapper.executeInWorkerThread(new Runnable() {
         @Override
         public void run() {
            checkIfLoggedIn();
         }
      });

      ActivityUtil.hideStatusBar(getWindow());

      setContentView(R.layout.activity_launcher);
      loginButton = findViewById(R.id.login_button);
      signupButton = findViewById(R.id.sign_up_button);
      loginButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            startActivityForResult(new Intent(LauncherActivity.this, SignInActivity.class), REQ_CODE);
         }
      });
      signupButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            startActivityForResult(new Intent(LauncherActivity.this, SignUpActivity.class), REQ_CODE);
         }
      });
   }

   private void checkIfLoggedIn() {
      try {
         FirebaseApp.initializeApp(LauncherActivity.this);
      }catch (Throwable t){
         MLog.e("LauncherActivity","",t);
      }
      try {
         FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
         if (UserPreferences.getInstance().isLoggedIn() && firebaseUser != null && firebaseUser.isEmailVerified()) {

            final DatabaseReference phoneNumberRef = FirebaseDatabase.getInstance()
                    .getReference(Constants.USER_INFO_REF(UserPreferences.getInstance().getUserId()) + "/" + Constants.PHONE_REF);
            phoneNumberRef.addListenerForSingleValueEvent(new ValueEventListener() {
               @Override
               public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                  disableButtons();
                  phoneNumberRef.removeEventListener(this);
                  if (dataSnapshot.exists()) {
                     startActivity(new Intent(LauncherActivity.this, GroupChatActivity.class));
                     finish();
                  } else {
                     goToVerifyPhoneActivity();
                  }
                  dismissProgressDialog();
               }

               @Override
               public void onCancelled(@NonNull DatabaseError databaseError) {
                  dismissProgressDialog();
                  phoneNumberRef.removeEventListener(this);
               }
            });
         } else {
            dismissProgressDialog();
         }
      }catch (Throwable t) {
         MLog.e("LauncherActivity","",t);
         dismissProgressDialog();
      }
   }

   private void disableButtons() {
      signupButton.setEnabled(false);
      loginButton.setEnabled(false);
   }

   private void dismissProgressDialog() {
      SimpleRxWrapper.executeInUiThread(new Runnable() {
         @Override
         public void run() {
            if (progressDialog != null) {
               progressDialog.dismiss();
            }
         }
      });
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == REQ_CODE && resultCode == Activity.RESULT_OK) {
         finish();
      }
   }

   private void goToVerifyPhoneActivity() {
      startActivity(new Intent(this, VerifyPhoneActivity.class));
      FirebaseAnalytics.getInstance(this).logEvent(Events.GO_VERIFY_PHONE, null);
      finish();
   }
}
