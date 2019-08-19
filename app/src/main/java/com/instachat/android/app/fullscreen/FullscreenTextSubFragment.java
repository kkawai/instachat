package com.instachat.android.app.fullscreen;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.BaseFragment;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.data.db.OneTimeMessageDb;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.databinding.FragmentFullscreenItemBinding;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.UserPreferences;
import com.instachat.android.view.TextViewUtil;
import com.instachat.android.view.ZoomImageListener;

/**
 * Created by kevin on 8/21/2016.
 */
public class FullscreenTextSubFragment extends BaseFragment implements ZoomImageListener {

    public static final String TAG = "FullscreenTextSubFragment";
    private FriendlyMessage mFriendlyMessage;
    private FragmentFullscreenItemBinding binding;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullscreenItemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFriendlyMessage = getArguments().getParcelable(Constants.KEY_FRIENDLY_MESSAGE);
        if (mFriendlyMessage.getMT() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME) {
            setCustomFragmentToolbarTitle("");
            if (mFriendlyMessage.getUserid() != UserPreferences.getInstance().getUserId()) {
                FirebaseDatabase.getInstance().getReference(getArguments().getString(Constants.KEY_FRIENDLY_MESSAGE_DATABASE)).
                        child(mFriendlyMessage.getId()).
                        child(Constants.CHILD_MESSAGE_CONSUMED_BY_PARTNER).
                        setValue(true);
            }

        } else
            setCustomFragmentToolbarTitle(mFriendlyMessage.getName());

        binding.toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PrivateChatActivity.startPrivateChatActivity(getActivity(), mFriendlyMessage.getUserid(), mFriendlyMessage.getName(), mFriendlyMessage.getDpid(), false, null, null, null);
            }
        });

        final String photoNotAvailable = getResources().getString(R.string.photo_not_available);

        MLog.d(TAG, "onActivityCreated() friendlyMessage: " + mFriendlyMessage.toString());

        if (mFriendlyMessage.getMT() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME) {
            if (OneTimeMessageDb.getInstance().messageExists(mFriendlyMessage.getId())) {
                if (!TextUtils.isEmpty(mFriendlyMessage.getImageUrl())) {
                    mFriendlyMessage.setImageUrl(null);
                    mFriendlyMessage.setText(photoNotAvailable);
                }
            }
        }

        if (!TextUtils.isEmpty(mFriendlyMessage.getImageUrl())) {
            binding.messagePhotoView.setZoomableImageListener(this);
            binding.messagePhotoView.setImageUrl(mFriendlyMessage.getImageUrl());
        }

        if (TextUtils.isEmpty(mFriendlyMessage.getImageUrl())) {
            if (mFriendlyMessage.getText() != null)
                binding.autoResizeTextView.setText(StringUtil.stripNewLines(mFriendlyMessage.getText()));
            binding.autoResizeTextView.setMaxLines(Integer.MAX_VALUE);
            //float t = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()), getResources().getDisplayMetrics());
            //textView.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()), getResources().getDisplayMetrics()));
            //float x = getResources().getDimension(R.dimen.max_fullscreen_text_size);
            /**
             * Constants.MAX_FULLSCREEN_FONT_SIZE
             * This is the max font size due to a bug in android where it can't handle emoji bigger than 199
             * https://code.google.com/p/android/issues/detail?id=69706  opengl bug, which is quite stupid
             * to me; they haven't fixed it in over 2 years
             */
            binding.autoResizeTextView.setTextSize(Constants.MAX_FULLSCREEN_FONT_SIZE);
        } else if (!TextUtils.isEmpty(mFriendlyMessage.getImageUrl()) && !TextUtils.isEmpty(mFriendlyMessage.getText())) {
            binding.messageTextView.setText(mFriendlyMessage.getText());
        }

        if (mFriendlyMessage.getMT() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME) {
            if (OneTimeMessageDb.getInstance().messageExists(mFriendlyMessage.getId())) {
                if (!mFriendlyMessage.getText().equals(photoNotAvailable)) {
                    binding.autoResizeTextView.post(new Runnable() {
                        @Override
                        public void run() {
                            TextViewUtil.blurText(binding.autoResizeTextView, true);
                        }
                    });
                }
            }
        }

        binding.rotate90.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.messagePhotoView.rotate();
                animateRotateButton();
            }
        });

    }

    @Override
    public void onImageTouched() {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean isZoomed) {

    }

    private boolean mHasShownInitialScaleInAnimation;

    @Override
    public void onSetImageBitmap() {
        binding.rotate90.setVisibility(View.VISIBLE);
        if (!mHasShownInitialScaleInAnimation) {
            mHasShownInitialScaleInAnimation = true;
            AnimationUtil.scaleInFromCenter(binding.rotate90);
        }
    }

    private void animateRotateButton() {
        RotateAnimation anim = new RotateAnimation(0f, -90f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(300);
        //AnimationUtil.scaleInFromCenter(mRotateButton);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                RotateAnimation anim = new RotateAnimation(-90, -0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                anim.setDuration(300);
                binding.rotate90.startAnimation(anim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        binding.rotate90.startAnimation(anim);
    }

    @Override
    public void onDestroy() {
        if (mFriendlyMessage != null &&
                mFriendlyMessage.getMT() == FriendlyMessage.MESSAGE_TYPE_ONE_TIME) {
            if (mFriendlyMessage.getUserid() != UserPreferences.getInstance().getUserId()) {
                OneTimeMessageDb.getInstance().insertMessageId(mFriendlyMessage.getId());
            }
        }
        super.onDestroy();
    }
}
