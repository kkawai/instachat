package com.instachat.android.app.fullscreen;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.databinding.FragmentFullscreenBinding;
import com.instachat.android.databinding.FragmentFullscreenItemBinding;
import com.instachat.android.util.MLog;

/**
 * Created by kevin on 8/21/2016.
 */
public class FullScreenTextFragment extends Fragment {

    public static final String TAG = "FullScreenTextFragment";
    private FriendlyMessageContainer mFriendlyMessageContainer;
    private FragmentStatePagerAdapter mPagerAdapter;
    private int mLastPos;
    private FragmentFullscreenBinding binding;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof FriendlyMessageContainer)) {
            throw new IllegalStateException("parent activity does not implement FriendlyMessageContainer");
        }
        mFriendlyMessageContainer = (FriendlyMessageContainer)context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_fullscreen, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ViewPager viewPager = binding.viewPager;
        mPagerAdapter = new FragmentStatePagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(final int position) {
                final Fragment fragment = new FullscreenTextSubFragment();
                final Bundle args = new Bundle();
                FriendlyMessage friendlyMessage = mFriendlyMessageContainer.getFriendlyMessage(position);
                MLog.d(TAG, "onActivityCreated() friendlyMessage: " + friendlyMessage.toString());
                args.putParcelable(Constants.KEY_FRIENDLY_MESSAGE, friendlyMessage);
                args.putString(Constants.KEY_FRIENDLY_MESSAGE_DATABASE, mFriendlyMessageContainer.getFriendlyMessageDatabase());
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
        viewPager.setCurrentItem(mLastPos, true);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mLastPos = position;
                FriendlyMessage friendlyMessage = mFriendlyMessageContainer.getFriendlyMessage(position);
                MLog.d(TAG, "onPageSelected: " + position + " " + friendlyMessage);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private int getStartingPos() {
        final int startingPos = getArguments().getInt(Constants.KEY_STARTING_POS, -1);
        if (startingPos > -1 && startingPos <= mFriendlyMessageContainer.getFriendlyMessageCount() - 1) {
            return startingPos;
        } else {
            return mFriendlyMessageContainer.getFriendlyMessageCount() - 1;
        }
    }

    public void notifyDataSetChanged() {
        mPagerAdapter.notifyDataSetChanged();
        MLog.i(TAG, "notifyDataSetChanged()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFriendlyMessageContainer.setCurrentFriendlyMessage(mLastPos);
    }
}
