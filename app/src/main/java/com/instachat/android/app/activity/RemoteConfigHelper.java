package com.instachat.android.app.activity;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.instachat.android.Constants;

import java.util.HashMap;
import java.util.Map;

public class RemoteConfigHelper {

    public FirebaseRemoteConfig initializeRemoteConfig() {
        // Initialize Firebase Remote Config.
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(false).build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(Constants.KEY_MAX_MESSAGE_HISTORY, Constants.DEFAULT_MAX_MESSAGE_HISTORY);
        defaultConfigMap.put(Constants.KEY_MAX_INDETERMINATE_MESSAGE_FETCH_PROGRESS, Constants
                .DEFAULT_MAX_INDETERMINATE_MESSAGE_FETCH_PROGRESS);
        defaultConfigMap.put(Constants.KEY_MAX_TYPING_DOTS_DISPLAY_TIME, Constants
                .DEFAULT_MAX_TYPING_DOTS_DISPLAY_TIME);
        defaultConfigMap.put(Constants.KEY_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY, Constants
                .DEFAULT_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY);
        defaultConfigMap.put(Constants.KEY_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME, Constants
                .DEFAULT_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME);
        defaultConfigMap.put(Constants.KEY_MAX_MESSAGE_LENGTH, Constants.DEFAULT_MAX_MESSAGE_LENGTH);
        defaultConfigMap.put(Constants.KEY_MAX_PERISCOPABLE_LIKES_PER_ITEM, Constants
                .DEFAULT_MAX_PERISCOPABLE_LIKES_PER_ITEM);
        defaultConfigMap.put(Constants.KEY_ALLOW_DELETE_OTHER_MESSAGES, Constants.DEFAULT_ALLOW_DELETE_OTHER_MESSAGES);
        defaultConfigMap.put(Constants.KEY_DO_SHORTEN_IMAGE_URLS, Constants.DEFAULT_DO_SHORTEN_IMAGE_URLS);
        defaultConfigMap.put(Constants.KEY_DO_SHOW_SIGNOUT_BUTTON, Constants.DEFAULT_DO_SHOW_SIGNOUT_BUTTON);
        defaultConfigMap.put(Constants.KEY_DO_SHOW_ADS, Constants.DEFAULT_DO_SHOW_ADS);
        defaultConfigMap.put(Constants.KEY_ADMIN_USERS, Constants.DEFAULT_ADMIN_USERS);


        // Apply config settings and default values.
        firebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        firebaseRemoteConfig.setDefaults(defaultConfigMap);
        return firebaseRemoteConfig;
    }
}
