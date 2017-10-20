package com.instachat.android.app.activity.pm;

import com.instachat.android.app.ui.base.BaseViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.util.rx.SchedulerProvider;

public class PrivateChatViewModel extends BaseViewModel<PrivateChatNavigator> {

    public PrivateChatViewModel(DataManager dataManager, SchedulerProvider schedulerProvider) {
        super(dataManager, schedulerProvider);
    }

}
