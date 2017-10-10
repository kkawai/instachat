package com.instachat.android.app.likes;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.instachat.android.app.activity.ActivityState;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.adapter.UserClickedListener;
import com.instachat.android.data.model.User;
import com.instachat.android.util.MLog;
import com.instachat.android.util.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UserLikedUserAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<User, UserLikedUserViewHolder> {

   private static final String TAG = "UserLikedUserAdapter";

   private UserClickedListener mUserClickedListener;
   private Activity mActivity;
   private ActivityState mActivityState;
   private List<Map.Entry<DatabaseReference, ValueEventListener>> mUserInfoChangeListeners = new ArrayList<>();

   public UserLikedUserAdapter(Class<User> modelClass, int modelLayout, Class<UserLikedUserViewHolder> viewHolderClass, DatabaseReference ref) {
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
   public UserLikedUserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      final UserLikedUserViewHolder holder = super.onCreateViewHolder(parent, viewType);
      holder.itemView.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            User user = getItem(holder.getAdapterPosition());
            mUserClickedListener.onUserClicked(user.getId(), user.getUsername(), user.getProfilePicUrl(), holder.userPic);
         }
      });
      return holder;
   }

   @Override
   protected void populateViewHolder(final UserLikedUserViewHolder viewHolder, User model, int position) {
      viewHolder.username.setText(model.getUsername());
      if (model.getLikes() <= 1)
         viewHolder.likedPersonsPosts.setText(mActivity.getString(R.string.gave_like_singular));
      else
         viewHolder.likedPersonsPosts.setText(mActivity.getString(R.string.gave_likes_plural, model.getLikes() + ""));
      if (TextUtils.isEmpty(model.getProfilePicUrl())) {
         viewHolder.userPic.setImageResource(R.drawable.ic_anon_person_36dp);
         return;
      }
      try {
         Glide.with(mActivity).
               load(model.getProfilePicUrl()).
               error(R.drawable.ic_anon_person_36dp).
               into(viewHolder.userPic);
      } catch (final Exception e) {
         MLog.e(TAG, "", e);
         viewHolder.userPic.setImageResource(R.drawable.ic_anon_person_36dp);
      }
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
      FirebaseDatabase.getInstance().getReference(Constants.USER_RECEIVED_LIKES_REF(Preferences.getInstance().getUserId())).child(user.getId() + "").updateChildren(map);
   }
}