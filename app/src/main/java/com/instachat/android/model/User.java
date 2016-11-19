package com.instachat.android.model;

import android.content.ContentValues;

import com.google.firebase.database.ServerValue;
import com.instachat.android.Constants;
import com.instachat.android.model.UserManager.UserColumns;
import com.instachat.android.util.MLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class User {

    private static final long serialVersionUID = 3121394108242466120L;

    private int id;
    private String instagramId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String location;
    private String website;
    private String profilePicUrl;
    private boolean isFollowing;
    private boolean isFollower;
    private boolean isBlocked;
    private boolean isStarred;
    private int age = 18;
    private String gender;
    private String bio;
    private int showlocation = 1;
    private String privileges;
    private int remainingvc;
    private String fbid;
    private String fbaccesstoken;
    private String tid;
    private String taccesstoken;
    private String ttokensecret;
    private String iaccesstoken;
    private int unread;
    private Timestamp tstamp = new Timestamp(new Date().getTime());
    private boolean isOnline;
    private transient boolean transientAddedToRoster;
    private transient ArrayList<StatusChangeListener> statusChangeListeners;
    private long lastOnline;
    private String currentGroupName;
    private long currentGroupId;
    private int likes;

    private int mediaCounts;
    private int followsCount;
    private int followersCount;
    private boolean isInstalled;
    private double distance; //for popular ih users list only

    private double lat;
    private double lon;

    public User() {
        super();
    }

    public User(ContentValues contentValues) {
        Integer idVal = contentValues.getAsInteger(UserColumns.ID);
        if (idVal != null)
            id = idVal.intValue();
        instagramId = contentValues.getAsString(UserColumns.INSTAGRAM_ID);
        username = contentValues.getAsString(UserColumns.USER_NAME);
        password = contentValues.getAsString(UserColumns.PASSWORD);
        fullName = contentValues.getAsString(UserColumns.FULL_NAME);
        email = contentValues.getAsString(UserColumns.EMAIL);
        location = contentValues.getAsString(UserColumns.LOCATION);
        website = contentValues.getAsString(UserColumns.WEB_SITE);
        profilePicUrl = contentValues.getAsString(UserColumns.PROFILE_PIC_URL);
        isFollowing = contentValues.getAsInteger(UserColumns.IS_FOLLOWS) == 1;
        isFollower = contentValues.getAsInteger(UserColumns.IS_FOLLOWER) == 1;
        Integer ageVal = contentValues.getAsInteger(UserColumns.AGE);
        if (ageVal != null)
            age = ageVal.intValue();
        isBlocked = contentValues.getAsInteger(UserColumns.IS_BLOCKED) == 1;
        isStarred = contentValues.getAsInteger(UserColumns.IS_STARRED) == 1;
        gender = contentValues.getAsString(UserColumns.GENDER);
        bio = contentValues.getAsString(UserColumns.BIO);
        Integer showLocVal = contentValues
                .getAsInteger(UserColumns.SHOW_LOCATION);
        if (showLocVal != null)
            showlocation = showLocVal.intValue();
        privileges = contentValues
                .getAsString(UserColumns.PRIVILEGES);
        fbid = contentValues.getAsString(UserColumns.FACEBOOK_ID);
        fbaccesstoken = contentValues
                .getAsString(UserColumns.FACEBOOK_ACCESS_TOKEN);
        tid = contentValues.getAsString(UserColumns.TWITTER_ID);
        taccesstoken = contentValues
                .getAsString(UserColumns.TWITTER_ACCESS_TOKEN);
        ttokensecret = contentValues
                .getAsString(UserColumns.TWITTER_TOKEN_SECRET);
        iaccesstoken = contentValues
                .getAsString(UserColumns.INSTAGRAM_ACCESS_TOKEN);
        final Integer unreadVal = contentValues.getAsInteger(UserColumns.UNREAD);
        if (unreadVal != null)
            unread = unreadVal.intValue();
        final Integer isInstalledVal = contentValues.getAsInteger(UserColumns.IS_INSTALLED);
        if (isInstalledVal != null)
            isInstalled = isInstalledVal.intValue() == 1;
        tstamp = new Timestamp(contentValues.getAsLong(UserColumns.TIME_STAMP));
        statusChangeListeners = new ArrayList<StatusChangeListener>();
    }

    public ContentValues getContentValues() {

        ContentValues values = new ContentValues();
        if (id != 0)
            values.put(UserColumns.ID, id);
        values.put(UserColumns.INSTAGRAM_ID, instagramId);
        values.put(UserColumns.USER_NAME, username);
        values.put(UserColumns.AGE, age);
        values.put(UserColumns.BIO, bio);
        values.put(UserColumns.EMAIL, email);
        values.put(UserColumns.FACEBOOK_ACCESS_TOKEN, fbaccesstoken);
        values.put(UserColumns.FACEBOOK_ID, fbid);
        values.put(UserColumns.IS_FOLLOWS, isFollowing ? 1 : 0);
        values.put(UserColumns.IS_FOLLOWER, isFollower ? 1 : 0);
        values.put(UserColumns.FULL_NAME, fullName);
        values.put(UserColumns.GENDER, gender);
        values.put(UserColumns.INSTAGRAM_ACCESS_TOKEN, iaccesstoken);
        values.put(UserColumns.IS_BLOCKED, isBlocked ? 1 : 0);
        values.put(UserColumns.IS_STARRED, isStarred ? 1 : 0);
        values.put(UserColumns.LOCATION, location);
        values.put(UserColumns.PASSWORD, password);
        values.put(UserColumns.PRIVILEGES, privileges);
        values.put(UserColumns.PROFILE_PIC_URL, profilePicUrl);
        values.put(UserColumns.SHOW_LOCATION, showlocation);
        values.put(UserColumns.TWITTER_ACCESS_TOKEN, taccesstoken);
        values.put(UserColumns.TWITTER_ID, tid);
        values.put(UserColumns.TWITTER_TOKEN_SECRET, ttokensecret);
        values.put(UserColumns.WEB_SITE, website);
        values.put(UserColumns.UNREAD, unread);
        values.put(UserColumns.IS_INSTALLED, isInstalled ? 1 : 0);
        values.put(UserColumns.TIME_STAMP, tstamp.getTime());
        return values;
    }

    /**
     * Return a map of only the attributes necessary for
     * indicating user presence in a group chat context
     *
     * @return
     */
    public Map<String, Object> toMap(boolean includeTimestamp) {
        Map<String, Object> map = new HashMap<>(10);
        map.put("username", username);
        if (profilePicUrl != null)
            map.put("profilePicUrl", profilePicUrl);
        map.put("id", id);
        if (bio != null)
            map.put("bio", bio);
        if (includeTimestamp)
            map.put(Constants.CHILD_LAST_ONLINE, ServerValue.TIMESTAMP);
        if (currentGroupName != null) {
            map.put(Constants.FIELD_CURRENT_GROUP_NAME, currentGroupName);
            map.put(Constants.FIELD_CURRENT_GROUP_ID, currentGroupId);
        }
        if (likes != 0) {
            map.put(Constants.CHILD_LIKES, likes);
        }
        return map;
    }

    public Map<String, Object> toMapForLikes() {
        Map<String, Object> map = new HashMap<>(1);
        map.put("username", username);
        if (profilePicUrl != null)
            map.put("profilePicUrl", profilePicUrl);
        map.put("id", id);
        if (likes != 0) {
            map.put(Constants.CHILD_LIKES, likes);
        }
        return map;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInstagramId() {
        return instagramId;
    }

    public void setInstagramId(String id) {
        this.instagramId = id;
    }

    public String getUsername() {
        return username + "";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullname) {
        this.fullName = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBio() {
        return bio + "";
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public int getShowlocation() {
        return showlocation;
    }

    public void setShowlocation(int showlocation) {
        this.showlocation = showlocation;
    }

    public String getPrivileges() {
        return privileges;
    }

    public void setPrivileges(String privileges) {
        this.privileges = privileges;
    }

    public String getFbid() {
        return fbid;
    }

    public void setFbid(String fbid) {
        this.fbid = fbid;
    }

    public String getFbaccesstoken() {
        return fbaccesstoken;
    }

    public void setFbaccesstoken(String fbaccesstoken) {
        this.fbaccesstoken = fbaccesstoken;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getTaccesstoken() {
        return taccesstoken;
    }

    public void setTaccesstoken(String taccesstoken) {
        this.taccesstoken = taccesstoken;
    }

    public String getTtokensecret() {
        return ttokensecret;
    }

    public void setTtokensecret(String ttokensecret) {
        this.ttokensecret = ttokensecret;
    }

    public String getIaccesstoken() {
        return iaccesstoken;
    }

    public void setIaccesstoken(String iaccesstoken) {
        this.iaccesstoken = iaccesstoken;
    }

    public Timestamp getTstamp() {
        return tstamp;
    }

    public void setTstamp(Timestamp tstamp) {
        this.tstamp = tstamp;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public boolean isFollower() {
        return isFollower;
    }

    public void setFollower(boolean isFollower) {
        this.isFollower = isFollower;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean isFollowing) {
        this.isFollowing = isFollowing;
    }

    public String getFullNameConcact() {
        String stripped;
        if (fullName != null) {
            stripped = stripWhiteSpace(fullName).toLowerCase();
        } else {
            stripped = "";
        }

        return stripped;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public void setStarred(boolean isStarred) {
        this.isStarred = isStarred;
    }

    public boolean isOnline() {
        return isOnline || isInstalled();
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline || isInstalled;
        if (statusChangeListeners == null) {
            statusChangeListeners = new ArrayList<StatusChangeListener>();
        }
        if (statusChangeListeners.size() > 0) {
            for (StatusChangeListener changeListener : statusChangeListeners) {
                if (changeListener != null)
                    changeListener.onStatusChanged(isOnline);
            }
        }
    }

    public void addStatusChangeListener(
            StatusChangeListener statusChangeListener) {
//		if (statusChangeListeners == null) {
//			statusChangeListeners = new ArrayList<StatusChangeListener>();
//		}		
//		statusChangeListeners.add(statusChangeListener);
    }

    public int getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        if (unread <= 0) {
            this.unread = 0;
        } else {
            this.unread = unread;
        }
    }

    public int getMediaCounts() {
        return mediaCounts;
    }

    public void setMediaCounts(int mediaCounts) {
        this.mediaCounts = mediaCounts;
    }

    public int getFollowsCount() {
        return followsCount;
    }

    public void setFollowsCount(int followsCount) {
        this.followsCount = followsCount;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public boolean isTransientAddedToRoster() {
        return transientAddedToRoster;
    }

    public void setTransientAddedToRoster(boolean transientAddedToRoster) {
        this.transientAddedToRoster = transientAddedToRoster;
    }

    public int getRemainingvc() {
        return remainingvc;
    }

    public void setRemainingvc(int remainingvc) {
        this.remainingvc = remainingvc;
    }

    public void setInstalled(final boolean installed) {
        this.isInstalled = installed;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User))
            return false;
        User user = (User) o;
        return user.id == id;
    }

    /**
     * Converts the location found by device to a more private format, stripping
     * out house or apartment #s
     *
     * @param loc
     * @return
     */
    private static String getVisibleLocation(final String loc) {

        if (loc == null || loc.equals("")) {
            return "";
        }
        final String[] a = loc.split("\\|");
        if (a.length >= 2) {
            final String part1 = stripNumbers(a[a.length - 2]);
            final String part2 = stripNumbers(a[a.length - 1]);
            return part1 + "|" + part2;
        }

        return "";
    }

    private static String stripNumbers(final String s) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static String stripWhiteSpace(String s) {
        StringBuilder sb = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                sb.append(s.charAt(i));
            }
        }

        return sb.toString();
    }

    public void setLat(final double lat) {
        this.lat = lat;
    }

    public void setLon(final double lon) {
        this.lon = lon;
    }

    /*
     * for popular ih users list only
     */
    public double getDistance() {
        return distance;
    }

    public void setDistance(final double distance) {
        this.distance = distance;
    }

    public static final int FollowStatusFollowedBy = 7;
    public static final int FollowStatusDoesNotExist = 6;
    public static final int FollowStatusFetching = 1;
    public static final int FollowStatusFollowing = 3;
    public static final int FollowStatusInProgress = 5;
    public static final int FollowStatusNotFollowing = 2;
    public static final int FollowStatusRequested = 4;
    public static final int FollowStatusUnknown = 0;

    public interface StatusChangeListener {
        void onStatusChanged(boolean isOnline);
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        try {
            object.put("id", id);
            if (username != null)
                object.put("username", username);
            if (password != null)
                object.put("password", password);
            if (email != null)
                object.put("email", email);
            if (bio != null)
                object.put("bio", bio);
            if (location != null)
                object.put("location", location);
            if (website != null)
                object.put("website", website);
            if (profilePicUrl != null)
                object.put("profilePicUrl", profilePicUrl);
            if (age != 0)
                object.put("age", age);
            if (gender != null)
                object.put("gender", gender);
            if (lastOnline != 0)
                object.put("lastOnline", lastOnline);
            if (currentGroupName != null)
                object.put("currentGroupName", currentGroupName);
            if (currentGroupId != 0)
                object.put("currentGroupId", currentGroupId);
            if (likes != 0)
                object.put("likes", likes);
            if (unread != 0)
                object.put("unread", unread);
        } catch (final JSONException e) {
            MLog.e("User", "", e);
        }
        return object;
    }

    public void copyFrom(JSONObject object) {
        id = object.optInt("id");
        username = object.optString("username");
        password = object.optString("password");
        email = object.optString("email");
        bio = object.optString("bio");
        location = object.optString("location");
        website = object.optString("website");
        profilePicUrl = object.optString("profilePicUrl");
        age = object.optInt("age");
        gender = object.optString("gender");
        lastOnline = object.optLong("lastOnline");
        isOnline = object.optBoolean("isOnline");
        isInstalled = object.optBoolean("isInstalled");
        currentGroupName = object.optString("currentGroupName");
        currentGroupId = object.optLong("currentGroupId");
        likes = object.optInt("likes");
        unread = object.optInt("unread");
    }

    public static User fromResponse(JSONObject response) throws Exception {
        final JSONObject data = response.getJSONObject("data");
        final User remote = new User();
        remote.copyFrom(data);
        return remote;
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public long getCurrentGroupId() {
        return currentGroupId;
    }

    public void setCurrentGroupId(long currentGroupId) {
        this.currentGroupId = currentGroupId;
    }

    public String getCurrentGroupName() {
        return currentGroupName;
    }

    public void setCurrentGroupName(String currentGroupName) {
        this.currentGroupName = currentGroupName;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void incrementLikes() {
        likes++;
    }
}
