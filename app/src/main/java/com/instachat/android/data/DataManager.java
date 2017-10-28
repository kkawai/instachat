package com.instachat.android.data;

import com.instachat.android.data.api.BasicExistenceResult;
import com.instachat.android.data.api.BasicResponse;
import com.instachat.android.data.api.RemoteSettingsResponse;
import com.instachat.android.data.api.UserResponse;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.Query;

public interface DataManager {

    Observable<UserResponse> saveUser3(Long id,
                                 String username,
                                 String password,
                                 String email,
                                 String profilePicUrl,
                                 String bio);

    Observable<UserResponse> getUserByEmail(@Query("em") String email);

    Observable<UserResponse> getUserById(@Query("i") int userid);

    Observable<UserResponse> getUserByEmailAndPassword(@Query("em") String email, @Query("pd") String password);

    Observable<BasicExistenceResult> emailExists(@Query("em") String email);

    Observable<BasicExistenceResult> userNameExists(@Query("un") String username);

    Observable<BasicResponse> forgotPassword(@Field("emun") String emailOrUsername);

    Observable<BasicResponse> registerGCM(@Field("iid") String userid,
                                    @Field("deviceid") String deviceid,
                                    @Field("regid") String regid);

    Observable<BasicResponse> unregisterGCM(@Field("iid") String userid,
                                      @Field("deviceid") String deviceid,
                                      @Field("regid") String regid);

    Observable<BasicResponse> unregisterGCM(@Field("msg") String msg,
                                      @Field("toid") String toid);

    Observable<RemoteSettingsResponse> getSettings();
}
