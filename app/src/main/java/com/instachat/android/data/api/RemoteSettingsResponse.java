package com.instachat.android.data.api;

import com.google.gson.annotations.SerializedName;

public class RemoteSettingsResponse {

    @SerializedName("status")
    public String status="";

    @SerializedName("data")
    public Setting data;

    public static class Setting {
        @SerializedName("a")
        public String a;

        @SerializedName("s")
        public String s;
    }
}
