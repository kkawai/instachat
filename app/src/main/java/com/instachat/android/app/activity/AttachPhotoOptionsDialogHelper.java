package com.instachat.android.app.activity;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.instachat.android.R;

public class AttachPhotoOptionsDialogHelper {

    public interface PhotoOptionsListener {
        void onTakePhoto();
        void onChoosePhoto();
    }
    private Context context;
    private PhotoOptionsListener listener;
    public AttachPhotoOptionsDialogHelper(Context context, PhotoOptionsListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void showBottomDialog() {
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.view_photo_options, null);
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(bottomSheetView);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog.show();
        bottomSheetView.findViewById(R.id.menu_take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onTakePhoto();
                bottomSheetDialog.dismiss();
            }
        });
        bottomSheetView.findViewById(R.id.menu_choose_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onChoosePhoto();
                bottomSheetDialog.dismiss();
            }
        });
    }
}
