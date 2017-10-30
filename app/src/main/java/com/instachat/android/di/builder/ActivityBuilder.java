package com.instachat.android.di.builder;

import com.instachat.android.app.activity.group.GroupChatActivity;
import com.instachat.android.app.activity.group.GroupChatModule;
import com.instachat.android.app.activity.pm.PrivateChatActivity;
import com.instachat.android.app.activity.pm.PrivateChatModule;
import com.instachat.android.app.login.SignInActivity;
import com.instachat.android.app.login.SignInModule;
import com.instachat.android.app.login.signup.SignUpActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ActivityBuilder {

    @ContributesAndroidInjector(modules = { GroupChatModule.class})
    abstract GroupChatActivity bindGroupChatActivity();

    @ContributesAndroidInjector(modules = { SignInModule.class})
    abstract SignInActivity bindSignInActivity();

    @ContributesAndroidInjector
    abstract SignUpActivity bindSignUpActivity();

    @ContributesAndroidInjector(modules = { PrivateChatModule.class })
    abstract PrivateChatActivity bindPrivateChatActivity();

}