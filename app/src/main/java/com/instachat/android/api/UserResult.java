package com.instachat.android.api;

import com.google.gson.annotations.SerializedName;
import com.instachat.android.model.User;

public class UserResult {

    @SerializedName("status")
    public String status;

    @SerializedName("data")
    public User user;

    public String toString() {
        if (status.equals(NetworkApi.RESPONSE_OK)) {
            return user.toString();
        }
        return status;
    }
}
