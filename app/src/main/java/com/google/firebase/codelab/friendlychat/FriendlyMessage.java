/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.friendlychat;

import android.os.Parcel;
import android.os.Parcelable;

import com.initech.util.MLog;

import org.json.JSONObject;

public class FriendlyMessage implements Parcelable{

    private static final String TAG = "FriendlyMessage";

    private String id;
    private String text;
    private String name;
    private String dpid;
    private int userid;
    private String imageUrl;
    private String imageId;
    private long time;

    public FriendlyMessage() {
    }

    public FriendlyMessage(String text, String name, int userid, String dpid, String imageUrl, String imageId, long time) {
        this.text = text;
        this.name = name;
        this.time = time;
        this.userid = userid;
        this.imageUrl = imageUrl;
        this.imageId = imageId;
        this.dpid = dpid;
    }

    public static final Parcelable.Creator<FriendlyMessage> CREATOR = new Parcelable.Creator<FriendlyMessage>() {
        public FriendlyMessage createFromParcel(final Parcel source) {
            return new FriendlyMessage(source);
        }
        public FriendlyMessage[] newArray(final int size) {
            return new FriendlyMessage[size];
        }
    };

    public FriendlyMessage(final Parcel parcel) {
        String s = parcel.readString();
        try {
            final JSONObject o = new JSONObject(s);
            name = o.getString("name");
            userid = o.getInt("userid");
            time = o.getLong("time");
            imageUrl = o.optString("imageUrl");
            imageId = o.optString("imageId");
            text = o.optString("text");
            id = o.getString("id");
            dpid = o.optString("dpid");
        }catch(final Exception e) {
            MLog.e(TAG,"",e);
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
            o.put("id",id);
            if (text != null)
                o.put("text",text);
            o.put("name",name);
            o.put("time", time);
            o.put("userid", userid);
            if (imageUrl != null)
                o.put("imageUrl",imageUrl);
            if (imageId != null) {
                o.put("imageId",imageId);
            }
            if (dpid != null) {
                o.put("dpid",dpid);
            }
            parcel.writeString(o.toString());
        }catch (Exception e) {
            MLog.e(TAG,"",e);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getUserid() {
        return userid;
    }

    public String getDpid() {
        return dpid;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getImageId() {
        return imageId;
    }
}
