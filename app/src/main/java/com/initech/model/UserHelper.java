package com.initech.model;

import org.json.JSONObject;

public final class UserHelper {

    public static final String TAG = UserHelper.class.getSimpleName();

	/*
     * NOTE: Instagram user structure
	 * 
	 * {"meta":{"code":200},"data":{"username":"theonetrevor","bio"
	 * :"im just that guy","website":"","profile_picture":
	 * "http:\/\/images.instagram.com\/profiles\/anonymousUser.jpg","full_name":"","counts":{"media":0,"followed_by":5,"follows":8},"id":"191624092"}}
	 */

    public static User mapIGUser(final JSONObject ig) {
        return mapIGUser(null, ig);
    }

    public static User mapIGUser(User user, final JSONObject ig) {
        try {
            if (user == null)
                user = new User();
            user.setFullName(ig.optString("full_name"));
            user.setProfilePicUrl(ig.optString("profile_picture"));
            user.setWebsite(ig.optString("website"));
            user.setBio(ig.optString("bio"));
            user.setUsername(ig.optString("username"));
            user.setInstagramId(ig.optString("id"));

            JSONObject counts = ig.optJSONObject("counts");
            if (counts != null) {
                user.setMediaCounts(counts.optInt("media"));
                user.setFollowsCount(counts.optInt("follows"));
                user.setFollowersCount(counts.optInt("followed_by"));
            }
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    public static User mapIHUser(final JSONObject ih) {
        try {
            final User user = new User();
            user.setFullName(ih.optString("firstname", ""));
            user.setGender(ih.optString("gender", ""));
            user.setProfilePicUrl(ih.optString("profilePicUrl"));
            user.setWebsite(ih.optString("website", ""));
            user.setBio(ih.optString("bio", ""));
            user.setUsername(ih.optString("username", ""));
            user.setInstagramId(ih.optString("iid", ""));
            user.setLat(ih.optDouble("lat", 0));
            user.setLon(ih.optDouble("lon", 0));
            user.setMediaCounts(ih.optInt("mediaCounts"));
            user.setFollowsCount(ih.optInt("followsCount", 0));
            user.setFollowersCount(ih.optInt("followersCount", 0));
            user.setDistance(ih.optDouble("distance", -1d));
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * NOTE: asynchronous call!
     * <p>
     * Fetches official user from IG based on
     * potentially partial user information
     * <p>
     * Finally, broadcasts the intent to switch fragments
     *
     * @param incomingUser
     */
    public static void showUserProfile(final User incomingUser) {

    }
}
