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
package com.instachat.android.login;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.instachat.android.BaseActivity;
import com.instachat.android.Events;
import com.instachat.android.R;
import com.instachat.android.font.FontUtil;
import com.instachat.android.util.ActivityUtil;
import com.instachat.android.util.ScreenUtil;
import com.instachat.android.util.StringUtil;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ClaimRoomActivity extends BaseActivity implements View.OnClickListener {

   private static final String TAG = "ClaimRoomActivity";
   private TextInputLayout roomNameLayout;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      ActivityUtil.hideStatusBar(getWindow());
      DataBindingUtil.setContentView(this, R.layout.activity_claim_room);
      roomNameLayout = (TextInputLayout) findViewById(R.id.input_room_name_layout);
      findViewById(R.id.next).setOnClickListener(this);
      findViewById(R.id.skip).setOnClickListener(this);
      ((TextView) findViewById(R.id.everybody_gets_own_room)).setText(getString(R.string.every_gets_own_room, getString(R.string.app_name)));
      FontUtil.setTextViewFont(roomNameLayout);
   }

   private void validateRoomName() {
      ScreenUtil.hideKeyboard(this);
      String roomName = roomNameLayout.getEditText().getText().toString().trim();

      if (!StringUtil.isValidUsername(roomName)) {
         roomNameLayout.setError(getString(R.string.invalid_roomname));
         return;
      }

      roomNameLayout.setError("");
      showSuccessDialog(roomName);
   }

   @Override
   public void onClick(final View v) {
      switch (v.getId()) {
         case R.id.next:
            validateRoomName();
            break;
         case R.id.skip:
            FirebaseAnalytics.getInstance(this).logEvent(Events.ROOM_NAME_SKIPPED, null);
            startActivity(new Intent(ClaimRoomActivity.this, SignUpActivity.class));
            finish();
            break;
         default:
            return;
      }
   }

   private void showSuccessDialog(final String roomName) {
      SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE).
            setTitleText(getString(R.string.success_exclamation)).
            setContentText(getString(R.string.nice_room_name)).
            setConfirmText(getString(android.R.string.ok)).
            setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
               @Override
               public void onClick(SweetAlertDialog sweetAlertDialog) {
                  sweetAlertDialog.dismiss();
                  Bundle bundle = new Bundle();
                  bundle.putString("room_name", roomName);
                  FirebaseAnalytics.getInstance(ClaimRoomActivity.this).logEvent(Events.ROOM_NAME_CHOSEN, bundle);
                  startActivity(new Intent(ClaimRoomActivity.this, SignUpActivity.class));
                  finish();
               }
            });
      dialog.setCancelable(false);
      dialog.show();
   }

}
