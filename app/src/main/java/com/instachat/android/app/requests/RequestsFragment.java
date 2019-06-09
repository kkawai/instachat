package com.instachat.android.app.requests;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.BaseFragment;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.app.analytics.Events;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.databinding.FragmentGenericUsersBinding;
import com.instachat.android.util.UserPreferences;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/12/2016.
 */

public class RequestsFragment extends BaseFragment {

    public static final String TAG = "RequestsFragment";
    private UserClickedListener mUserClickedListener;
    private RequestsAdapter mRequestsAdapter;
    private FragmentGenericUsersBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGenericUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.customFragmentToolbarTitle.setText(R.string.pending_requests_title);
        mUserClickedListener = new UserClickedListener() {
            @Override
            public void onUserClicked(final int userid, final String username, final String dpid, final View transitionImageView) {
                acceptUserPrompt(userid, username, dpid, transitionImageView);
            }
        };
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_REQUESTS_REF())
                .orderByChild(Constants.FIELD_LAST_MESSAGE_SENT_TIMESTAMP)
                .limitToLast((int) FirebaseRemoteConfig.getInstance().getLong(Constants.KEY_MAX_PRIVATE_CHATS)).getRef();
        mRequestsAdapter = new RequestsAdapter(PrivateChatSummary.class, ref);
        mRequestsAdapter.setUserClickedListener(mUserClickedListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(mRequestsAdapter);
    }

    private void acceptUserPrompt(final int userid, final String username, final String dpid, final View transitionImageView) {

        new SweetAlertDialog(getActivity(), SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                .setTitleText(getActivity().getString(R.string.accept_request_title) + " " + username + "?")
                .setContentText(username + " " + getActivity().getString(R.string.accept_request_question))
                .setCancelText(getActivity().getString(R.string.no))
                .setConfirmText(getActivity().getString(R.string.yes))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.cancel();
                        FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_REQUEST_STATUS_PARENT_REF(userid, UserPreferences.getInstance().getUserId())).removeValue();
                        FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).child("" + userid).removeValue();
                        FirebaseAnalytics.getInstance(getActivity()).logEvent(Events.PENDING_REQUEST_DENIED, null);
                    }
                }).setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                sweetAlertDialog.dismiss();
                PrivateChatActivity.startPrivateChatActivity(getActivity(), userid, username, dpid, true, transitionImageView, null, null);
                FirebaseAnalytics.getInstance(getActivity()).logEvent(Events.PENDING_REQUEST_ACCEPTED, null);
            }
        }).setCustomImage(dpid, R.drawable.ic_anon_person_48dp).
                show();
    }

    @Override
    public void onDestroy() {
        if (mRequestsAdapter != null)
            mRequestsAdapter.cleanup();
        super.onDestroy();
    }
}
