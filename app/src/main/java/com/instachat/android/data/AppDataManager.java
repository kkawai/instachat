package com.instachat.android.data;

import com.instachat.android.data.api.APIInterface;
import com.instachat.android.data.api.BasicExistenceResult;
import com.instachat.android.data.api.BasicResponse;
import com.instachat.android.data.api.RemoteSettingsResponse;
import com.instachat.android.data.api.UserResponse;
import com.instachat.android.util.rx.SchedulerProvider;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

@Singleton
public class AppDataManager implements DataManager {

    private final APIInterface apiInterface;
    private final SchedulerProvider schedulerProvider;

    @Inject
    AppDataManager(APIInterface apiInterface, SchedulerProvider schedulerProvider) {
        this.apiInterface = apiInterface;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<BasicResponse> deleteAccount(Long id) {
        return apiInterface.deleteAccount(id)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<UserResponse> saveUser3(Long id, String username, String password, String email, String profilePicUrl, String bio) {
        return apiInterface.saveUser3(id, username, password, email, profilePicUrl, bio)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<UserResponse> getUserByEmail(String email) {
        return apiInterface.getUserByEmail(email)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<UserResponse> getUserById(int userid) {
        return apiInterface.getUserById(userid)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<UserResponse> getUserByEmailAndPassword(String email, String password) {
        return apiInterface.getUserByEmailAndPassword(email, password)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<BasicExistenceResult> emailExists(String email) {
        return apiInterface.emailExists(email)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<BasicExistenceResult> userNameExists(String username) {
        return apiInterface.userNameExists(username)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<BasicResponse> forgotPassword(String emailOrUsername) {
        return apiInterface.forgotPassword(emailOrUsername)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());

    }

    @Override
    public Observable<BasicResponse> registerGCM(String userid, String deviceid, String regid) {
        return apiInterface.registerGCM(userid, deviceid, regid)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<BasicResponse> unregisterGCM(String userid, String deviceid, String regid) {
        return apiInterface.unregisterGCM(userid, deviceid, regid)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<BasicResponse> unregisterGCM(String msg, String toid) {
        return apiInterface.unregisterGCM(msg, toid)
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }

    @Override
    public Observable<RemoteSettingsResponse> getSettings() {
        return apiInterface.getSettings()
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.io());
    }
}
