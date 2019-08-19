package com.instachat.android.app.blocks;

import android.app.Activity;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.util.UserPreferences;

import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/14/2016.
 */

public class ReportUserDialogHelper {

    public void showReportUserQuestionDialog(final Activity activity,
                                             final int userid,
                                             @NonNull final String username,
                                             final String dpid) {

        if (UserPreferences.getInstance().getUserId() == userid) {
            return;
        }

        new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(activity.getString(R.string.report_person_title) + " " + username + "?")
                .setContentText(activity.getString(R.string.report_person_question))
                .setCancelText(activity.getString(android.R.string.no))
                .setConfirmText(activity.getString(android.R.string.yes))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.cancel();
                    }
                }).setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {

                sweetAlertDialog.dismiss();
                final Map<String, Object> reportedMap = new HashMap<>(2);
                reportedMap.put("name", username);
                if (!TextUtils.isEmpty(dpid))
                    reportedMap.put("dpid", dpid);
                FirebaseDatabase.getInstance().getReference(Constants.REPORTS_REF(userid)).
                        updateChildren(reportedMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            new SweetAlertDialog(activity, SweetAlertDialog.SUCCESS_TYPE)
                                    .setTitleText(activity.getString(R.string.success_exclamation))
                                    .setContentText(activity.getString(R.string.report_person_success) + " " + username)
                                    .show();
                            Map<String, Object> reporterMap = new HashMap<>(2);
                            reporterMap.put("name", UserPreferences.getInstance().getUsername());
                            String dpid = UserPreferences.getInstance().getUser().getProfilePicUrl();
                            if (!TextUtils.isEmpty(dpid))
                                reporterMap.put("dpid", dpid);
                            FirebaseDatabase.getInstance().getReference(Constants.REPORTS_REF(userid))
                                    .child(UserPreferences.getInstance().getUserId() + "")
                                    .updateChildren(reporterMap);
                        } else {
                            new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText(activity.getString(R.string.oops_exclamation))
                                    .setContentText(activity.getString(R.string.report_person_failed))
                                    .show();
                        }
                    }
                });
            }
        }).show();
    }
}
