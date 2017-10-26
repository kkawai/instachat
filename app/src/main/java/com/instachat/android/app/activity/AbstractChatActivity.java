package com.instachat.android.app.activity;

import android.databinding.ViewDataBinding;

import com.instachat.android.app.ui.base.BaseActivity;
import com.instachat.android.app.ui.base.BaseViewModel;

public abstract class AbstractChatActivity<T extends ViewDataBinding, V extends BaseViewModel> extends BaseActivity<T, V> {
}
