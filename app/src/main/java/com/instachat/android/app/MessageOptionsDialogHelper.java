package com.instachat.android.app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.util.UserPreferences;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/11/2016.
 */

public class MessageOptionsDialogHelper {

    public interface MessageOptionsListener {
        void onCopyTextRequested(FriendlyMessage friendlyMessage);

        void onDeleteMessageRequested(FriendlyMessage friendlyMessage);

        void onBlockPersonRequested(FriendlyMessage friendlyMessage);

        void onReportPersonRequested(FriendlyMessage friendlyMessage);

        void onMessageOptionsDismissed();

        void onBan5Minutes(FriendlyMessage friendlyMessage);
        void onBan15Minutes(FriendlyMessage friendlyMessage);
        void onBan2Days(FriendlyMessage friendlyMessage);
        void onRemoveComments(FriendlyMessage friendlyMessage);
    }

    public interface SendOptionsListener {
        void onSendNormalRequested(FriendlyMessage friendlyMessage);

        void onSendOneTimeRequested(FriendlyMessage friendlyMessage);
    }

    public void showSendOptions(
            @NonNull final Context context,
            @NonNull final View anchor,
            final FriendlyMessage friendlyMessage,
            @NonNull final SendOptionsListener listener) {

        PopupMenu popupMenu = new PopupMenu(context, anchor);
        popupMenu.inflate(R.menu.send_options);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_send_normal:
                        listener.onSendNormalRequested(friendlyMessage);
                        break;
                    case R.id.menu_send_one_time:
                        listener.onSendOneTimeRequested(friendlyMessage);
                        break;
                    case R.id.menu_what_is_one_time:
                        new SweetAlertDialog(context, SweetAlertDialog.NORMAL_TYPE).setContentText(context.getString(R.string.one_time_definition)).show();
                    default:
                        break;
                }
                return true;
            }
        });
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
            }
        });
        popupMenu.show();
    }

    private boolean isAdmin() {
        return FirebaseRemoteConfig.getInstance().getString(Constants.KEY_ADMIN_USERS).contains("["+UserPreferences.getInstance().getUserId()+"]");
    }

    public void showMessageOptions(
            @NonNull final Context context,
            @NonNull final View anchor,
            @NonNull final FriendlyMessage friendlyMessage,
            @NonNull final MessageOptionsListener listener) {

        PopupMenu popupMenu = new PopupMenu(context, anchor);
        popupMenu.inflate(R.menu.message_options);
        if (TextUtils.isEmpty(friendlyMessage.getText()) ||
                friendlyMessage.getMessageType() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME) {
            popupMenu.getMenu().removeItem(R.id.menu_copy_text);
        }
        if (!TextUtils.isEmpty(friendlyMessage.getImageUrl()) && TextUtils.isEmpty(friendlyMessage.getText())) {
            popupMenu.getMenu().findItem(R.id.menu_delete_message).setTitle(R.string.delete_photo);
        }
        if (!FirebaseRemoteConfig.getInstance().getBoolean(Constants.KEY_ALLOW_DELETE_OTHER_MESSAGES)) {
            if (UserPreferences.getInstance().getUserId() != friendlyMessage.getUserid()) {
                if (!isAdmin()) {
                    popupMenu.getMenu().removeItem(R.id.menu_delete_message);
                }
            }
        }
        if (UserPreferences.getInstance().getUserId() == friendlyMessage.getUserid()) {
            popupMenu.getMenu().removeItem(R.id.menu_report_user);
            popupMenu.getMenu().removeItem(R.id.menu_block_user);
        } else {
            popupMenu.getMenu().findItem(R.id.menu_block_user).setTitle(context.getString(R.string.block) + " " + friendlyMessage.getName());
            popupMenu.getMenu().findItem(R.id.menu_report_user).setTitle(context.getString(R.string.report) + " " + friendlyMessage.getName());
        }
        if (!isAdmin()) {
            popupMenu.getMenu().removeItem(R.id.admin_menu_remove_comments);
            popupMenu.getMenu().removeItem(R.id.admin_menu_ban_5);
            popupMenu.getMenu().removeItem(R.id.admin_menu_ban_15);
            popupMenu.getMenu().removeItem(R.id.admin_menu_ban_2_days);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_copy_text:
                        listener.onCopyTextRequested(friendlyMessage);
                        break;
                    case R.id.menu_delete_message:
                        listener.onDeleteMessageRequested(friendlyMessage);
                        break;
                    case R.id.menu_block_user:
                        listener.onBlockPersonRequested(friendlyMessage);
                        break;
                    case R.id.menu_report_user:
                        listener.onReportPersonRequested(friendlyMessage);
                        break;
                    case R.id.admin_menu_remove_comments:
                        listener.onRemoveComments(friendlyMessage);
                        break;
                    case R.id.admin_menu_ban_2_days:
                        listener.onBan2Days(friendlyMessage);
                        break;
                    case R.id.admin_menu_ban_5:
                        listener.onBan2Days(friendlyMessage);
                        break;
                    case R.id.admin_menu_ban_15:
                        listener.onBan2Days(friendlyMessage);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                listener.onMessageOptionsDismissed();
            }
        });
        popupMenu.show();

    }

    /*
    defined in strings.xml

    <string-array name="message_options">
        <item>Copy Text</item>
        <item>Delete Message</item>
        <item>Block</item>
        <item>Report</item>
    </string-array>


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
    */
}
