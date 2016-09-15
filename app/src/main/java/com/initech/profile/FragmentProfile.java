package com.initech.profile;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.codelab.friendlychat.R;
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.initech.BaseFragment;
import com.initech.Constants;
import com.initech.api.NetworkApi;
import com.initech.util.MLog;

/**
 * Created by kevin on 9/13/2016.
 */
public class FragmentProfile extends BaseFragment {

    public static final String TAG = "FragmentProfile";

    public static Fragment newInstance(final FriendlyMessage message) {
        Fragment fragment = new FragmentProfile();
        Bundle args = new Bundle();
        args.putParcelable(Constants.KEY_FRIENDLY_MESSAGE, message);
        fragment.setArguments(args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fragment.setSharedElementEnterTransition(new ProfilePicTransition());
            fragment.setEnterTransition(new Fade());
            fragment.setExitTransition(new Fade());
            fragment.setSharedElementReturnTransition(new ProfilePicTransition());
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_profile, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FriendlyMessage friendlyMessage = getArguments().getParcelable(Constants.KEY_FRIENDLY_MESSAGE);
        setupToolbar(friendlyMessage.getName());
        ((TextView) getView().findViewById(R.id.username)).setText(friendlyMessage.getName());
        final ImageView pic = (ImageView) getView().findViewById(R.id.profile_pic);

        Constants.DP_URL(friendlyMessage.getUserid(), friendlyMessage.getDpid(), new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (isActivityDestroyed())
                            return;
                        try {
                            Glide.with(FragmentProfile.this)
                                    .load(task.getResult().toString())
                                    .error(R.drawable.ic_account_circle_black_36dp)
                                    .crossFade()
                                    .into(pic);
                        } catch (Exception e) {
                            MLog.e(TAG, "Constants.DP_URL user profile pic exist in google cloud storage", e);
                            pic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                        }
                    }
                }
        );

//        getView().findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                getActivity().onBackPressed();
//            }
//        });
    }

    private void setSupportActionBar(Toolbar toolbar) {
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    }

    private ActionBar getSupportActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    private void setupToolbar(final String username) {
        /*
        Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        //ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        //ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(username);
        */
    }
}
