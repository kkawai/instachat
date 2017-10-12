package com.instachat.android.app.activity.group;

import android.arch.lifecycle.ViewModelProvider;
import android.support.v7.widget.LinearLayoutManager;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.AdHelper;
import com.instachat.android.app.adapter.MessageViewHolder;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.di.ViewModelProviderFactory;
import com.instachat.android.util.rx.SchedulerProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class GroupChatActivityModule {

    @Provides
    GroupChatViewModel provideGroupChatViewModel(DataManager dataManager,
                                       SchedulerProvider schedulerProvider) {
        return new GroupChatViewModel(dataManager, schedulerProvider);
    }

    @Provides
    ViewModelProvider.Factory groupChatViewModelProvider(GroupChatViewModel groupChatViewModel) {
        return new ViewModelProviderFactory<>(groupChatViewModel);
    }

    @Provides
    MessagesRecyclerAdapter provideMessagesRecyclerAdapter(GroupChatActivity activity, FirebaseRemoteConfig firebaseRemoteConfig) {
        return new MessagesRecyclerAdapter<>(FriendlyMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                FirebaseDatabase.getInstance().getReference(activity.getDatabaseRoot()).
                        limitToLast((int) firebaseRemoteConfig.getLong(Constants.KEY_MAX_MESSAGE_HISTORY)));
    }

    @Provides
    LinearLayoutManager provideGridLayoutManager(GroupChatActivity activity) {
        return new LinearLayoutManager(activity);
    }

    @Provides
    AdHelper provideAdHelper(GroupChatActivity activity) {
        return new AdHelper(activity);
    }

}
