package com.instachat.android.data;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

@Singleton
public class AppDataManager implements DataManager {

    @Inject
    AppDataManager() {
    }

//    @Override
//    public Observable<HomeResponse> getHomeData() {
//        return homeHelper.getHomeData();
//    }
}
