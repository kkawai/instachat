package com.instachat.android.data.api;

import com.google.gson.annotations.SerializedName;
import com.instachat.android.data.model.User;

public class UserResponse {

    @SerializedName("status")
    public String status="";

    @SerializedName("data")
    public User user;

    public String toString() {
        if (status.equals(NetworkApi.RESPONSE_OK)) {
            return user.toString();
        }
        return status;
    }
}
