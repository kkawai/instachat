package com.instachat.android;

import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;

/**
 * Created by kevin on 10/9/2016.
 */

public class ExternalSendIntentConsumer {

    private ExternalSendIntentListener listener;

    public interface ExternalSendIntentListener {
        void onHandleSendImage(Uri imageUri);
        void onHandleSendText(String text);
    }

    public void setListener(ExternalSendIntentListener listener) {
        this.listener = listener;
    }

    public void clear() {
        listener = null;
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
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null && listener != null) {
            listener.onHandleSendText(sharedText);
        }
    }

    private void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null && listener != null) {
            listener.onHandleSendImage(imageUri);
        }
    }

    //not supported yet
    private void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
        }
    }
}
