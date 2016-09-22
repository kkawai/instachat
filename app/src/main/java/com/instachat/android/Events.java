package com.instachat.android;

public class Events {

	public static final String IG_LOGIN_SUCESS = "IG_LOGIN_SUCESS";
	public static final String IG_LOGIN_FAIL = "IG_LOGIN_FAIL";
	public static final String IG_LOGOUT = "IG_LOGOUT";
	public static final String IH_LOGIN_FAIL = "IH_LOGIN_FAIL";
	
	public static final String FOLLOWINGS_TAB_SELECTED = "FOLLOWINGS_TAB_SELECTED";
	public static final String FOLLOWERS_TAB_SELECTED = "FOLLOWERS_TAB_SELECTED";
	public static final String FOLLOWINGS_LIST_REFRESH = "FOLLOWINGS_LIST_REFRESH";
	public static final String FOLLOWERS_LIST_REFRESH = "FOLLOWERS_LIST_REFRESH";
	
	public static final String CONTACT_BLOCK = "CONTACT_BLOCK";
	public static final String CONTACT_FAVORITE = "CONTACT_FAVORITE";
	public static final String USER_PHOTO_CLICKED = "USER_PHOTO_CLICKED";
	public static final String USER_ROW_CLICKED = "USER_ROW_CLICKED";
	
	public static final String USER_SEARCH = "USER_SEARCH";
	public static final String TAG_SEARCH = "TAG_SEARCH";
	public static final String TAG_CLICKED = "TAG_CLICKED";
	
	public static final String POPULAR_PHOTOS_REFRESH = "POPULAR_PHOTOS_REFRESH";
	public static final String POPULAR_USERS_REFRESH = "POPULAR_USERS_REFRESH";
	public static final String NEARBY_PHOTOS = "NEARBY_PHOTOS";
	public static final String POPULAR_USERS = "POPULAR_USERS";
	public static final String NEARBY_PHOTOS_REFRESH = "NEARBY_PHOTOS_REFRESH";
	
	public static final String CONTEXTUAL_MENU_IN_GRID = "CONTEXTUAL_MENU_IN_GRID";
	public static final String EDIT_USER_PROFILE = "EDIT_USER_PROFILE";
	public static final String USER_PROFILE_FOLLOW_BUTTON_PRESSED = "USER_PROFILE_FOLLOW_BUTTON_PRESSED";
	public static final String USER_LIST_FOLLOW_BUTTON_PRESSED = "USER_LIST_FOLLOW_BUTTON_PRESSED";
	public static final String USER_PROFILE_FOLLOWINGS_BUTTON_PRESSED = "USER_PROFILE_FOLLOWINGS_BUTTON_PRESSED";
	public static final String USER_PROFILE_FOLLOWERS_BUTTON_PRESSED = "USER_PROFILE_FOLLOWERS_BUTTON_PRESSED";
	public static final String USER_PROFILE_LOGOUT_BUTTON_PRESSED = "USER_PROFILE_LOGOUT_BUTTON_PRESSED";
	public static final String CHAT_BUTTON_PRESSED = "CHAT_BUTTON_PRESSED";
	public static final String INVITE_BUTTON_PRESSED = "INVITE_BUTTON_PRESSED";
	public static final String PROMOTE_BUTTON_PRESSED = "PROMOTE_BUTTON_PRESSED";
	
	public static final String USER_PHOTO_FULL_SCREEN = "USER_PHOTO_FULL_SCREEN";
	public static final String USER_PHOTO_FULL_SCREEN_TAP = "USER_PHOTO_FULL_SCREEN_TAP";
	public static final String USER_PHOTO_FULL_SCREEN_DOUBLE_TAP = "USER_PHOTO_FULL_SCREEN_DOUBLE_TAP";
	public static final String USER_PHOTO_SCROLL = "USER_PHOTO_SCROLL";
	public static final String PHOTO_LIKED = "PHOTO_LIKED";
	public static final String PHOTO_UNLIKED = "PHOTO_UNLIKED";
	
	public static final String SETTINGS_ABOUT_SELECTED = "SETTINGS_ABOUT_SELECTED";
	public static final String SETTINGS_DELETE_MESSAGES = "SETTINGS_DELETE_MESSAGES";
	public static final String SETTINGS_NOTIFICATIONS_OFF = "SETTINGS_NOTIFICATIONS_OFF";
	public static final String SETTINGS_VIBRATE_OFF = "SETTINGS_VIBRATE_OFF";
	public static final String SETTINGS_SOUND_OFF = "SETTINGS_SOUND_OFF";
	public static final String SETTINGS_ENCRYPTION_OFF = "SETTINGS_ENCRYPTION_OFF";
	public static final String SETTINGS_EXTRAS_SELECTED = "SETTINGS_EXTRAS_SELECTED";
	public static final String SETTINGS_DISCONNECT_SELECTED = "SETTINGS_DISCONNECT_SELECTED";
	public static final String SETTINGS_LOGOUT_SELECTED = "SETTINGS_LOGOUT_SELECTED";
	
