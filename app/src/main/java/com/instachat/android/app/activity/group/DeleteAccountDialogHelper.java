package com.instachat.android.app.activity.group;

import android.app.Activity;

import com.instachat.android.R;

import androidx.annotation.NonNull;
import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 12/26/2019
 */

public class DeleteAccountDialogHelper {

    public DeleteAccountDialogHelper() {
    }

    public interface DeleteAccountListener {
        void onConfirmDeleteAccount();
    }

    public void showDeleteAccountDialog(final Activity activity, @NonNull final DeleteAccountListener listener) {

        new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(activity.getString(R.string.delete_account))
                .setContentText(activity.getString(R.string.delete_account_are_you_sure))
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
                listener.onConfirmDeleteAccount();
            }
        }).show();
    }
}
