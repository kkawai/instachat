package com.instachat.android.app.blocks;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
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
import com.instachat.android.app.BaseFragment;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.data.model.PrivateChatSummary;
import com.instachat.android.databinding.FragmentGenericUsersBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by kevin on 10/12/2016.
 */

public class BlocksFragment extends BaseFragment {

    public static final String TAG = "BlocksFragment";
    private UserClickedListener mUserClickedListener;
    private BlocksAdapter mBlocksAdapter;
    private FragmentGenericUsersBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_generic_users, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.customFragmentToolbarTitle.setText(R.string.manage_blocks);
        mUserClickedListener = new UserClickedListener() {
            @Override
            public void onUserClicked(final int userid, final String username, final String dpid, final View transitionImageView) {

                new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE)
                        .setTitleText(getActivity().getString(R.string.unblock_person_title, username))
                        .setContentText(getActivity().getString(R.string.unblock_person_question, username))
                        .setCancelText(getActivity().getString(android.R.string.no))
                        .setConfirmText(getActivity().getString(android.R.string.yes))
                        .showCancelButton(true)
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.cancel();
                            }
                        }).setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {

                        sweetAlertDialog.dismiss();
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF());
                        ref.child(userid + "").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    createPrivateChatSummary(userid, username, dpid);
                                    new SweetAlertDialog(getActivity(), SweetAlertDialog.SUCCESS_TYPE)
                                            .setTitleText(getActivity().getString(R.string.success_exclamation))
                                            .setContentText(getActivity().getString(R.string.unblock_person_success, username))
                                            .show();
                                } else {
                                    new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE)
                                            .setTitleText(getActivity().getString(R.string.oops_exclamation))
                                            .setContentText(getActivity().getString(R.string.unblock_person_failed, username))
                                            .show();
                                }
                            }
                        });
                    }
                }).show();
            }
        };
        DatabaseReference userBlocksRef = FirebaseDatabase.getInstance().getReference(Constants.MY_BLOCKS_REF());
        mBlocksAdapter = new BlocksAdapter(BlockedUser.class, userBlocksRef);
        mBlocksAdapter.setUserClickedListener(mUserClickedListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(mBlocksAdapter);
    }

    private void createPrivateChatSummary(int userid, String username, String dpid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.MY_PRIVATE_CHATS_SUMMARY_PARENT_REF());
        PrivateChatSummary summary = new PrivateChatSummary();
        summary.setName(username);
        summary.setDpid(dpid);
        summary.setAccepted(true);
        ref.child(userid + "").updateChildren(summary.toMap());
    }

    @Override
    public void onDestroy() {
        if (mBlocksAdapter != null)
            mBlocksAdapter.cleanup();
        super.onDestroy();
    }
}
