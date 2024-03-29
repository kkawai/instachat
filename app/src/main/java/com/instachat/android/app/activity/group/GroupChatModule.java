package com.instachat.android.app.activity.group;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.app.bans.BanHelper;
import com.instachat.android.data.DataManager;
import com.instachat.android.di.ViewModelProviderFactory;
import com.instachat.android.util.rx.SchedulerProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class GroupChatModule {

    @Provides
    GroupChatViewModel provideGroupChatViewModel(DataManager dataManager,
                                                 SchedulerProvider schedulerProvider,
                                                 FirebaseRemoteConfig firebaseRemoteConfig,
                                                 FirebaseDatabase firebaseDatabase,
                                                 BanHelper banHelper) {
        return new GroupChatViewModel(dataManager, schedulerProvider, firebaseRemoteConfig, firebaseDatabase, banHelper);
    }

    @Provides
    ViewModelProvider.Factory groupChatViewModelProvider(GroupChatViewModel groupChatViewModel) {
        return new ViewModelProviderFactory<>(groupChatViewModel);
    }

    @Provides
    LinearLayoutManager provideGridLayoutManager(GroupChatActivity activity) {
        return new LinearLayoutManager(activity);
    }

}
