package com.instachat.android.amazon;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ADMReceiverModule {

    @Provides
    @Singleton
    ADMReceiver provideADMReceiver() {
        return new ADMReceiver();
    }
}
