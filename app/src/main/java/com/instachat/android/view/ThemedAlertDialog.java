package com.instachat.android.view;

import android.content.Context;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;

import com.instachat.android.R;

/**
 * Created by igo on 4/28/15.
 * Use ThemedAlertDialog.Builder() instead of AlertDialog.Builder() to create an AlertDialog that matches
 * the Material Design mockups in dimension and theme.
 */
public final class ThemedAlertDialog extends AlertDialog {

    public ThemedAlertDialog(Context context) {
        super(context);
    }

    public static class Builder extends AlertDialog.Builder {
        Context mContext;

        /**
         * Use the given theme for most Material Design AlertDialogs. Alter width and height of the dialog to match the mockups.
         *
         * @param context
         */
        public Builder(Context context, int theme) {
            super(context, theme);
            mContext = context;
        }

        /**
         * Use the project's theme for most Material Design AlertDialogs. Alter width and height of the dialog to match the mockups.
         *
         * @param context
         */
        public Builder(Context context) {
            this(context, Build.VERSION.SDK_INT >= 21 ? android.R.style.Theme_Material_Light_Dialog_NoActionBar : R.style.AppCompatAlertDialog);
        }

        /**
         * @return An AlertDialog with a width and height matching the Material Design mockups.
         */
      /*  This only applied to V4.  let's go with stock for V5+

      @Override
      public AlertDialog show() {
         AlertDialog dialog = super.show();
         int width = mContext.getResources().getDimensionPixelSize(R.dimen.alert_dialog_width);
         int height = mContext.getResources().getDimensionPixelSize(R.dimen.alert_dialog_height);
         dialog.getWindow().setLayout(width, height);

         return dialog;
      }
      */
    }
}
