package com.instachat.android.app.activity.group;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class GroupChatFragmentProvider {

    @ContributesAndroidInjector(modules = GroupChatFragmentModule.class)
    abstract GroupChatFragment provideGroupChatFragmentFactory();
}
