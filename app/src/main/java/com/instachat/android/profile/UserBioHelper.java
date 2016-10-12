package com.instachat.android.profile;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.instachat.android.R;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.font.FontUtil;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;

/**
 * Created by kevin on 10/6/2016.
 */

public class UserBioHelper {

    public void showBioInputDialog(final Activity activity) {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View promptView = layoutInflater.inflate(R.layout.dialog_input_bio, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setView(promptView);

        final User user = Preferences.getInstance().getUser();
        final TextInputEditText editText = (TextInputEditText) promptView.findViewById(R.id.input_bio);
        FontUtil.setTextViewFont(editText);
        editText.setText(user.getBio());
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        user.setBio(editText.getText().toString());
                        NetworkApi.saveUser(activity, user, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                MLog.d("UserBioHelper","response: ",response);
                                Preferences.getInstance().saveUser(user);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                MLog.d("UserBioHelper","error response: ",error);
                            }
                        });
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }


}
