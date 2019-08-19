package com.instachat.android.app.activity;

import android.content.Context;
import androidx.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.instachat.android.R;
import com.instachat.android.databinding.ViewPhotoOptionsBinding;

public class AttachPhotoOptionsDialogHelper {

    public interface PhotoOptionsListener {
        void onPhotoGallery();
        void onPhotoTake();
    }
    private Context context;
    private PhotoOptionsListener listener;
    public AttachPhotoOptionsDialogHelper(Context context, PhotoOptionsListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void showBottomDialog() {
        ViewPhotoOptionsBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.view_photo_options, null, false);
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(binding.getRoot());
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) binding.getRoot().getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog.show();
        binding.menuTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onPhotoGallery();
                bottomSheetDialog.dismiss();
            }
        });
        binding.menuChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onPhotoTake();
                bottomSheetDialog.dismiss();
            }
        });
    }
}
