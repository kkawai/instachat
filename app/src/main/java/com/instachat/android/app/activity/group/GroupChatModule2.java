package com.instachat.android.app.activity.group;

import android.arch.lifecycle.ViewModelProvider;
import android.support.v7.widget.LinearLayoutManager;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.app.bans.BanHelper;
import com.instachat.android.data.DataManager;
import com.instachat.android.di.ViewModelProviderFactory;
import com.instachat.android.util.rx.SchedulerProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class GroupChatModule2 {

    @Provides
    GroupChatViewModel2 provideGroupChatViewModel(DataManager dataManager,
                                                 SchedulerProvider schedulerProvider,
                                                 FirebaseRemoteConfig firebaseRemoteConfig,
                                                 FirebaseDatabase firebaseDatabase,
                                                 BanHelper banHelper) {
        return new GroupChatViewModel2(dataManager, schedulerProvider, firebaseRemoteConfig, firebaseDatabase, banHelper);
    }

    @Provides
    ViewModelProvider.Factory groupChatViewModelProvider(GroupChatViewModel2 groupChatViewModel) {
        return new ViewModelProviderFactory<>(groupChatViewModel);
    }

    @Provides
    LinearLayoutManager provideGridLayoutManager(GroupChatActivity2 activity) {
        return new LinearLayoutManager(activity);
    }

}
