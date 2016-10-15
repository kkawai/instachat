package com.instachat.android.blocks;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import com.instachat.android.R;
import com.instachat.android.font.FontUtil;
import com.instachat.android.view.ThemedAlertDialog;

import org.jetbrains.annotations.NotNull;

/**
 * Created by kevin on 10/11/2016.
 */

public class BlockedUserOptionsDialogHelper {

    public interface BlockedUserOptionsListener {
        void onReportPersonRequested(int userid, String username);

        void onUnblockPersonRequested(int userid, String username);
    }

    public AlertDialog showBlockedUserOptions(@NotNull final Context context,
                                              final int userid,
                                              final String username,
                                              @NotNull final BlockedUserOptionsListener listener) {
        final TextView title = new TextView(context);
        title.setTextAppearance(context, R.style.MessageOptionsDialogTitle);
        FontUtil.setTextViewFont(title);
        int padding = context.getResources().getDimensionPixelSize(R.dimen.dialog_options_title_padding);
        title.setPadding(padding, padding, padding, 0);
        final String[] messageOptions = context.getResources().getStringArray(R.array.blocked_user_options);
        title.setText(R.string.blocked_user_options_title);
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
                                listener.onUnblockPersonRequested(userid,username);
                                break;
                            case 1:
                                listener.onReportPersonRequested(userid,username);
                                break;
                        }
                    }
                }).create();
        dialog.show();
        return dialog;
    }
}
