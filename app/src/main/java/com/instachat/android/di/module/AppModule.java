package com.instachat.android.di.module;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;
import com.instachat.android.app.activity.RemoteConfigHelper;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.activity.group.LogoutDialogHelper;
import com.instachat.android.data.AppDataManager;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.gcm.GCMHelper;
import com.instachat.android.gcm.GCMRegistrationManager;
import com.instachat.android.util.BitmapLruCache;
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

    @Provides
    @Singleton
    FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    MessagesRecyclerAdapterHelper provideMessagesRecyclerAdapterHelper() {
        return new MessagesRecyclerAdapterHelper();
    }

    @Provides
    @Singleton
    RequestQueue providesVolleyRequestQueue(Context context) {
        return Volley.newRequestQueue(context);
    }

    @Provides
    @Singleton
    ImageLoader provideVolleyImageLoader(RequestQueue requestQueue) {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int maxCacheSize = maxMemory / 8;
        return new ImageLoader(requestQueue, new BitmapLruCache(maxCacheSize));
    }

    @Provides
    @Singleton
    NetworkApi provideNetworkApi(RequestQueue requestQueue) {
        return new NetworkApi(requestQueue);
    }

    @Provides
    @Singleton
    GCMRegistrationManager provideGCMRegistrationManager(Context context, NetworkApi networkApi) {
        return new GCMRegistrationManager(context, networkApi);
    }

    @Provides
    GCMHelper provideGCMHelper(Context context, NetworkApi networkApi, GCMRegistrationManager gcmRegistrationManager) {
        return new GCMHelper(context, networkApi, gcmRegistrationManager);
    }

    @Provides
    LogoutDialogHelper provideLogoutDialogHelper() {
        return new LogoutDialogHelper();
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
