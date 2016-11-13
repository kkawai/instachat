package com.instachat.android.requests;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.BaseFragment;
import com.instachat.android.Constants;
import com.instachat.android.PrivateChatActivity;
import com.instachat.android.R;
import com.instachat.android.adapter.MessageViewHolder;
import com.instachat.android.adapter.UserClickedListener;
import com.instachat.android.model.PrivateChatSummary;
import com.instachat.android.util.Preferences;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/12/2016.
 */

public class RequestsFragment extends BaseFragment {

    public static final String TAG = "RequestsFragment";
    private RecyclerView mRecyclerView;
    private UserClickedListener mUserClickedListener;
    private RequestsAdapter mRequestsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generic_users, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TextView fragmentTitle = (TextView) getView().findViewById(R.id.customFragmentToolbarTitle);
        fragmentTitle.setText(R.string.pending_requests_title);
        mUserClickedListener = new UserClickedListener() {
            @Override
            public void onUserClicked(final int userid, final String username, final String dpid, final View transitionImageView) {
                acceptUserPrompt(userid, username, dpid, transitionImageView);
            }
        };
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_REQUESTS());
        mRequestsAdapter = new RequestsAdapter(PrivateChatSummary.class, R.layout.item_request, MessageViewHolder.class, ref);
        mRequestsAdapter.setUserClickedListener(mUserClickedListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mRequestsAdapter);
    }

    private void acceptUserPrompt(final int userid, final String username, final String dpid, final View transitionImageView) {

        new SweetAlertDialog(getActivity(), SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                .setTitleText(getActivity().getString(R.string.accept_request_title, username))
                .setContentText(getActivity().getString(R.string.accept_request_question, username))
                .setCancelText(getActivity().getString(R.string.no))
                .setConfirmText(getActivity().getString(R.string.yes))
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.cancel();
                        FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_REQUEST_STATUS_PARENT(userid, Preferences.getInstance().getUserId())).removeValue();
                        FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF()).child("" + userid).removeValue();
                    }
                }).setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                sweetAlertDialog.dismiss();
                PrivateChatActivity.startPrivateChatActivity(getActivity(), userid, username, dpid, transitionImageView, null, null);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        /**
                         * add the person to your private chat summaries - to your left drawer!
                         */
                        PrivateChatSummary privateChatSummary = new PrivateChatSummary();
                        privateChatSummary.setName(username);
                        privateChatSummary.setDpid(dpid);
                        privateChatSummary.setAccepted(true);
                        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF())
                                .child(userid + "");
                        ref.updateChildren(privateChatSummary.toMap());

                        /**
                         * remove the person from your pending requests
                         */
                        FirebaseDatabase.getInstance().getReference(Constants.PRIVATE_REQUEST_STATUS_PARENT(userid, Preferences.getInstance().getUserId())).
                                removeValue().
                                addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (isActivityDestroyed())
                                            return;
                                        /**
                                         * close pending requests fragment if there are none left
                                         */
                                        if (mRequestsAdapter != null && mRequestsAdapter.getItemCount() == 0)
                                            getActivity().onBackPressed();
                                    }
                                });

                    }
                }, 1500);
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
