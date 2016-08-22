package com.google.firebase.codelab.friendlychat;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.initech.Constants;
import com.initech.util.MLog;
import com.initech.util.ScreenUtil;
import com.lb.auto_fit_textview.AutoResizeTextView;

/**
 * Created by kevin on 8/21/2016.
 */
public class FullscreenTextSubFragment extends Fragment {

    public static final String TAG = "FullscreenTextSubFragment";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_fullscreen_item,container,false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final AutoResizeTextView textView = (AutoResizeTextView)getView().findViewById(R.id.textView);
        textView.setText(getArguments().getString(Constants.KEY_TEXT));
        textView.setMaxLines(Integer.MAX_VALUE);
        textView.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()),getResources().getDisplayMetrics()));
        MLog.i(TAG,"onActivityCreated(): "+getArguments().getString(Constants.KEY_TEXT) + " textView.height: "+textView.getHeight());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final AutoResizeTextView textView = (AutoResizeTextView)getView().findViewById(R.id.textView);
        textView.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ScreenUtil.getScreenHeight(getActivity()),getResources().getDisplayMetrics()));
    }
}
