package com.instachat.android.di.component;


import android.app.Application;

import com.instachat.android.TheApp;
import com.instachat.android.di.builder.ActivityBuilder;
import com.instachat.android.di.module.AppModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;

//todo: add more modules as activities are added
@Singleton
@Component(modules = {AndroidInjectionModule.class, AppModule.class, ActivityBuilder.class})
public interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder application(Application application);

        AppComponent build();

    }

    void inject(TheApp app);

}
