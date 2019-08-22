package com.instachat.android.app.login;

import com.instachat.android.data.DataManager;
import com.instachat.android.di.ViewModelProviderFactory;
import com.instachat.android.util.rx.SchedulerProvider;

import androidx.lifecycle.ViewModelProvider;
import dagger.Module;
import dagger.Provides;

@Module
public class VerifyPhoneModule {

    @Provides
    VerifyPhoneViewModel provideSignInViewModel(DataManager dataManager,
                                           SchedulerProvider schedulerProvider) {
        return new VerifyPhoneViewModel(dataManager, schedulerProvider);
    }

    @Provides
    ViewModelProvider.Factory verifyPhoneModelProvider(VerifyPhoneViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }

}
