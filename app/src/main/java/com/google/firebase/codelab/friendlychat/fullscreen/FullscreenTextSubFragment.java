package com.google.firebase.codelab.friendlychat.fullscreen;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.google.firebase.codelab.friendlychat.R;
import com.initech.Constants;
import com.initech.util.MLog;
import com.initech.util.ScreenUtil;
import com.initech.view.AutoResizeTextView;

/**
 * Created by kevin on 8/21/2016.
 */
public class FullscreenTextSubFragment extends Fragment {

    public static final String TAG = "FullscreenTextSubFragment";
    private FriendlyMessage mFriendlyMessage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_fullscreen_item, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFriendlyMessage = getArguments().getParcelable(Constants.KEY_FRIENDLY_MESSAGE);
        final AutoResizeTextView textView = (AutoResizeTextView) getView().findViewById(R.id.textView);
        //textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        textView.setText(mFriendlyMessage.getText());
        textView.setMaxLines(Integer.MAX_VALUE);
        //float t = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()), getResources().getDisplayMetrics());
        //textView.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()), getResources().getDisplayMetrics()));
        //float x = getResources().getDimension(R.dimen.max_fullscreen_text_size);
        /**
         * Constants.MAX_FULLSCREEN_FONT_SIZE
         * This is the max font size due to a bug in android where it can't handle emoji bigger than 199
         * https://code.google.com/p/android/issues/detail?id=69706  opengl bug, which is quite stupid
         * to me; they haven't fixed it in over 2 years
         */
        textView.setTextSize(Constants.MAX_FULLSCREEN_FONT_SIZE);
        MLog.i(TAG, "onActivityCreated(): " + mFriendlyMessage.getText() + " textView.height: " + textView.getHeight());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final AutoResizeTextView textView = (AutoResizeTextView) getView().findViewById(R.id.textView);
        textView.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()), getResources().getDisplayMetrics()));
    }
}
