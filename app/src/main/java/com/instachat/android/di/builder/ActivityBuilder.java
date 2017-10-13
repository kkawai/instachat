package com.instachat.android.di.builder;

import com.instachat.android.app.activity.group.GroupChatActivity;
import com.instachat.android.app.activity.group.GroupChatActivityModule;
import com.instachat.android.app.login.SignInActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ActivityBuilder {

    @ContributesAndroidInjector(modules = { GroupChatActivityModule.class})
    abstract GroupChatActivity bindGroupChatActivity();

}
