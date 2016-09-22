/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.instachat.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.instachat.android.util.MLog;

import org.json.JSONObject;

public class FriendlyUser implements Parcelable {

    private static final String TAG = "FriendlyUser";

    private String id; //db key
    private String name;
    private String dpid;
    private int userid;
    private String lastMessage;
    private long lastMessageTime;

    public FriendlyUser() {
    }

    public FriendlyUser(String name, int userid, String dpid, long lastMessageTime, String lastMessage) {
        this.name = name;
        this.dpid = dpid;
        this.userid = userid;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public static final Creator<FriendlyUser> CREATOR = new Creator<FriendlyUser>() {
        public FriendlyUser createFromParcel(final Parcel source) {
            return new FriendlyUser(source);
        }

        public FriendlyUser[] newArray(final int size) {
            return new FriendlyUser[size];
        }
    };

    public FriendlyUser(final Parcel parcel) {
        String s = parcel.readString();
        try {
            final JSONObject o = new JSONObject(s);
            name = o.getString("name");
            userid = o.getInt("userid");
            lastMessageTime = o.getLong("lastMessageTime");
            lastMessage = o.optString("lastMessage");
            id = o.getString("id");
            dpid = o.optString("dpid");
        } catch (final Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            if (lastMessage != null)
                o.put("lastMessage", lastMessage);
            o.put("name", name);
            o.put("lastMessageTime", lastMessageTime);
            o.put("userid", userid);
            if (dpid != null) {
                o.put("dpid", dpid);
            }
            parcel.writeString(o.toString());
        } catch (Exception e) {
            MLog.e(TAG, "", e);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getName() {
        return name;
    }

    public int getUserid() {
        return userid;
    }

    public String getDpid() {
        return dpid;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long time) {
        this.lastMessageTime = time;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    @Override
    public String toString() {
        return "lastMessage: " + lastMessage + " dpid: " + dpid + " name: " + name + " user id: " + userid + "  id: " + id;
    }
}
