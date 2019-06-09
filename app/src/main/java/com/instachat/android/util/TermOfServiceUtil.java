package com.instachat.android.util;

import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.instachat.android.TheApp;
import com.instachat.android.app.activity.group.GroupChatNavigator;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public final class TermOfServiceUtil {

    private static final long ONE_DAY = 1000 * 60 * 60 * 24L;

    private TermOfServiceUtil() {
    }

    public static Disposable getTermsOfServiceDisposable(final GroupChatNavigator groupChatNavigator) {
        return Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TheApp.getInstance());
                long last = prefs.getLong("lspt", 0L);
                if (System.currentTimeMillis() - last > ONE_DAY) {
                    prefs.edit().putLong("lspt", System.currentTimeMillis()).apply();
                    return true;
                }
                return false;
            }
        }).delay(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean doShow) {
                        if (doShow) {
                            groupChatNavigator.showTermsOfService();
                        }
                    }
                });
    }

}