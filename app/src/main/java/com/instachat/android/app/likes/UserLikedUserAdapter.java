package com.instachat.android.app.likes;

import android.app.Activity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.ActivityState;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.data.model.User;
import com.instachat.android.databinding.ItemPersonLikedUsersPostsBinding;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UserLikedUserAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<User, UserLikedUserAdapter.UserLikedUserViewHolder> {

   private static final String TAG = "UserLikedUserAdapter";

   private UserClickedListener mUserClickedListener;
   private Activity mActivity;
   private ActivityState mActivityState;
   private List<Map.Entry<DatabaseReference, ValueEventListener>> mUserInfoChangeListeners = new ArrayList<>();

   public UserLikedUserAdapter(DatabaseReference ref) {
      super(User.class, R.layout.item_person_liked_users_posts, UserLikedUserViewHolder.class, ref);
   }

   public void setUserClickedListener(UserClickedListener userClickedListener) {
      mUserClickedListener = userClickedListener;
   }

   public void setActivity(Activity activity, ActivityState activityState) {
      mActivity = activity;
      mActivityState = activityState;
   }

   @Override
   public UserLikedUserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      ItemPersonLikedUsersPostsBinding binding = ItemPersonLikedUsersPostsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
      final UserLikedUserViewHolder holder = new UserLikedUserViewHolder(binding);
      holder.itemView.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            User user = getItem(holder.getAdapterPosition());
            mUserClickedListener.onUserClicked(user.getId(), user.getUsername(), user.getProfilePicUrl(), holder.binding.userPic);
         }
      });
      return holder;
   }

   @Override
   protected void populateViewHolder(final UserLikedUserViewHolder viewHolder, User model, int position) {
      viewHolder.binding.setUser(model);
   }

   @Override
   protected User parseSnapshot(DataSnapshot snapshot) {
      User user = snapshot.getValue(User.class);
      listenForUserUpdates(user.getId());
      return user;
   }

   @Override
   public void cleanup() {
      mUserClickedListener = null;
      mActivity = null;
      mActivityState = null;
      for (Map.Entry<DatabaseReference, ValueEventListener> entry : mUserInfoChangeListeners) {
         entry.getKey().removeEventListener(entry.getValue());
      }
      mUserInfoChangeListeners = null;
      super.cleanup();
   }

   private void listenForUserUpdates(final int userid) {

      final DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.USER_INFO_REF(Integer.parseInt(userid + "")));
      final ValueEventListener eventListener = ref.addValueEventListener(new ValueEventListener() {
         @Override
         public void onDataChange(DataSnapshot dataSnapshot) {
            if (mActivityState == null || mActivityState.isActivityDestroyed())
               return;
            try {
               if (dataSnapshot.getValue() != null) {
                  User user = dataSnapshot.getValue(User.class);
                  updateUser(user);
               }
            } catch (Exception e) {
               MLog.e(TAG, "", e);
            }
         }

         @Override
         public void onCancelled(DatabaseError databaseError) {

         }
      });

      Map.Entry<DatabaseReference, ValueEventListener> entry = new Map.Entry<DatabaseReference, ValueEventListener>() {
         @Override
         public DatabaseReference getKey() {
            return ref;
         }

         @Override
         public ValueEventListener getValue() {
            return eventListener;
         }

         @Override
         public ValueEventListener setValue(ValueEventListener valueEventListener) {
            return null;
         }
      };
      mUserInfoChangeListeners.add(entry);
   }

   private void updateUser(User user) {
      Map<String, Object> map = new HashMap<>(1);
      map.put("username", user.getUsername());
      if (user.getProfilePicUrl() != null)
         map.put("profilePicUrl", user.getProfilePicUrl());
      map.put("id", user.getId());
      FirebaseDatabase.getInstance().getReference(Constants.USER_RECEIVED_LIKES_REF(UserPreferences.getInstance().getUserId())).child(user.getId() + "").updateChildren(map);
   }

   final static class UserLikedUserViewHolder extends RecyclerView.ViewHolder {
      private final ItemPersonLikedUsersPostsBinding binding;
      UserLikedUserViewHolder(ItemPersonLikedUsersPostsBinding binding) {
         super(binding.getRoot());
         this.binding = binding;
      }
   }
}