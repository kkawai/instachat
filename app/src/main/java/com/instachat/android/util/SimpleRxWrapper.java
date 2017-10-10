package com.instachat.android.util;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SimpleRxWrapper {

    public static void executeInWorkerThread(final Runnable task) {

        Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                task.run();
                return 0;
            }
        }).subscribeOn(Schedulers.io()).subscribe();

    }

    public static void executeInUiThread(final Runnable task) {
        Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                task.run();
                return 0;
            }
        }).subscribeOn(AndroidSchedulers.mainThread()).subscribe();
    }

}
