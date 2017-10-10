package com.instachat.android.app.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.instachat.android.R;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/9/2016.
 */

public class ExternalSendIntentConsumer {

    public ExternalSendIntentConsumer(Context context) {
        this.context = context;
    }

    private ExternalSendIntentListener listener;
    private Context context;

    public interface ExternalSendIntentListener {
        void onHandleSendImage(Uri imageUri);

        void onHandleSendText(String text);
    }

    public void setListener(ExternalSendIntentListener listener) {
        this.listener = listener;
    }

    public void cleanup() {
        listener = null;
        context = null;
    }

    public void consumeIntent(Intent intent) {

        if (intent == null || intent.getAction() == null || intent.getType() == null)
            return;

        // Get intent, action and MIME type
        //Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // Handle multiple images being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }
    }

    private void handleSendText(Intent intent) {
        final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(sharedText) || listener == null) {
            return;
        }
        intent.removeExtra(Intent.EXTRA_TEXT);
        new SweetAlertDialog(context, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText(context.getString(R.string.share_message))
                .setContentText(context.getString(R.string.please_choose_person_or_group))
                .setCancelText(context.getString(android.R.string.cancel))
                .setConfirmText(context.getString(android.R.string.ok))
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
                listener.onHandleSendText(sharedText);
            }
        }).show();
    }

    private void handleSendImage(Intent intent) {
        final Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri == null || listener == null)
            return;
        intent.removeExtra(Intent.EXTRA_STREAM);
        new SweetAlertDialog(context, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText(context.getString(R.string.share_photo))
                .setContentText(context.getString(R.string.please_choose_person_or_group))
                .setCancelText(context.getString(android.R.string.cancel))
                .setConfirmText(context.getString(android.R.string.ok))
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
                listener.onHandleSendImage(imageUri);
            }
        }).show();
    }

    //not supported yet
    private void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
        }
    }
}
