package com.instachat.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.instachat.android.Constants;
import com.instachat.android.TheApp;
import com.instachat.android.data.model.User;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public final class UserPreferences {

    private static final Random RANDOM = new Random();

    private UserPreferences() {
    }

    //Note: 'photo_path' is simply used for deceptive purposes.  It's really the user.
    private static final String PREFERENCE_USER = "user";  //this is here for deceptive purposes; it does nothing
    private static final String PHOTOS_PATH = "photos_path"; //named deceptively; used for actual user path
    private static final String PREFERENCE_IS_LOGGED_IN = "is_logged_in";
    private static final String PREFERENCE_LAST_SIGN_IN = "last_sign_in";
    private static final String INSTAGRAM_ACCESS_TOKEN = "accessToken";
    private static final String INSTAGRAM_STORE = "INSTAGRAM_STORE";

    private static final String PREFERENCE_ENCRYPTION = "msg_encryption";
    private static final String PREFERENCE_VIBRATE = "vibrate";
    private static final String PREFERENCE_SOUND = "sound";

    private static final int MAX_SAVED_TAGS = 15;
    private static final int MAX_SAVED_USERS = 5;
    public static final String PREFERENCES_SERVICE = "com.instachat.android.UserPreferences";
    private static final String PREFERENCE_ADVANCED_CAMERA_ENABLED = "advanced_camera_enabled";
    private static final String PREFERENCE_BORDERS_ENABLED = "borders_enabled";
    private static final String PREFERENCE_DOUBLE_TAP_TO_LIKE_HINT_IMPRESSIONS = "used_double_tap_hint_impressions";
    private static final String PREFERENCE_GEOTAG_ENABLED = "geotag_enabled";
    private static final String PREFERENCE_HAS_USED_DOUBLE_TAP_TO_LIKE = "used_double_tap";
    private static final String PREFERENCE_NEEDS_PHOTO_MAP_EDUCATION = "needs_photo_map_education";
    private static final String PREFERENCE_PUSH_REGISTRATION_DATE = "push_reg_date";
    private static final String PREFERENCE_RECENT_HASHTAG_SEARCHES = "recent_hashtag_searches";
    private static final String PREFERENCE_RECENT_USER_SEARCHES = "recent_user_searches";
    private static final String PREFERENCE_SYSTEM_MESSAGES = "system_message_";
    private static final String PREFERENCE_UNIQUE_ID = "unique_id";
    private static final String PREFERENCE_SHOW_VC_COUNT_MSG = "show_vc_count_msg";

    private static final String TAG = "UserPreferences";
    private static final long TWO_DAYS = 172800000L;
    private static final long HALF_DAY = 43200000L;
    private static String sUniqueID = null;
    // private final ObjectMapper mObjectMapper;
    private SharedPreferences mPrefs;
    private static UserPreferences instance;
    private static User sUser;

    private UserPreferences(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static UserPreferences getInstance() {
        if (instance == null)
            instance = new UserPreferences(TheApp.getInstance());
        return instance;
    }

    public boolean isEncryptionEnabled() {
        return this.mPrefs.getBoolean(PREFERENCE_ENCRYPTION, true);
    }

    public void setEncryptionEnabled(final boolean enable) {
        final Editor editor = mPrefs.edit();
        editor.putBoolean(PREFERENCE_ENCRYPTION, enable);
        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                editor.commit();
            }
        });
    }

    public boolean isVibrateEnabled() {
        return this.mPrefs.getBoolean(PREFERENCE_VIBRATE, true);
    }

    public void setVibrateEnabled(final boolean enable) {
        final Editor editor = mPrefs.edit();
        editor.putBoolean(PREFERENCE_VIBRATE, enable);
        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                editor.commit();
            }
        });
    }

    public boolean isSoundEnabled() {
        return this.mPrefs.getBoolean(PREFERENCE_SOUND, true);
    }

    public void setSoundEnabled(final boolean enable) {
        final Editor editor = mPrefs.edit();
        editor.putBoolean(PREFERENCE_SOUND, enable);
        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                editor.commit();
            }
        });
    }

    public ArrayList getRecentHashtagSearches() {
        return null; //TODO
    }

    public ArrayList getRecentUserSearches() {
        return null; //TODO
    }

    public void saveRecentHashtag(String hashtag) {
        //TODO
    }

    public void saveRecentUser(User user) {
        //TODO
    }

    public void signOut() {
        sUser = null;
        mPrefs.edit().remove(PHOTOS_PATH).remove(PREFERENCE_IS_LOGGED_IN).apply();
    }

    public boolean isLoggedIn() {
        if (getUser() == null || getUser().getUsername() == null)
            return false;
        return mPrefs.getBoolean(PREFERENCE_IS_LOGGED_IN, false);
    }

    public Integer getUserId() {
        return getUser().getId();

    }

    public String getUsername() {
        return getUser().getUsername();
    }

    public String getEmail() {
        return getUser().getEmail();
    }

    public User getUser() {
        if (sUser != null) {
            return sUser;
        }
        try {
            if (mPrefs.getBoolean(PREFERENCE_IS_LOGGED_IN, false) == false) {
                return null;
            }
            JSONObject json = new JSONObject(AdminUtil.decode(mPrefs.getString(PHOTOS_PATH, null)));
            User user = new User();
            user.copyFrom(json);
            if (user.getId() > 0 && user.getUsername().length() > 1) { //minor validation
                sUser = user;
                return sUser;
            } else {
                return null;
            }
        } catch (Exception e) {
            MLog.e(TAG,"getUser()",e);
        }
        return null;
    }

    public void saveLastSignIn(final String lastSignIn) {
        MLog.i(TAG, "lastSignIn " + lastSignIn);
        mPrefs.edit().putString(PREFERENCE_LAST_SIGN_IN, lastSignIn).apply();
    }

    public String getLastSignIn() {
        return mPrefs.getString(PREFERENCE_LAST_SIGN_IN, null);
    }

    public void removeLastSignIn() {
        mPrefs.edit().remove(PREFERENCE_LAST_SIGN_IN).apply();
    }

    public void saveUser(@NonNull User user) {
        if (user == null)
            throw new IllegalArgumentException("cannot pass null user to saveUser(..)");
        sUser = user;
        try {
            String encode = AdminUtil.encode(user.toJSON().toString());
            mPrefs.edit().putBoolean(PREFERENCE_IS_LOGGED_IN, true).putString(PHOTOS_PATH, encode).apply();
            mPrefs.edit().putString(PREFERENCE_USER, user.toJSON().toString()).apply(); //deception!
        }catch (Exception e) {
            MLog.e(TAG,"saveUser failed",e);
        }
    }

    public String getAccessToken() {
        final String accessToken = mPrefs.getString(INSTAGRAM_ACCESS_TOKEN, null);
        MLog.i(UserPreferences.class.getSimpleName(), "Prefences.getAccessToken()=" + accessToken);
        return this.mPrefs.getString(INSTAGRAM_ACCESS_TOKEN, null);
    }

    public void saveAccessToken(final String accessToken) {
        final Editor editor = this.mPrefs.edit();
        if (accessToken == null) {
            editor.remove(INSTAGRAM_ACCESS_TOKEN);
            MLog.i(UserPreferences.class.getSimpleName(), "Prefences.saveAccessToken() removed access token because it's " +
                    "null..");
        } else {
            editor.putString(INSTAGRAM_ACCESS_TOKEN, accessToken);
            MLog.i(UserPreferences.class.getSimpleName(), "Prefences.saveAccessToken() saved.." + accessToken);
        }
        editor.commit();
    }

    public void setVcCountShown() {
        final Editor editor = this.mPrefs.edit();
        editor.putBoolean(PREFERENCE_SHOW_VC_COUNT_MSG, true);
        editor.commit();
    }

    public boolean isVcCountShown() {
        return this.mPrefs.getBoolean(PREFERENCE_SHOW_VC_COUNT_MSG, false);
    }

    public boolean isNotifyEnabled() {
        return this.mPrefs.getBoolean("notify", true);
    }

    public void setNotifyEnabled(final Boolean newValue) {
        final Editor editor = this.mPrefs.edit();
        editor.putBoolean("notify", newValue);
        editor.commit();
    }

    public void incrementStarts() {
        final int starts = mPrefs.getInt("starts", 0);
        mPrefs.edit().putInt("starts", starts + 1).commit();
    }

    public int getStarts() {
        return mPrefs.getInt("starts", 1);
    }

    public void setRated(final boolean isRated) {
        mPrefs.edit().putBoolean("is_rated", isRated).commit();
    }

    public boolean isRated() {
        return mPrefs.getBoolean("is_rated", false);
    }

    public void locationUpdated() {
        final Editor editor = this.mPrefs.edit();
        editor.putLong("location_update", new Date().getTime());
        editor.commit();
    }

    public boolean locationNeedsUpdate() {
        final long last = mPrefs.getLong("location_update", 0);
        final long now = new Date().getTime();
        return now - last > HALF_DAY;
    }

    public String getGender() {
        return mPrefs.getString("gender", "");
    }

    public void setGender(final String gender) {
        mPrefs.edit().putString("gender", gender).commit();
    }

    public int getBannerAdCounter() {
        final int cur = mPrefs.getInt("bannerAdCounter", RANDOM.nextInt(9));
        int next = cur + 1;
        next = next > 100000 ? 1 : next; // reset after 100k requests
        mPrefs.edit().putInt("bannerAdCounter", next).commit();
        return cur;
    }

    public int getInterstitialAdCounter() {
        final int cur = mPrefs.getInt("interstitialAdCounter", RANDOM.nextInt(9));
        int next = cur + 1;
        next = next > 100000 ? 1 : next; // reset after 100k requests
        mPrefs.edit().putInt("interstitialAdCounter", next).commit();
        return cur;
    }

    public boolean hasShownUsernameTooltip() {
        return mPrefs.getBoolean("shown_username_tooltip", false);
    }

    public void setShownUsernameTooltip(boolean shown) {
        mPrefs.edit().putBoolean("shown_username_tooltip", shown).apply();
    }

    public boolean hasShownToolbarProfileTooltip() {
        return mPrefs.getBoolean("shown_toolbar_profile_tooltip", false);
    }

    public void setShownToolbarProfileTooltip(boolean shown) {
        mPrefs.edit().putBoolean("shown_toolbar_profile_tooltip", shown).apply();
    }

    public long getLastGroupChatRoomVisited() {
        return mPrefs.getLong("last_group_chat_room", Constants.DEFAULT_PUBLIC_GROUP_ID);
    }

    public void setLastGroupChatRoomVisited(long groupid) {
        mPrefs.edit().putLong("last_group_chat_room", groupid).apply();
    }

    public boolean hasShownSendFirstMessageDialog() {
        return mPrefs.getBoolean("shown_first_message_dialog_" + getUserId(), false);
    }

    public void setShownSendFirstMessageDialog(boolean shown) {
        mPrefs.edit().putBoolean("shown_first_message_dialog_" + getUserId(), shown).apply();
    }

    private  static final long ONE_HOUR = 60 * 1000 * 60L;

    public boolean canShowRewardedAd() {
        return System.currentTimeMillis()
                - mPrefs.getLong("last_rewarded_ad_shown", 0L)
                > ONE_HOUR;
    }

    public void setShownRewardedAd() {
        mPrefs.edit().putLong("last_rewarded_ad_shown", System.currentTimeMillis()).apply();
    }
}
