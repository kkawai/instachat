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
public class GroupChatFragmentModule {

    @Provides
    GroupChatFragmentViewModel provideGroupChatViewModel(DataManager dataManager,
                                                 SchedulerProvider schedulerProvider,
                                                 FirebaseRemoteConfig firebaseRemoteConfig,
                                                 FirebaseDatabase firebaseDatabase,
                                                 BanHelper banHelper) {
        return new GroupChatFragmentViewModel(dataManager, schedulerProvider, firebaseRemoteConfig, firebaseDatabase, banHelper);
    }

    @Provides
    ViewModelProvider.Factory groupChatViewModelProvider(GroupChatFragmentViewModel groupChatFragmentViewModel) {
        return new ViewModelProviderFactory<>(groupChatFragmentViewModel);
    }

    @Provides
    LinearLayoutManager provideGridLayoutManager(GroupChatFragment groupChatFragment) {
        return new LinearLayoutManager(groupChatFragment.getActivity());
    }

}