	public static final String CHAT_SESSION = "CHAT_SESSION";
	public static final String EMOTICON_SELECTED = "EMOTICON_SELECTED";
	public static final String CHAT_LIST_OPENED = "CHAT_LIST_OPENED";
	public static final String CHAT_LIST_CHAT_CLOSED = "CHAT_LIST_CHAT_CLOSED";
	public static final String CHAT_LIST_CHAT_SELECTED = "CHAT_LIST_CHAT_SELECTED";
	public static final String SEND_PHOTO_SELECTED = "SEND_PHOTO_SELECTED";
	public static final String CAPTURE_AND_SEND_SELECTED = "CAPTURE_AND_SEND_SELECTED";
	public static final String CHAT_BLOCK_USER_SELECTED = "CHAT_BLOCK_USER_SELECTED";
	public static final String CHAT_CLEAR_SELECTED = "CHAT_CLEAR_SELECTED";
	public static final String CHAT_END_SELECTED = "CHAT_END_SELECTED";
	public static final String VOICE_MESSAGE_BUTTON_PRESSED = "VOICE_MESSAGE_BUTTON_PRESSED";
	public static final String VOICE_MESSAGE_SEND = "VOICE_MESSAGE_SEND";
	public static final String TEXT_MESSAGE_SEND = "TEXT_MESSAGE_SEND";
	public static final String PIC_MESSAGE_SEND = "PIC_MESSAGE_SEND";
	public static final String DELETE_MESSAGE_SEND = "DELETE_MESSAGE_SEND";
	public static final String MSG_SEND_BUTTON_PRESSED = "MSG_SEND_BUTTON_PRESSED";
	public static final String MSG_DONE_KEY_PRESSED = "MSG_DONE_KEY_PRESSED";
	public static final String MSG_COPY = "MSG_COPY";
	public static final String MSG_DELETE = "MSG_DELETE";
	public static final String MSG_CLICKED = "MSG_CLICKED";
	public static final String MSG_PHOTO_CLICKED = "MSG_PHOTO_CLICKED";
	public static final String MSG_VOICE_CLICKED = "MSG_VOICE_CLICKED";
	
	public static final String PURCHASE_DIALOG_PROMPTED = "PURCHASE_DIALOG_PROMPTED";
	public static final String PURCHASE_BUTTON_CLICKED = "PURCHASE_BUTTON_CLICKED";
	public static final String VC_DIALOG_PTOMPTED = "VC_DIALOG_PTOMPTED";
	
	public static final String CHAT_NO_XMPP_ON_POST = "CHAT_NO_XMPP_ON_POST";
	
	public static final String PARAM_ERROR_CODE = "PARAM_ERROR_CODE";
	
	public static final String RATE_NOW = "RATE_NOW";
	public static final String RATE_LATER = "RATE_LATER";
	public static final String RATE_NO_THANKS = "RATE_NO_THANKS";
	public static final String RATE_NEEDS_IMPROVEMENT = "RATE_NEEDS_IMPROVEMENT";
	public static final String APP_CRASHED = "APP_CRASHED";
	public static final String APP_THROWABLE_TRAPPED = "APP_THROWABLE_TRAPPED";
	
	public static final String UNCAUGHT_ERROR = "UNCAUGHT_ERROR";
	public static final String IG_LOGIN_ERROR = "IG_LOGIN_ERROR";
	public static final String SSL_ERROR = "SSL_ERROR";
	
	public static final String NO_EXTERNAL_CACHE_DIR_FOR_PROMOTE = "NO_EXTERNAL_CACHE_DIR_FOR_PROMOTE";
	public static final String NO_EXTERNAL_CACHE_DIR_FOR_PHOTO = "NO_EXTERNAL_CACHE_DIR_FOR_PHOTO";
	
	public static final String GCM_INCOMING_MSG_FAIL = "GCM_INCOMING_MSG_FAIL";
	public static final String GCM_INIT_ERROR = "GCM_INIT_ERROR";
	public static final String NO_GOOGLE_PLAY_SERVICES = "NO_GOOGLE_PLAY_SERVICES";
	
	public static final String PROMOTE_MYSELF_ERROR = "PROMOTE_MYSELF_ERROR";
	public static final String AD_SHOWED_INTERSTITIAL = "AD_SHOWED_INTERSTITIAL";
	public static final String SETTINGS_NO_ADS_SELECTED = "SETTINGS_NO_ADS_SELECTED";
	public static final String CLEAR_CACHE = "CLEAR_CACHE";

    public static final String POPULAR_MALE = "POPULAR_MALE";
    public static final String POPULAR_FEMALE = "POPULAR_FEMALE";
    public static final String POPULAR_BOTH_GENDER = "POPULAR_BOTH_GENDER";
}
