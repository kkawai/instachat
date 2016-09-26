package com.instachat.android.profile;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.instachat.android.BaseFragment;
import com.instachat.android.Constants;
import com.instachat.android.MyApp;
import com.instachat.android.PrivateChatActivity;
import com.instachat.android.R;
import com.instachat.android.api.NetworkApi;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.User;
import com.instachat.android.util.MLog;

import org.json.JSONObject;

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
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            fragment.setSharedElementEnterTransition(new ProfilePicTransition());
//            fragment.setEnterTransition(new Fade());
//            fragment.setExitTransition(new Fade());
//            fragment.setSharedElementReturnTransition(new ProfilePicTransition());
//        }
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
        NetworkApi.getUserById(this, friendlyMessage.getUserid(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final User remote = User.fromResponse(response);
                    ((TextView) getView().findViewById(R.id.username)).setText(remote.getUsername());
                    Constants.DP_URL(remote.getId(), remote.getProfilePicUrl(), new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (isActivityDestroyed())
                                        return;
                                    if (!task.isSuccessful()) {
                                        pic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                                        return;
                                    }
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
                    pic.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            MLog.d(TAG, "starting chat activity with " + remote.getId() + "  username: " + remote.getUsername());
                            PrivateChatActivity.startPrivateChatActivity(getContext(), remote.getId());
                        }
                    });
                } catch (Exception e) {
                    MLog.e(TAG, "Fail within onResponse(1)", e);
                    try {
                        Toast.makeText(getActivity(), getString(R.string.general_api_error, "1"), Toast.LENGTH_SHORT).show();
                        getActivity().onBackPressed();
                    } catch (Exception x) {
                        MLog.e(TAG, "Fail within onResponse(2)", x);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                MLog.e(TAG, "Fail within onErrorResponse(1)", error);
                try {
                    Toast.makeText(getActivity(), getString(R.string.general_api_error, "2"), Toast.LENGTH_SHORT).show();
                    getActivity().onBackPressed();
                } catch (Exception e) {
                    MLog.e(TAG, "Fail within onErrorResponse(2)", e);
                }
            }
        });

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

    @Override
    public void onDestroy() {
        MyApp.getInstance().getRequestQueue().cancelAll(this);
        super.onDestroy();
    }
}
