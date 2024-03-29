package com.instachat.android.app.activity.pm;

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
public class PrivateChatModule {

    @Provides
    PrivateChatViewModel provideGroupChatViewModel(DataManager dataManager,
                                                   SchedulerProvider schedulerProvider,
                                                   FirebaseRemoteConfig firebaseRemoteConfig,
                                                   FirebaseDatabase firebaseDatabase,
                                                   BanHelper banHelper) {
        return new PrivateChatViewModel(dataManager, schedulerProvider, firebaseRemoteConfig, firebaseDatabase, banHelper);
    }

    @Provides
    ViewModelProvider.Factory privateChatViewModelProvider(PrivateChatViewModel privateChatViewModel) {
        return new ViewModelProviderFactory<>(privateChatViewModel);
    }

    @Provides
    LinearLayoutManager provideGridLayoutManager(PrivateChatActivity activity) {
        return new LinearLayoutManager(activity);
    }
}
