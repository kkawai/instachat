package com.instachat.android.app.bans;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.instachat.android.R;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.databinding.ItemBannedPersonBinding;
import com.instachat.android.util.MLog;

import androidx.recyclerview.widget.RecyclerView;

public final class BannedUsersAdapter<T, VH extends RecyclerView.ViewHolder>
        extends FirebaseRecyclerAdapter<BannedUser, BannedUsersAdapter.BannedUserViewHolder>
        implements Filterable {

    private static final String TAG = "BlocksAdapter";

    private UserClickedListener mUserClickedListener;

    public BannedUsersAdapter(Class<BannedUser> modelClass, Query ref) {
        super(modelClass, R.layout.item_banned_person, BannedUserViewHolder.class, ref);
    }

    public void setUserClickedListener(UserClickedListener userClickedListener) {
        mUserClickedListener = userClickedListener;
    }

    @Override
    public BannedUserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemBannedPersonBinding binding = ItemBannedPersonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        final BannedUserViewHolder holder = new BannedUserViewHolder(binding);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BannedUser bannedUser = getItem(holder.getAdapterPosition());
                mUserClickedListener.onUserClicked(bannedUser.id, bannedUser.username, bannedUser.dpid, holder.binding.userPic);
            }
        });
        return holder;
    }

    @Override
    protected void populateViewHolder(final BannedUserViewHolder viewHolder, BannedUser model, int position) {
        viewHolder.binding.setBannedUser(model);
    }

    @Override
    protected BannedUser parseSnapshot(DataSnapshot snapshot) {
        MLog.d(TAG, "BannedUser: " + snapshot);
        BannedUser bannedUser = snapshot.getValue(BannedUser.class);
        //bannedUser.id = Integer.parseInt(snapshot.getKey());
        return bannedUser;
    }

    @Override
    public void cleanup() {
        mUserClickedListener = null;
        super.cleanup();
    }

    static final class BannedUserViewHolder extends RecyclerView.ViewHolder {

        private final ItemBannedPersonBinding binding;

        public BannedUserViewHolder(ItemBannedPersonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @Override
    public Filter getFilter() {
        return myFilter;
    }

    private Filter myFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

        }
    };
}