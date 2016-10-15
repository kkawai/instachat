package com.instachat.android.options;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import com.instachat.android.R;
import com.instachat.android.font.FontUtil;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.view.ThemedAlertDialog;

import org.jetbrains.annotations.NotNull;

/**
 * Created by kevin on 10/11/2016.
 */

public class MessageOptionsDialogHelper {

    public interface MessageOptionsListener {
        void onCopyTextRequested(FriendlyMessage friendlyMessage);

        void onDeleteMessageRequested(FriendlyMessage friendlyMessage);

        void onBlockPersonRequested(FriendlyMessage friendlyMessage);

        void onReportPersonRequested(FriendlyMessage friendlyMessage);
    }

    public AlertDialog showMessageOptions(@NonNull final Context context,
                                          @NonNull final FriendlyMessage friendlyMessage,
                                          @NonNull final MessageOptionsListener listener) {
        final TextView title = new TextView(context);
        title.setTextAppearance(context, R.style.MessageOptionsDialogTitle);
        FontUtil.setTextViewFont(title);
        int padding = context.getResources().getDimensionPixelSize(R.dimen.dialog_options_title_padding);
        title.setPadding(padding, padding, padding, 0);
        final String[] messageOptions = context.getResources().getStringArray(R.array.message_options);
        messageOptions[2] = messageOptions[2] + " " + friendlyMessage.getName();
        messageOptions[3] = messageOptions[3] + " " + friendlyMessage.getName();
        title.setText(R.string.message_options_title);
        AlertDialog dialog = new ThemedAlertDialog.Builder(context).
                setCancelable(true).
                setCustomTitle(title).
                setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).
                setItems(messageOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        switch (which) {
                            case 0:
                                listener.onCopyTextRequested(friendlyMessage);
                                break;
                            case 1:
                                listener.onDeleteMessageRequested(friendlyMessage);
                                break;
                            case 2:
                                listener.onBlockPersonRequested(friendlyMessage);
                                break;
                            case 3:
                                listener.onReportPersonRequested(friendlyMessage);
                                break;
                        }
                    }
                }).create();
        dialog.show();
        return dialog;
    }
}
