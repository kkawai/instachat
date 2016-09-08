package com.google.firebase.codelab.friendlychat.fullscreen;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ath.fuel.FuelInjector;
import com.google.firebase.codelab.friendlychat.R;
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.initech.Constants;
import com.initech.util.MLog;

/**
 * Created by kevin on 8/21/2016.
 */
public class FullScreenTextFragment extends Fragment {

    public static final String TAG = "FullScreenTextFragment";
    private FriendlyMessageContainer mFriendlyMessageContainer;
    private FragmentStatePagerAdapter mPagerAdapter;
    private int mLastPos;

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
        mPagerAdapter = new FragmentStatePagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(final int position) {
                final Fragment fragment = new FullscreenTextSubFragment();
                final Bundle args = new Bundle();
                args.putParcelable(Constants.KEY_FRIENDLY_MESSAGE,mFriendlyMessageContainer.getFriendlyMessage(position));
                fragment.setArguments(args);
                return fragment;
            }

            @Override
            public int getCount() {
                return mFriendlyMessageContainer.getFriendlyMessageCount();
            }
        };
        viewPager.setAdapter(mPagerAdapter);
        mLastPos = getStartingPos();
        viewPager.setCurrentItem(mLastPos,true);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mLastPos = position;
                FriendlyMessage friendlyMessage = mFriendlyMessageContainer.getFriendlyMessage(position);
                MLog.d(TAG,"onPageSelected: "+position + " " + friendlyMessage);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private int getStartingPos() {
        final int startingPos = getArguments().getInt(Constants.KEY_STARTING_POS,-1);
        if (startingPos > -1 && startingPos <= mFriendlyMessageContainer.getFriendlyMessageCount()-1) {
            return startingPos;
        } else {
            return mFriendlyMessageContainer.getFriendlyMessageCount()-1;
        }
    }

    public void setFriendlyMessageContainer(final FriendlyMessageContainer messageContainer) {
        mFriendlyMessageContainer = messageContainer;
    }

    public void notifyDataSetChanged() {
        mPagerAdapter.notifyDataSetChanged();
        MLog.i(TAG,"notifyDataSetChanged()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFriendlyMessageContainer.setCurrentFriendlyMessage(mLastPos);
    }
}
