package com.instachat.android.di.component;


import android.app.Application;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.instachat.android.TheApp;
import com.instachat.android.app.login.recovery.ForgotPasswordActivity;
import com.instachat.android.data.api.NetworkApi;
import com.instachat.android.data.api.NetworkModule;
import com.instachat.android.di.builder.ActivityBuilder;
import com.instachat.android.di.module.AppModule;
import com.instachat.android.view.MyNetworkImageView;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;

@Singleton
@Component(modules = {AndroidInjectionModule.class, AppModule.class, ActivityBuilder.class, NetworkModule.class})
public interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder application(Application application);

        AppComponent build();

    }

    void inject(TheApp app);
    void inject(MyNetworkImageView networkImageView);
    void inject(ForgotPasswordActivity forgotPasswordActivity);
}
