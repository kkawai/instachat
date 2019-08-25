package com.instachat.android.app.bans;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.BaseFragment;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.databinding.FragmentGenericUsersBinding;
import com.instachat.android.util.AdminUtil;

import androidx.recyclerview.widget.RecyclerView;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class BannedUsersFragment extends BaseFragment {

    public static final String TAG = "BannedUsersFragment";

    private BannedUsersAdapter bannedUsersAdapter;
    private FragmentGenericUsersBinding binding;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGenericUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.customFragmentToolbarTitle.setText(R.string.title_bans);
        UserClickedListener userClickedListener = new UserClickedListener() {
            @Override
            public void onUserClicked(final int userid, final String username, final String dpid, final View transitionImageView) {

                new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Remove Ban")
                        .setContentText("Remove Ban on " + username + "?")
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
                        if (!AdminUtil.isMeAdmin()) {
                            Toast.makeText(getActivity(),"Not authorized", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        BanHelper.unban(userid, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    new SweetAlertDialog(getActivity(), SweetAlertDialog.SUCCESS_TYPE)
                                            .setTitleText(getActivity().getString(R.string.success_exclamation))
                                            .setContentText(username + " has been unbanned.")
                                            .show();
                                } else {
                                    new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE)
                                            .setTitleText(getActivity().getString(R.string.oops_exclamation))
                                            .setContentText("Failed to unban " + username)
                                            .show();
                                }
                            }
                        });
                    }
                }).show();
            }
        };
        bannedUsersAdapter = new BannedUsersAdapter(BannedUser.class, FirebaseDatabase.getInstance().getReference(Constants.BANS));
        bannedUsersAdapter.setUserClickedListener(userClickedListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(bannedUsersAdapter);
    }

    @Override
    public void onDestroy() {
        if (bannedUsersAdapter != null) {
            bannedUsersAdapter.cleanup();
        }
        super.onDestroy();
    }
}
