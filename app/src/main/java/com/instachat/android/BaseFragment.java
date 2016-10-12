package com.instachat.android;

import android.os.Build;
import android.support.v4.app.Fragment;

/**
 * Created by kevin on 9/13/2016.
 */
public class BaseFragment extends Fragment implements ActivityState {

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
