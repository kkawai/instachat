package com.instachat.android.app.activity.pm;

import com.instachat.android.app.activity.AbstractChatViewModel;
import com.instachat.android.app.ui.base.BaseViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.util.rx.SchedulerProvider;

public class PrivateChatViewModel extends AbstractChatViewModel<PrivateChatNavigator> {

    public PrivateChatViewModel(DataManager dataManager, SchedulerProvider schedulerProvider) {
        super(dataManager, schedulerProvider);
    }

    @Override
    public boolean isPrivateChat() {
        return true;
    }

    @Override
    public void cleanup() {

    }
}
