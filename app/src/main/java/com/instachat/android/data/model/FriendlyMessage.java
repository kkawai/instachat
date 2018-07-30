package com.instachat.android.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.webkit.URLUtil;

import com.google.firebase.auth.FirebaseAuth;
import com.instachat.android.Constants;
import com.instachat.android.util.MLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FriendlyMessage implements Parcelable {

    public static final int MESSAGE_TYPE_NORMAL = 0;
    public static final int MESSAGE_TYPE_ONE_TIME = 1;

    private static final String TAG = "FriendlyMessage";

    private String id;
    private String text;
    private String name;
    private String dpid;
    private int userid;
    private boolean possibleViolentImage;
    private boolean possibleAdultImage;
    private String imageUrl;
    private String imageId;
    private long time;
    private int mT;
    private long groupId;
    private String groupName;
    private int likes;
    private boolean consumedByPartner;
    private boolean isBlocked; //NOT persisted, volatile
    private boolean isPrivate; //for optimization

    public FriendlyMessage() {
    }

    public Map<String, Object> toMapForLikes() {
        Map<String, Object> map = new HashMap<>(8);
        if (getName() != null)
            map.put("username", getName());
        if (getDpid() != null)
            map.put("profilePicUrl", getDpid());
        map.put("id", getUserid());
        map.put(Constants.CHILD_LIKES, 1);
        return map;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(8);
        if (getName() != null)
            map.put("name", getName());
        if (getDpid() != null)
            map.put("dpid", getDpid());
        if (getImageUrl() != null)
            map.put("imageUrl", getImageUrl());
        if (getImageId() != null)
            map.put("imageId", getImageId());
        if (getId() != null)
            map.put("id", getId());
        if (getText() != null)
            map.put("text", getText());
        if (getUserid() != 0)
            map.put("userid", getUserid());
        if (getTime() != 0)
            map.put("time", getTime());
        if (groupId != 0) {
            map.put("groupId", groupId);
        }
        if (groupName != null) {
            map.put("groupName", groupName);
        }
        if (likes != 0) {
            map.put(Constants.CHILD_LIKES, likes);
        }
        map.put("mT", getMT());
        if (possibleAdultImage)
            map.put("possibleAdultImage", possibleAdultImage);
        if (possibleViolentImage)
            map.put("possibleViolentImage", possibleViolentImage);
        if (isPrivate)
            map.put(Constants.CHILD_MESSAGE_CONSUMED_BY_PARTNER, consumedByPartner);
        return map;
    }

    public FriendlyMessage(String text, String name, int userid, String dpid, String imageUrl,
                           boolean possibleAdultImage, boolean possibleViolentImage,
                           String imageId, long time) {
        this.text = text;
        this.name = name;
        this.time = time;
        this.userid = userid;
        this.imageUrl = imageUrl;
        this.imageId = imageId;
        this.dpid = dpid;
        this.possibleAdultImage = possibleAdultImage;
        this.possibleViolentImage = possibleViolentImage;
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
        if (groupId != 0)
            o.put("groupId", groupId);
        if (groupName != null)
            o.put("groupName", groupName);
        if (likes != 0)
            o.put(Constants.CHILD_LIKES, likes);
        o.put("mT", mT);
        if (possibleAdultImage)
            o.put("possibleAdultImage", possibleAdultImage);
        if (possibleViolentImage)
            o.put("possibleViolentImage", possibleViolentImage);
        if (consumedByPartner)
            o.put(Constants.CHILD_MESSAGE_CONSUMED_BY_PARTNER, consumedByPartner);
        FirebaseAuth.getInstance().getUid();
        return o;
    }

    /**
     * Only create lightweight json object for the purpose of
     * notification messaging.  Cannot send payloads of more than
     * a few K over gcm!
     *
     * @return
     * @throws JSONException
     */
    public JSONObject toLightweightJSONObject() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        if (!TextUtils.isEmpty(text))
            o.put("text", shorten(text, 64));
        o.put("name", name);
        o.put("userid", userid);
        if (dpid != null) {
            o.put("dpid", dpid);
        }
        o.put("mT", mT);
        return o;
    }

    private String shorten(final String s, final int limit) {
        if (s.length() <= limit) {
            return s;
        }
        return s.substring(0, limit) + "...";
    }

    /**
     * tests if the given FriendlyMessage can be appended to this FriendlyMessage
     * <p/>
     * If the current friendly message already has an image and the given FriendlyMessage
     * has an image, then we cannot append.  Otherwise, we can append.
     *
     * @param friendlyMessage
     * @return
     */
    private boolean canAppend(FriendlyMessage friendlyMessage) {

        if (System.currentTimeMillis() - getTime() > 1000 * 60 * 5L) {
            return false;
        }

        if (mT != MESSAGE_TYPE_NORMAL || friendlyMessage.mT != MESSAGE_TYPE_NORMAL)
            return false;
        if (this.imageUrl != null && friendlyMessage.imageUrl != null)
            return false;
        if (isPureUrl(this.text) || isPureUrl(friendlyMessage.getText()))
            return false;
        return true;
    }

    private boolean isPureUrl(String text) {
        return text != null && (URLUtil.isHttpUrl(text) || URLUtil.isHttpsUrl(text));
    }

    /**
     * Attempts to append the given FriendlyMessage to this
     * FriendlyMessage.
     *
     * @param friendlyMessage
     * @return - true if was able to append; false otherwise
     */
    public boolean append(FriendlyMessage friendlyMessage) {

        if (!canAppend(friendlyMessage)) {
            return false;
        }
        if (!TextUtils.isEmpty(friendlyMessage.getDpid()))
            dpid = friendlyMessage.getDpid();
        name = friendlyMessage.getName();
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
        if (possibleAdultImage || friendlyMessage.possibleAdultImage)
            possibleAdultImage = true;
        if (possibleViolentImage || friendlyMessage.possibleViolentImage)
            possibleViolentImage = true;
        consumedByPartner = false;
        return true;
    }

    public static FriendlyMessage fromJSONObject(JSONObject o) {
        FriendlyMessage friendlyMessage = new FriendlyMessage();
        try {
            friendlyMessage.name = o.getString("name");
            friendlyMessage.userid = o.getInt("userid");
            friendlyMessage.time = o.optLong("time");
            friendlyMessage.imageUrl = o.optString("imageUrl");
            friendlyMessage.imageId = o.optString("imageId");
            friendlyMessage.text = o.optString("text");
            friendlyMessage.id = o.optString("id");
            friendlyMessage.dpid = o.optString("dpid");
            friendlyMessage.mT = o.optInt("mT");
            friendlyMessage.groupName = o.optString("groupName");
            friendlyMessage.groupId = o.optLong("groupId");
            friendlyMessage.possibleAdultImage = o.optBoolean("possibleAdultImage");
            friendlyMessage.possibleViolentImage = o.optBoolean("possibleViolentImage");
            friendlyMessage.likes = o.optInt(Constants.CHILD_LIKES);
            friendlyMessage.consumedByPartner = o.optBoolean(Constants.CHILD_MESSAGE_CONSUMED_BY_PARTNER, false);
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
            groupName = friendlyMessage.groupName;
            groupId = friendlyMessage.groupId;
            mT = friendlyMessage.mT;
            possibleAdultImage = friendlyMessage.possibleAdultImage;
            possibleViolentImage = friendlyMessage.possibleViolentImage;
            consumedByPartner = friendlyMessage.consumedByPartner;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getUserid() {
        return userid;
    }

    public String getDpid() {
        return dpid;
    }

    public void setDpid(String dpid) {
        this.dpid = dpid;
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

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public int getMT() {
        return mT;
    }

    public void setMT(int mT) {
        this.mT = mT;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public boolean isPossibleViolentImage() {
        return possibleViolentImage;
    }

    public boolean isPossibleAdultImage() {
        return possibleAdultImage;
    }

    public boolean isConsumedByPartner() {
        return consumedByPartner;
    }

    public void setConsumedByPartner(boolean consumedByPartner) {
        this.consumedByPartner = consumedByPartner;
    }

    @Override
    public String toString() {
        return "text: " + text + " dpid: " + dpid + " image id: " + imageId + " name: " + name + " user id: " + userid + "  message id: " + id + " imageUrl: " + imageUrl;
    }

    @Override
    public boolean equals(Object obj) {
        FriendlyMessage other = (FriendlyMessage) obj;
        return this.getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
