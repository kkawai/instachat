package com.instachat.android.app.bans;

import android.os.Bundle;
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
import com.instachat.android.util.MLog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class BannedUsersFragment extends BaseFragment implements UserClickedListener {

    public static final String TAG = "BannedUsersFragment";

    private BannedUsersAdapter bannedUsersAdapter;
    private FragmentGenericUsersBinding binding;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGenericUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onUserClicked(int userid, String username, String dpid, View transitionImageView) {
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
                    Toast.makeText(getActivity(), "Not authorized", Toast.LENGTH_SHORT).show();
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

    private boolean wasSearched;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        binding.customFragmentToolbarTitle.setText(R.string.title_bans);
        bannedUsersAdapter = new BannedUsersAdapter(BannedUser.class, FirebaseDatabase.getInstance()
                .getReference(Constants.BANS), this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(bannedUsersAdapter);

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() < 3) {
                    return false;
                }
                MLog.i(TAG,"onQueryTextSubmit: " + query);
                bannedUsersAdapter.cleanup();
                bannedUsersAdapter = new BannedUsersAdapter(BannedUser.class, FirebaseDatabase.getInstance()
                                        .getReference(Constants.BANS).orderByChild("username")
                                        .startAt(query), BannedUsersFragment.this);
                binding.recyclerView.setAdapter(bannedUsersAdapter);
                wasSearched = true;
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                MLog.i(TAG,"onQueryTextChange: " + newText);
                return false;
            }
        });

        binding.searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                MLog.i(TAG,"onClose (search)");
                if (!wasSearched) {
                    return false; //save some resources
                }
                bannedUsersAdapter.cleanup();
                bannedUsersAdapter = new BannedUsersAdapter(BannedUser.class, FirebaseDatabase.getInstance()
                                        .getReference(Constants.BANS), BannedUsersFragment.this);
                binding.recyclerView.setAdapter(bannedUsersAdapter);
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        if (bannedUsersAdapter != null) {
            bannedUsersAdapter.cleanup();
        }
        super.onDestroy();
    }

}
