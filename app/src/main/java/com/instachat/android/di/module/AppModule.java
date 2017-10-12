package com.instachat.android.di.module;

import android.app.Application;
import android.content.Context;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;
import com.instachat.android.app.activity.RemoteConfigHelper;
import com.instachat.android.data.AppDataManager;
import com.instachat.android.data.DataManager;
import com.instachat.android.util.rx.AppSchedulerProvider;
import com.instachat.android.util.rx.SchedulerProvider;


import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    @Provides
    @Singleton
    Context provideContext(Application application) {
        return application;
    }

    @Provides
    @Singleton
    SchedulerProvider provideSchedulerProvider() {
        return new AppSchedulerProvider();
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    DataManager provideDataManager(AppDataManager appDataManager) {
        return appDataManager;
    }

    @Provides
    @Singleton
    FirebaseRemoteConfig provideFirebaseRemoteConfig() {
        return new RemoteConfigHelper().initializeRemoteConfig();
    }

//    @Provides
//    @Singleton
//    HomeHelper provideHomeHelper(LocalHomeManager localHomeManager) {
//        return localHomeManager;
//    }
//
//    @Provides
//    @Singleton
//    NetworkModule provideNetworkModule(NetworkModule networkModule) {
//        return networkModule;
//    }
}
