package com.instachat.android.di.module;

import android.app.Application;
import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;
import com.instachat.android.app.activity.AdsHelper;
import com.instachat.android.app.activity.group.DeleteAccountDialogHelper;
import com.instachat.android.app.bans.BanHelper;
import com.instachat.android.app.activity.PresenceHelper;
import com.instachat.android.app.activity.RemoteConfigHelper;
import com.instachat.android.app.activity.group.LogoutDialogHelper;
import com.instachat.android.app.adapter.ChatSummariesRecyclerAdapter;
import com.instachat.android.app.adapter.MessagesRecyclerAdapterHelper;
import com.instachat.android.app.adapter.UserPresenceManager;
import com.instachat.android.data.AppDataManager;
import com.instachat.android.data.DataManager;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.api.NetworkModule;
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
    FirebaseDatabase provideFirebaseDatabase() {
        return FirebaseDatabase.getInstance();
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
    GCMRegistrationManager provideGCMRegistrationManager(Context context,
                                                         DataManager dataManager,
                                                         SchedulerProvider schedulerProvider
                                                         ) {
        return new GCMRegistrationManager(context, dataManager, schedulerProvider);
    }

    @Provides
    GCMHelper provideGCMHelper(Context context, NetworkApi networkApi, GCMRegistrationManager gcmRegistrationManager) {
        return new GCMHelper(context, networkApi, gcmRegistrationManager);
    }

    @Provides
    LogoutDialogHelper provideLogoutDialogHelper() {
        return new LogoutDialogHelper();
    }

    @Provides
    DeleteAccountDialogHelper provideDeleteAccountDialogHelper() {
        return new DeleteAccountDialogHelper();
    }

    @Provides
    UserPresenceManager provideUserPresenceManager(NetworkApi networkApi) {
        return new UserPresenceManager(networkApi);
    }

    @Provides
    PresenceHelper providePresenceHelper(FirebaseDatabase firebaseDatabase) {
        return new PresenceHelper(firebaseDatabase);
    }

    @Provides
    ChatSummariesRecyclerAdapter provideChatSummariesRecyclerAdapter(UserPresenceManager userPresenceManager) {
        return new ChatSummariesRecyclerAdapter(userPresenceManager);
    }

    @Provides
    AdsHelper provideAdHelper() {
        return new AdsHelper();
    }

    @Provides
    @Singleton
    BanHelper provideBanHelper(FirebaseDatabase firebaseDatabase) {
        return new BanHelper(firebaseDatabase);
    }

    @Provides
    @Singleton
    NetworkModule provideNetworkModule(NetworkModule networkModule) {
        return networkModule;
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
