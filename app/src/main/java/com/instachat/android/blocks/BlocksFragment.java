package com.instachat.android.blocks;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instachat.android.BaseFragment;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.adapter.UserClickedListener;

/**
 * Created by kevin on 10/12/2016.
 */

public class BlocksFragment extends BaseFragment {

    public static final String TAG = "BlocksFragment";
    private RecyclerView mBlocksRecyclerView;
    private UserClickedListener mUserClickedListener;
    private BlocksAdapter mBlocksAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blocks, container, false);
        mBlocksRecyclerView = (RecyclerView) view.findViewById(R.id.blocksRecyclerView);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUserClickedListener = new UserClickedListener() {
            @Override
            public void onUserClicked(int userid) {
                //todo
            }
        };
        DatabaseReference userBlocksRef = FirebaseDatabase.getInstance().getReference(Constants.BLOCKS_REF());
        mBlocksAdapter = new BlocksAdapter(BlockedUser.class, R.layout.item_person, BlocksViewHolder.class, userBlocksRef);
        mBlocksAdapter.setActivity(getActivity(), this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mBlocksRecyclerView.setLayoutManager(linearLayoutManager);
        mBlocksRecyclerView.setAdapter(mBlocksAdapter);
    }

    @Override
    public void onDestroy() {
        if (mBlocksAdapter != null)
            mBlocksAdapter.cleanup();
        super.onDestroy();
    }
}
