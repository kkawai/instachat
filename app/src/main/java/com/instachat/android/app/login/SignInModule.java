package com.instachat.android.app.login;

import androidx.lifecycle.ViewModelProvider;

import com.instachat.android.data.DataManager;
import com.instachat.android.di.ViewModelProviderFactory;
import com.instachat.android.util.rx.SchedulerProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class SignInModule {

    @Provides
    SignInViewModel provideSignInViewModel(DataManager dataManager,
                                           SchedulerProvider schedulerProvider) {
        return new SignInViewModel(dataManager, schedulerProvider);
    }

    @Provides
    ViewModelProvider.Factory signInViewModelProvider(SignInViewModel signInViewModel) {
        return new ViewModelProviderFactory<>(signInViewModel);
    }

}
