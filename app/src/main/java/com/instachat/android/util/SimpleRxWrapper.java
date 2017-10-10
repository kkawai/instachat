package com.instachat.android.util;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SimpleRxWrapper {

    public static void executeInWorkerThread(final Runnable task) {

        Observable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                task.run();
                return null;
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    public static void executeInUiThread(final Runnable task) {
        Observable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                task.run();
                return null;
            }
        }).subscribeOn(AndroidSchedulers.mainThread()).subscribe();
    }

}
