/**
 * Copyright Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FriendlyMessage implements Parcelable {

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

    public static Map<String, Object> toMap(FriendlyMessage friendlyMessage) {
        Map<String, Object> map = new HashMap<>(8);
        if (friendlyMessage.getName() != null)
            map.put("name", friendlyMessage.getName());
        if (friendlyMessage.getDpid() != null)
            map.put("dpid", friendlyMessage.getDpid());
        if (friendlyMessage.getImageUrl() != null)
            map.put("imageUrl", friendlyMessage.getImageUrl());
        if (friendlyMessage.getImageId() != null)
            map.put("imageId", friendlyMessage.getImageId());
        if (friendlyMessage.getId() != null)
            map.put("id", friendlyMessage.getId());
        if (friendlyMessage.getText() != null)
            map.put("text", friendlyMessage.getText());
        if (friendlyMessage.getUserid() != 0)
            map.put("userid", friendlyMessage.getUserid());
        if (friendlyMessage.getTime() != 0)
            map.put("time", friendlyMessage.getTime());
        return map;
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

    public JSONObject toJSONObject() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        if (text != null)
            o.put("text", text);
        o.put("name", name);
        o.put("time", time);
        o.put("userid", userid);
        if (imageUrl != null)
            o.put("imageUrl", imageUrl);
        if (imageId != null) {
            o.put("imageId", imageId);
        }
        if (dpid != null) {
            o.put("dpid", dpid);
        }
        return o;
    }

    /**
     * tests if the given FriendlyMessage can be appended to this FriendlyMessage
     * <p>
     * If the current friendly message already has an image and the given FriendlyMessage
     * has an image, then we cannot append.  Otherwise, we can append.
     *
     * @param friendlyMessage
     * @return
     */
    private boolean canAppend(FriendlyMessage friendlyMessage) {
        if (imageUrl != null && friendlyMessage.imageUrl != null) {
            return false;
        }
        return true;
    }

    /**
     * Attemps to append the given FriendlyMessage to this
     * FrienlyMessage.
     *
     * @param friendlyMessage
     * @return - true if was able to append; false otherwise
     */
    public boolean append(FriendlyMessage friendlyMessage) {

        if (!canAppend(friendlyMessage)) {
            return false;
        }
        time = friendlyMessage.getTime();
        if (friendlyMessage.imageUrl != null)
            imageUrl = friendlyMessage.imageUrl;
        if (friendlyMessage.imageId != null)
            imageId = friendlyMessage.imageId;
        if (text != null && friendlyMessage.text != null) {
            text = text + "\n" + friendlyMessage.text;
        } else if (text == null && friendlyMessage.text != null) {
            text = friendlyMessage.text;
        }
        return true;
    }

    public static FriendlyMessage fromJSONObject(JSONObject o) {
        FriendlyMessage friendlyMessage = new FriendlyMessage();
        try {
            friendlyMessage.name = o.getString("name");
            friendlyMessage.userid = o.getInt("userid");
            friendlyMessage.time = o.getLong("time");
            friendlyMessage.imageUrl = o.optString("imageUrl");
            friendlyMessage.imageId = o.optString("imageId");
            friendlyMessage.text = o.optString("text");
            friendlyMessage.id = o.optString("id");
            friendlyMessage.dpid = o.optString("dpid");
        } catch (final Exception e) {
            MLog.e(TAG, "", e);
        }
        return friendlyMessage;
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
            FriendlyMessage friendlyMessage = fromJSONObject(o);
            name = friendlyMessage.name;
            userid = friendlyMessage.userid;
            time = friendlyMessage.time;
            imageUrl = friendlyMessage.imageUrl;
            imageId = friendlyMessage.imageId;
            text = friendlyMessage.text;
            id = friendlyMessage.id;
            dpid = friendlyMessage.dpid;
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
        JSONObject o = null;
        try {
            o = toJSONObject();
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

    @Override
    public String toString() {
        return "text: " + text + " dpid: " + dpid + " image id: " + imageId + " name: " + name + " user id: " + userid + "  message id: " + id;
    }
}
