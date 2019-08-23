package com.instachat.android.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

/**
 * Created by kevin on 8/9/2016.
 */
public final class LauncherActivity extends AppCompatActivity {

   final private int REQ_CODE = 10;
   private boolean userAlreadyClicked;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setTheme(R.style.Theme_App);
      super.onCreate(savedInstanceState);

      SimpleRxWrapper.executeInWorkerThread(new Runnable() {
         @Override
         public void run() {
            try {
               FirebaseApp.initializeApp(LauncherActivity.this);
            }catch (Throwable t){
               MLog.e("LauncherActivity","",t);
            }
            try {
               FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
               if (UserPreferences.getInstance().isLoggedIn() && firebaseUser != null && firebaseUser.isEmailVerified()) {
                  checkPhoneNumber(); //start new activity before this screen can render
               }
            }catch (Throwable t) {
               MLog.e("LauncherActivity","",t);
            }
         }
      });

      ActivityUtil.hideStatusBar(getWindow());

      setContentView(R.layout.activity_launcher);
      findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            userAlreadyClicked = true;
            startActivityForResult(new Intent(LauncherActivity.this, SignInActivity.class), REQ_CODE);
         }
      });
      findViewById(R.id.sign_up_button).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            userAlreadyClicked = true;
            startActivityForResult(new Intent(LauncherActivity.this, SignUpActivity.class), REQ_CODE);
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

   /**
    * Called if user is already signed in.  However, there is
    * a final step before user can chat:  Verify phone number!
    */
   private void checkPhoneNumber() {

      if (userAlreadyClicked) {
         return;
      }

      final DatabaseReference phoneNumberRef = FirebaseDatabase.getInstance()
              .getReference(Constants.USER_INFO_REF(UserPreferences.getInstance().getUserId()) + "/" + Constants.PHONE_REF);
      phoneNumberRef.addListenerForSingleValueEvent(new ValueEventListener() {
         @Override
         public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            phoneNumberRef.removeEventListener(this);
            if (dataSnapshot.exists()) {
               startActivity(new Intent(LauncherActivity.this, GroupChatActivity.class));
               finish();
            } else {
               goToVerifyPhoneActivity();
            }
         }

         @Override
         public void onCancelled(@NonNull DatabaseError databaseError) {
            phoneNumberRef.removeEventListener(this);
         }
      });
   }

   private void goToVerifyPhoneActivity() {
      startActivity(new Intent(this, VerifyPhoneActivity.class));
      FirebaseAnalytics.getInstance(this).logEvent(Events.GO_VERIFY_PHONE, null);
      finish();
   }
}
