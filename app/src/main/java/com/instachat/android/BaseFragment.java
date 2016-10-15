package com.instachat.android;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

/**
 * Created by kevin on 9/13/2016.
 */
public class BaseFragment extends Fragment implements ActivityState {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View backArrow = getView().findViewById(R.id.back);
        if (backArrow != null) {
            backArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().onBackPressed();
                }
            });
        }
    }

    @Override
    public boolean isActivityDestroyed() {

        if (getActivity() != null && getActivity() instanceof ActivityState) {
            return ((ActivityState) getActivity()).isActivityDestroyed();
        }

        if (Build.VERSION.SDK_INT >= 17)
            return getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing();
        return getActivity() == null || getActivity().isFinishing();
    }
}
