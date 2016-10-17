package com.instachat.android.fullscreen;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.font.FontUtil;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.view.ZoomImageListener;
import com.instachat.android.view.ZoomableImageView;

/**
 * Created by kevin on 8/21/2016.
 */
public class FullscreenTextSubFragment extends Fragment implements ZoomImageListener {

    public static final String TAG = "FullscreenTextSubFragment";
    private FriendlyMessage mFriendlyMessage;
    private ZoomableImageView mZoomableImageView;
    private TextView mAutoResizeTextView, mTextView;
    private ImageView mRotateButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_fullscreen_item, container, false);
        mZoomableImageView = (ZoomableImageView) view.findViewById(R.id.messagePhotoView);
        mAutoResizeTextView = (TextView) view.findViewById(R.id.autoResizeTextView);
        mTextView = (TextView) view.findViewById(R.id.messageTextView);
        mRotateButton = (ImageView) view.findViewById(R.id.rotate90);
        mRotateButton.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFriendlyMessage = getArguments().getParcelable(Constants.KEY_FRIENDLY_MESSAGE);

        MLog.d(TAG, "onActivityCreated() friendlyMessage: " + mFriendlyMessage.toString());

        if (!TextUtils.isEmpty(mFriendlyMessage.getImageUrl())) {
            mZoomableImageView.setZoomableImageListener(this);
            mZoomableImageView.setImageUrl(mFriendlyMessage.getImageUrl());
        }

        if (TextUtils.isEmpty(mFriendlyMessage.getImageUrl())) {
            FontUtil.setTextViewFont(mAutoResizeTextView);
            mAutoResizeTextView.setText(mFriendlyMessage.getText());
            mAutoResizeTextView.setMaxLines(Integer.MAX_VALUE);
            //float t = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()), getResources().getDisplayMetrics());
            //textView.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()), getResources().getDisplayMetrics()));
            //float x = getResources().getDimension(R.dimen.max_fullscreen_text_size);
            /**
             * Constants.MAX_FULLSCREEN_FONT_SIZE
             * This is the max font size due to a bug in android where it can't handle emoji bigger than 199
             * https://code.google.com/p/android/issues/detail?id=69706  opengl bug, which is quite stupid
             * to me; they haven't fixed it in over 2 years
             */
            mAutoResizeTextView.setTextSize(Constants.MAX_FULLSCREEN_FONT_SIZE);
        } else if (!TextUtils.isEmpty(mFriendlyMessage.getImageUrl()) && !TextUtils.isEmpty(mFriendlyMessage.getText())) {
            mTextView.setText(mFriendlyMessage.getText());
        }

        mRotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mZoomableImageView.rotate();
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
        mRotateButton.setVisibility(View.VISIBLE);
        if (!mHasShownInitialScaleInAnimation) {
            mHasShownInitialScaleInAnimation = true;
            AnimationUtil.scaleInFromCenter(mRotateButton);
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
                mRotateButton.startAnimation(anim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mRotateButton.startAnimation(anim);
    }
}
