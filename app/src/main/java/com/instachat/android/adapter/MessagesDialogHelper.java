package com.instachat.android.adapter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.instachat.android.R;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.util.MLog;

import org.jetbrains.annotations.NotNull;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/19/2016.
 */

public class MessagesDialogHelper {
    private static final String TAG = "MessagesDialogHelper";

    public void showDeleteMessageDialog(@NonNull final Context context,
                                        @NotNull final FriendlyMessage friendlyMessage,
                                        @NonNull final StorageReference storageRef,
                                        @NonNull final String dbRef) {
        new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(context.getString(R.string.message_delete_title))
                .setContentText(context.getString(R.string.message_delete_question))
                .setCancelText(context.getString(android.R.string.no))
                .setConfirmText(context.getString(android.R.string.yes))
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
                if (friendlyMessage.getImageUrl() != null && friendlyMessage.getImageId() != null) {
                    final StorageReference photoRef = storageRef.child(dbRef).child(friendlyMessage.getImageId());
                    photoRef.delete();
                    MLog.d(TAG, "deleted photo " + friendlyMessage.getImageId());
                }

                FirebaseDatabase.getInstance().getReference(dbRef + "/" + friendlyMessage.getId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                                    .setTitleText(context.getString(R.string.success_exclamation))
                                    .setContentText(context.getString(R.string.message_delete_success))
                                    .show();
                        } else {
                            new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText(context.getString(R.string.oops_exclamation))
                                    .setContentText(context.getString(R.string.message_delete_failed))
                                    .show();
                        }
                    }
                });
            }
        }).show();
    }
}
