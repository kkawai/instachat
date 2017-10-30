package com.instachat.android.app.activity.group;

import android.arch.lifecycle.ViewModelProvider;
import android.support.v7.widget.LinearLayoutManager;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.app.activity.AdHelper;
import com.instachat.android.app.activity.PresenceHelper;
import com.instachat.android.app.activity.RemoteConfigHelper;
import com.instachat.android.app.adapter.MessageViewHolder;
import com.instachat.android.app.adapter.MessagesRecyclerAdapter;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.model.FriendlyMessage;
import com.instachat.android.di.ViewModelProviderFactory;
import com.instachat.android.util.rx.SchedulerProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class GroupChatModule {

    @Provides
    GroupChatViewModel provideGroupChatViewModel(DataManager dataManager,
                                                 SchedulerProvider schedulerProvider,
                                                 FirebaseRemoteConfig firebaseRemoteConfig,
                                                 FirebaseDatabase firebaseDatabase) {
        return new GroupChatViewModel(dataManager, schedulerProvider, firebaseRemoteConfig, firebaseDatabase);
    }

    @Provides
    ViewModelProvider.Factory groupChatViewModelProvider(GroupChatViewModel groupChatViewModel) {
        return new ViewModelProviderFactory<>(groupChatViewModel);
    }

    @Provides
    LinearLayoutManager provideGridLayoutManager(GroupChatActivity activity) {
        return new LinearLayoutManager(activity);
    }

    @Provides
    PresenceHelper providePresenceHelper() {
        return new PresenceHelper();
    }
}