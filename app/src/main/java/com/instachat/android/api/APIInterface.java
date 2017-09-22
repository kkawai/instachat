package com.instachat.android.api;

import com.instachat.android.model.User;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface APIInterface {

    @FormUrlEncoded
    @POST("/ih/saveuser3")
    Call<UserResult> saveUser3(@Field("id") String id,
                               @Field("username") String username,
                               @Field("password") String password,
                               @Field("email") String email,
                               @Field("profilePicUrl") String profilePicUrl,
                               @Field("bio") String bio);

    @GET("/ih/getubyem")
    Call<User> getUserByEmail(@Query("em") String email);

    @GET("/ih/getubid")
    Call<User> getUserById(@Query("i") int userid);

    @GET("/ih/getu")
    Call<User> getUserByEmailAndPassword(@Query("em") String email, @Query("pd") String password);

    @GET("/ih/exists")
    Call<BasicExistenceResult> emailExists(@Query("em") String email);

    @GET("/ih/exists")
    Call<BasicExistenceResult> userNameExists(@Query("un") String username);

    @FormUrlEncoded
    @POST("/ih/fgp")
    Call<BasicResult> forgotPassword(@Field("emun") String emailOrUsername);

    @FormUrlEncoded
    @POST("/ih/gcmreg")
    Call<BasicResult> registerGCM(@Field("iid") String userid,
                                  @Field("deviceid") String deviceid,
                                  @Field("regid") String regid);

    @FormUrlEncoded
    @POST("/ih/gcmunreg")
    Call<BasicResult> unregisterGCM(@Field("iid") String userid,
                                    @Field("deviceid") String deviceid,
                                    @Field("regid") String regid);

    @FormUrlEncoded
    @POST("/ih/gcmsend")
    Call<BasicResult> unregisterGCM(@Field("msg") String msg,
                                    @Field("toid") String toid);

    @GET("/ih/settings")
    Call<RemoteSettingsResult> getSettings();
}