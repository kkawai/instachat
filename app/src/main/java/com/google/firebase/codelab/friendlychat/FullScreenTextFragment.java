package com.google.firebase.codelab.friendlychat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ath.fuel.FuelInjector;
import com.ath.fuel.Lazy;
import com.initech.util.MLog;

/**
 * Created by kevin on 8/21/2016.
 */
public class FullScreenTextFragment extends Fragment {

    public static final String TAG = "FullScreenTextFragment";
    private final Lazy<PagerAdapterHelper> mPagerAdapterHelper = Lazy.attain(this, PagerAdapterHelper.class);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_fullscreen,container,false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ViewPager viewPager = (ViewPager)getView().findViewById(R.id.view_pager);
        FuelInjector.ignite(getActivity(),this);
        mPagerAdapterHelper.get().getPagerAdapter(new PagerAdapterHelper.PagerAdapterHelperCallback() {
            @Override
            public void onPagerAdapterHelperResult(FragmentStatePagerAdapter pagerAdapter) {
                viewPager.setAdapter(pagerAdapter);
                MLog.i(TAG,"onActivityCreated() viewPager.setAdapter():"+pagerAdapter + " count: "+pagerAdapter.getCount());
                viewPager.setCurrentItem(pagerAdapter.getCount()-1,true);
            }
        });
    }

}
