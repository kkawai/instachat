package com.google.firebase.codelab.friendlychat;

/**
 * Created by kevin on 8/2/2016.
 */
import android.support.v4.app.FragmentStatePagerAdapter;

import com.ath.fuel.ActivitySingleton;

@ActivitySingleton
public final class PagerAdapterHelper {

    private PagerAdapterHelperListener mListener;

    public interface PagerAdapterHelperListener {
        void onPagerAdapterRequested(PagerAdapterHelperCallback callback);
    }

    public interface PagerAdapterHelperCallback {
        void onPagerAdapterHelperResult(FragmentStatePagerAdapter pagerAdapter);
    }

    public void setListener(PagerAdapterHelperListener listener) {
        mListener = listener;
    }

    public void getPagerAdapter(PagerAdapterHelperCallback callback) {
        if (mListener != null) {
            mListener.onPagerAdapterRequested(callback);
        }
    }

}
