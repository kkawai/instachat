package com.instachat.android.app.adapter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.instachat.android.R;
import com.instachat.android.app.activity.AbstractChatViewModel;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.util.MLog;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/19/2016.
 */

public class MessagesDialogHelper {
    private static final String TAG = "MessagesDialogHelper";

    public void showDeleteMessageDialog(@NonNull final Context context,
                                        @NonNull final FriendlyMessage friendlyMessage,
                                        @NonNull final AbstractChatViewModel abstractChatViewModel) {
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
                abstractChatViewModel
                        .removeMessage(friendlyMessage)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    try {
                                        new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                                                .setTitleText(context.getString(R.string.success_exclamation))
                                                .setContentText(context.getString(R.string.message_delete_success))
                                                .show();
                                        MLog.d(TAG,"sort_tag remove single message check sort order");
                                        abstractChatViewModel.checkMessageSortOrder();
                                    } catch (Exception e) {
                                        MLog.e(TAG, "", e);
                                    }

                                } else {
                                    try {
                                        new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                                                .setTitleText(context.getString(R.string.oops_exclamation))
                                                .setContentText(context.getString(R.string.message_delete_failed))
                                                .show();
                                    } catch (Exception e) {
                                        MLog.e(TAG, "", e);
                                    }
                                }
                            }
                        });

            }
        }).show();
    }
}
