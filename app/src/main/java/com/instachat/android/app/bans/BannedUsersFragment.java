package com.instachat.android.app.bans;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.BaseFragment;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.databinding.FragmentGenericUsersBinding;
import com.instachat.android.util.AdminUtil;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;

import java.util.concurrent.TimeUnit;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

public class BannedUsersFragment extends BaseFragment {

    public static final String TAG = "BannedUsersFragment";

    private BannedUsersAdapter bannedUsersAdapter;
    private FragmentGenericUsersBinding binding;
    private ChildEventListener childEventListener;
    private DatabaseReference databaseReference;
    private Disposable disposable;

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

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.BANS);
                        ref.child(userid + "").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
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
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(bannedUsersAdapter);

        /* kkawai, let's not remove bans in the client anymore!
        disposable = Observable.timer(350, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        removeExpiredBans();
                    }
                })
                .subscribe();
                */

    }

    private void removeBan(int userId) {
        FirebaseDatabase.getInstance().getReference(Constants.BANS + userId).removeValue();
    }

    private void removeExpiredBans() {
        databaseReference = FirebaseDatabase.getInstance().getReference(Constants.BANS);
        childEventListener = databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                BannedUser bannedUser = dataSnapshot.getValue(BannedUser.class);
                bannedUser.id = Integer.parseInt(dataSnapshot.getKey());
                MLog.i(TAG, "Found banned user.  Check if expired.");
                if (System.currentTimeMillis() > bannedUser.banExpiration) {
                    removeBan(bannedUser.id);
                    MLog.i(TAG, bannedUser.username + " is no longer banned. Remove from bans");
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //not implemented
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //not implemented
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                //not implemented
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //not implemented
            }
        });
    }

    @Override
    public void onDestroy() {
        if (bannedUsersAdapter != null) {
            bannedUsersAdapter.cleanup();
        }
        if (databaseReference != null && childEventListener != null) {
            databaseReference.removeEventListener(childEventListener);
        }
        if (disposable != null) {
            disposable.dispose();
        }
        super.onDestroy();
    }
}
