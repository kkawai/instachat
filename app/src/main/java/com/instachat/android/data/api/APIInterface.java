package com.instachat.android.data.api;

import com.instachat.android.Constants;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface APIInterface {

    @FormUrlEncoded
    @POST("/ih/delusr")
    Observable<BasicResponse> deleteAccount(@Field("id") Long id);

    @FormUrlEncoded
    @POST("/ih/saveuser3")
    Observable<UserResponse> saveUser3(@Field("id") Long id,
                                 @Field("username") String username,
                                 @Field("password") String password,
                                 @Field("email") String email,
                                 @Field("profilePicUrl") String profilePicUrl,
                                 @Field("bio") String bio);

    @GET("/ih/getubyem")
    Observable<UserResponse> getUserByEmail(@Query("em") String email);

    @GET("/ih/getubid")
    Observable<UserResponse> getUserById(@Query("i") int userid);

    @GET("/ih/getu")
    Observable<UserResponse> getUserByEmailAndPassword(@Query("em") String email, @Query("pd") String password);

    @GET("/ih/exists")
    Observable<BasicExistenceResult> emailExists(@Query("em") String email);

    @GET("/ih/exists")
    Observable<BasicExistenceResult> userNameExists(@Query("un") String username);

    @FormUrlEncoded
    @POST("/ih/fgp")
    Observable<BasicResponse> forgotPassword(@Field("emun") String emailOrUsername);

    @FormUrlEncoded
    @POST("/ih/gcmreg")
    Observable<BasicResponse> registerGCM(@Field("iid") String userid,
                                    @Field("deviceid") String deviceid,
                                    @Field("regid") String regid);

    @FormUrlEncoded
    @POST("/ih/gcmunreg")
    Observable<BasicResponse> unregisterGCM(@Field("iid") String userid,
                                      @Field("deviceid") String deviceid,
                                      @Field("regid") String regid);

    @FormUrlEncoded
    @POST("/ih/gcmsend")
    Observable<BasicResponse> unregisterGCM(@Field("msg") String msg,
                                      @Field("toid") String toid);

    @GET("/ih/settings")
    Observable<RemoteSettingsResponse> getSettings();

    @FormUrlEncoded
    @POST("/ih/gcmsend")
    Observable<BasicResponse> gcmSend(@Field(Constants.KEY_GCM_MSG_TYPE) String gcmMsgType,
                                       @Field(Constants.KEY_TO_USERID) int toid,
                                       @Field(Constants.KEY_GCM_MESSAGE) String message);

    @FormUrlEncoded
    @POST("/ih/gcmreg")
    Observable<BasicResponse> gcmReg(@Field("iid") String userid,
                                      @Field("deviceid") String androidId,
                                      @Field("regid") String regid);

    @FormUrlEncoded
    @POST("/ih/gcmunreg")
    Observable<BasicResponse> gcmUnreg(@Field("iid") String userid,
                                     @Field("deviceid") String androidId,
                                     @Field("regid") String regid);
}