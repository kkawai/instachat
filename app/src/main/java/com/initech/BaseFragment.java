package com.initech;

import android.os.Build;
import android.support.v4.app.Fragment;

/**
 * Created by kevin on 9/13/2016.
 */
public class BaseFragment extends Fragment {

    public boolean isActivityDestroyed() {
        if (Build.VERSION.SDK_INT >= 17)
            return getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing();
        return getActivity() == null || getActivity().isFinishing();
    }
}
