package com.instachat.android.data.api;

import com.google.gson.annotations.SerializedName;

public class BasicExistenceResult {

    public static class Data {
        @SerializedName("username")
        public String username;

        @SerializedName("email")
        public String email;

        @SerializedName("exists")
        public boolean exists;
    }

    @SerializedName("status")
    public String status;

    @SerializedName("data")
    public Data data;


}
