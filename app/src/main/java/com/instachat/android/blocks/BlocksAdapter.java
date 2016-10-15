package com.instachat.android.blocks;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.instachat.android.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.adapter.UserClickedListener;
import com.instachat.android.util.MLog;

public final class BlocksAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<BlockedUser, BlocksViewHolder> {

    public static final String TAG = "BlocksAdapter";

    private UserClickedListener mUserClickedListener;
    private Activity mActivity;
    private ActivityState mActivityState;

    public BlocksAdapter(Class<BlockedUser> modelClass, int modelLayout, Class<BlocksViewHolder> viewHolderClass, DatabaseReference ref) {
        super(modelClass, modelLayout, viewHolderClass, ref);
    }

    public void setUserClickedListener(UserClickedListener userClickedListener) {
        mUserClickedListener = userClickedListener;
    }

    public void setActivity(Activity activity, ActivityState activityState) {
        mActivity = activity;
        mActivityState = activityState;
    }

    @Override
    public BlocksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final BlocksViewHolder holder = super.onCreateViewHolder(parent, viewType);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BlockedUser blockedUser = getItem(holder.getAdapterPosition());
                mUserClickedListener.onUserClicked(blockedUser.id, blockedUser.getName(), blockedUser.getDpid());
            }
        });
        return holder;
    }

    @Override
    protected void populateViewHolder(final BlocksViewHolder viewHolder, BlockedUser model, int position) {
        viewHolder.username.setText(model.getName());
        viewHolder.username.setTextColor(mActivity.getResources().getColor(android.R.color.black));
        if (TextUtils.isEmpty(model.dpid)) {
            viewHolder.userPic.setImageResource(R.drawable.ic_account_circle_black_36dp);
            return;
        }
        Constants.DP_URL(model.id, model.getDpid(), new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (mActivityState.isActivityDestroyed())
                    return;
                try {
                    if (!task.isSuccessful()) {
                        throw new Exception("failed to get user pic");
                    }
                    Glide.with(mActivity).
                            load(task.getResult().toString()).
                            error(R.drawable.ic_account_circle_black_36dp).
                            into(viewHolder.userPic);
                } catch (final Exception e) {
                    MLog.e(TAG, "Constants.DP_URL failed to get pic from storage.  task: " + task.isSuccessful(), e);
                    viewHolder.userPic.setImageResource(R.drawable.ic_account_circle_black_36dp);
                }
            }
        });
    }

    @Override
    protected BlockedUser parseSnapshot(DataSnapshot snapshot) {
        MLog.d(TAG, "BlockedUser: " + snapshot);
        BlockedUser blockedUser = snapshot.getValue(BlockedUser.class);
        blockedUser.id = Integer.parseInt(snapshot.getKey());
        return blockedUser;
    }

    @Override
    public void cleanup() {
        mUserClickedListener = null;
        mActivity = null;
        mActivityState = null;
        super.cleanup();
    }
}