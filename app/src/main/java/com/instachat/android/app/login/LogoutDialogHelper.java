package com.instachat.android.app.login;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.instachat.android.R;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/14/2016.
 */

public class LogoutDialogHelper {

    public interface LogoutListener {
        void onConfirmLogout();
    }

    public void showLogoutDialog(final Activity activity, @NonNull final LogoutListener listener) {

        new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(activity.getString(R.string.logout_title))
                .setContentText(activity.getString(R.string.logout_question))
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
                listener.onConfirmLogout();
            }
        }).show();
    }
}
