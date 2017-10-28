package com.instachat.android.data;

import com.instachat.android.data.api.APIInterface;
import com.instachat.android.data.api.BasicExistenceResult;
import com.instachat.android.data.api.BasicResponse;
import com.instachat.android.data.api.RemoteSettingsResponse;
import com.instachat.android.data.api.UserResponse;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

@Singleton
public class AppDataManager implements DataManager {

    private final APIInterface apiInterface;

    @Inject
    AppDataManager(APIInterface apiInterface) {
        this.apiInterface = apiInterface;
    }

    @Override
    public Observable<UserResponse> saveUser3(Long id, String username, String password, String email, String profilePicUrl, String bio) {
        return apiInterface.saveUser3(id,username,password,email,profilePicUrl,bio);
    }

    @Override
    public Observable<UserResponse> getUserByEmail(String email) {
        return apiInterface.getUserByEmail(email);
    }

    @Override
    public Observable<UserResponse> getUserById(int userid) {
        return apiInterface.getUserById(userid);
    }

    @Override
    public Observable<UserResponse> getUserByEmailAndPassword(String email, String password) {
        return apiInterface.getUserByEmailAndPassword(email,password);
    }

    @Override
    public Observable<BasicExistenceResult> emailExists(String email) {
        return apiInterface.emailExists(email);
    }

    @Override
    public Observable<BasicExistenceResult> userNameExists(String username) {
        return apiInterface.userNameExists(username);
    }

    @Override
    public Observable<BasicResponse> forgotPassword(String emailOrUsername) {
        return apiInterface.forgotPassword(emailOrUsername);
    }

    @Override
    public Observable<BasicResponse> registerGCM(String userid, String deviceid, String regid) {
        return apiInterface.registerGCM(userid,deviceid,regid);
    }

    @Override
    public Observable<BasicResponse> unregisterGCM(String userid, String deviceid, String regid) {
        return apiInterface.unregisterGCM(userid,deviceid,regid);
    }

    @Override
    public Observable<BasicResponse> unregisterGCM(String msg, String toid) {
        return apiInterface.unregisterGCM(msg, toid);
    }

    @Override
    public Observable<RemoteSettingsResponse> getSettings() {
        return apiInterface.getSettings();
    }
}
