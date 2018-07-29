package com.instachat.android.app.blocks;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/14/2016.
 */

public class BlockUserDialogHelper {

    private final FirebaseDatabase firebaseDatabase;

    public BlockUserDialogHelper(FirebaseDatabase firebaseDatabase) {
        this.firebaseDatabase = firebaseDatabase;
    }

    private boolean canBlock(Activity activity, int userid) {
        return true; //fix this on the firebase console
    }

    public void showBlockUserQuestionDialog(final Activity activity,
                                            final int userid,
                                            @NonNull final String username,
                                            final String dpid,
                                            @NonNull final BlockedUserListener listener) {

        if (!canBlock(activity, userid)) {
            return;
        }

        if (UserPreferences.getInstance().getUserId() == userid) {
            return;
        }

        new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(activity.getString(R.string.block_person_title, username))
                .setContentText(activity.getString(R.string.block_person_question, username))
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

                if (!FirebaseAuth.getInstance().getCurrentUser().getEmail().equals(UserPreferences.getInstance().getEmail())) {
                    return;
                }

                Map<String, Object> map = new HashMap<>(2);
                map.put("name", username);
                map.put("z","z");
                if (!TextUtils.isEmpty(dpid))
                    map.put("dpid", dpid);
                firebaseDatabase.getReference(Constants.MY_BLOCKS_REF()).
                        child(userid + "").updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(activity, activity.getString(R.string.block_person_success, username), Toast.LENGTH_SHORT).show();
                            listener.onUserBlocked(userid);
                            firebaseDatabase.getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF())
                                    .child(userid + "")
                                    .removeValue();
                        } else {
                            new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText(activity.getString(R.string.oops_exclamation))
                                    .setContentText(activity.getString(R.string.block_person_failed, username))
                                    .show();
                        }
                    }
                });
            }
        }).show();
    }
}
