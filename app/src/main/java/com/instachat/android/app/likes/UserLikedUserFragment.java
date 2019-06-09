package com.instachat.android.app.likes;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.app.BaseFragment;
import com.instachat.android.Constants;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.R;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.data.model.User;
import com.instachat.android.databinding.FragmentGenericUsersBinding;

/**
 * Created by kevin on 10/12/2016.
 */

public class UserLikedUserFragment extends BaseFragment {

   public static Fragment newInstance(int userId, String username) {
      Fragment fragment = new UserLikedUserFragment();
      Bundle bundle = new Bundle();
      bundle.putInt(Constants.KEY_USERID, userId);
      bundle.putString(Constants.KEY_USERNAME, username);
      fragment.setArguments(bundle);
      return fragment;
   }

   public static final String TAG = "UserLikedUserFragment";
   private RecyclerView recyclerView;
   private UserClickedListener mUserClickedListener;
   private UserLikedUserAdapter mAdapter;
   FragmentGenericUsersBinding binding;

   @Override
   public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
      binding = FragmentGenericUsersBinding.inflate(inflater, container, false);
      View view = binding.getRoot();
      recyclerView = binding.recyclerView;
      return view;
   }

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      binding.customFragmentToolbarTitle.setText(getArguments().getString(Constants.KEY_USERNAME) + " " + getString(R.string.likes_plural));
      mUserClickedListener = new UserClickedListener() {
         @Override
         public void onUserClicked(final int userid, final String username, final String dpid, View transitionImageView) {
            PrivateChatActivity.startPrivateChatActivity(getActivity(), userid, username, dpid, false, transitionImageView, null, null);
         }
      };
      DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.USER_RECEIVED_LIKES_REF(getArguments().getInt(Constants.KEY_USERID)));
      mAdapter = new UserLikedUserAdapter(ref);
      mAdapter.setActivity(getActivity(), this);
      mAdapter.setUserClickedListener(mUserClickedListener);
      LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
      linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
      recyclerView.setLayoutManager(linearLayoutManager);
      recyclerView.setAdapter(mAdapter);
   }

   @Override
   public void onDestroy() {
      if (mAdapter != null)
         mAdapter.cleanup();
      mUserClickedListener = null;
      super.onDestroy();
   }
}
