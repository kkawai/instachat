package com.instachat.android;

import com.instachat.android.util.Preferences;

/**
 * @author kkawai
 */
public final class Constants {

    private Constants() {
    }

    public static final int MAX_USERNAME_LENGTH = 30;//also see dimens
    public static final int MIN_USERNAME_LENGTH = 2;
    public static final int MAX_EMAIL_LENGTH = 128; //also see dimens
    public static final int MIN_EMAIL_LENGTH = 5;
    public static final int MAX_PASSWORD_LENGTH = 20;//also see dimens
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_BIO_LENGTH = 512;

    //remote configuration with default values
    public static final boolean DEFAULT_DO_SHORTEN_IMAGE_URLS = true;
    public static final boolean DEFAULT_ALLOW_DELETE_OTHER_MESSAGES = false;
    public static final int DEFAULT_MAX_MESSAGE_HISTORY = 1000;
    public static final int DEFAULT_MAX_INDETERMINATE_MESSAGE_FETCH_PROGRESS = 3500;
    public static final int DEFAULT_MAX_TYPING_DOTS_DISPLAY_TIME = 3500;
    public static final int DEFAULT_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY = 8000;
    public static final int DEFAULT_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME = 4000;
    public static final int DEFAULT_MAX_MESSAGE_LENGTH = 256;
    public static final int DEFAULT_MAX_PERISCOPABLE_LIKES_PER_ITEM = 25;
    public static String KEY_MAX_MESSAGE_HISTORY = "MAX_MESSAGE_HISTORY";
    public static String KEY_MAX_INDETERMINATE_MESSAGE_FETCH_PROGRESS = "MAX_INDETERMINATE_MESSAGE_FETCH_PROGRESS";
    public static String KEY_MAX_TYPING_DOTS_DISPLAY_TIME = "MAX_TYPING_DOTS_DISPLAY_TIME";
    public static String KEY_COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY = "COLLAPSE_PRIVATE_CHAT_APPBAR_DELAY";
    public static String KEY_MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME = "MAX_SHOW_PROFILE_TOOLBAR_TOOL_TIP_TIME";
    public static String KEY_MAX_MESSAGE_LENGTH = "MAX_MESSAGE_LENGTH";
    public static String KEY_MAX_PERISCOPABLE_LIKES_PER_ITEM = "MAX_PERISCOPABLE_LIKES_PER_ITEM";
    public static String KEY_ALLOW_DELETE_OTHER_MESSAGES = "ALLOW_DELETE_OTHER_MESSAGES";
    public static String KEY_DO_SHORTEN_IMAGE_URLS = "DO_SHORTEN_IMAGE_URLS";

    /*
     * CHANGE THESE WHEN MAKING PRODUCTION BUILDS
     */
    public static final boolean IS_LOGGING_ENABLED = false;
    public static final boolean IS_FOR_AMAZON_ONLY = false;//CHANGE PER AMAZON BUILD!!!
    public static final boolean IS_AMAZON_ADS_ENABLED = false;//CHANGE PER AMAZON BUILD!!!
    public static final boolean IS_AMAZON_DEBUG_AD = false; //CHANGE PER AMAZON BUILD!!!

    /*
     * SPAM constants control how many comments
     * can be posted within a certain time span.
     *
     * E.g. you can't post more than 2 comments
     * within 5 seconds.
     */
    public static final int SPAM_MAX_BURST_COMMENTS = 2;
    public static final int SPAM_BURST_DURATION = 5000;

    public static final String GC_SERVICE_NAME = "conference.xmpp.instachat.us";
    public static final String GC_SETTINGS = "group_chat_settings";

    public static final String XMPP_SERVER = "xmpp.instachat.us";
    public static final String API_BASE_URL = "https://api.instachat.us/ih";
    public static final String PUBLIC_WEBSITE = "http://instachat.us";
    public static final int XMPP_PORT = 50138; //5222;
    public static final int GLOBAL_GRID_COLUMNS = 3;

    public static final String ACTION_EXIT = "com.instachat.android.ACTION_EXIT";
    public static final String ACTION_OFFLINE_MESSAGE_UPDATE = "com.instachat.android.ACTION_OFFLINE_MESSAGE_UPDATE";

    /*
     * number of messages to display before fetching from database
     */
    public static final int MAX_MESSAGE_FETCH_SIZE = 20;

    public static final String EDIT_PROFILE_URL = "https://instagram.com/accounts/edit_inapp/";
    public static final String ANIMATED_EMOS_URL = "http://images.instachat.us/files/animemo/";

    public static final int MAX_THUMBNAILS_CACHE_MB = 3;

    /*
     * let's just have one place on the disk for all caches
     */
    public static final String GLOBAL_DISK_CACHE_NAME = "images";
    public static final int GLOBAL_DISK_CACHE_SIZE_MB = 1024 * 1024 * 25; // 20
    // megs

    /*
     * NOTE!! MAKE SURE IS_DEBUG_CRAZY_USER IS FALSE FOR PRODUCTION!!!
     */
    public static final boolean IS_DEBUG_CRAZY_USER = false;
    public static final boolean IS_CHATROOMS_ENABLED = true;
    public static final boolean IS_ANIMATED_EMO_ENABLED = false;

    public static final String AMAZON_APP_KEY_PROD = "30551ba1743442cdb3ad7b6d3276531e";
    public static final String AMAZON_APP_KEY_TEST = "sample-app-v1_pub-2";

    public static final int MAX_SECONDS_TO_DISPLAY_IND_PROGRESS = 7500;

    public static final String GCM_SENDER_ID = "483704506104";
    public static final int MAX_ALLOWED_FOLLOWED_BY = 500;
    public static final int MAX_IMPORTED_IG_FOLLOWERS = 400;
    public static final int MAX_IMPORTED_IG_FOLLOWS = 500;

    public static final String FLURRY_KEY = "HC7CVBW3RQ5XTN5RZK28";
    public static final String CONTACT_EMAIL = "help@instachat.us";
    public static final String PROMOTE_IMAGE_NAME = "promote_me.jpg";

    public static final String KEVIN_ADMOB_BANNER_ID = "ca-app-pub-6966894572988765/6985884411";
    public static final String KEVIN_ADMOB_INTERSTITIAL_ID = "ca-app-pub-6966894572988765/9939350812";
    public static final String RONAK_ADMOB_BANNER_ID = "ca-app-pub-7530127826585618/6160553686";
    public static final String RONAK_ADMOB_INTERSTITIAL_ID = "ca-app-pub-7530127826585618/7637286882";

    /*
     * Use for when sending voice or photo, since the text cannot be null.
     */
    public static final String XMPP_BLANK_MSG = "          ";

    public static final String MALE_VALUE = "m";
    public static final String FEMALE_VALUE = "f";

    public static final int MAX_GC_MSG_SIZE = 256;
    public static final int MAX_EMOJI_PER_POST = 8;

    // User with lot of followers/follows.
    public static final String CRAZY_USER_ID = "245343712";


    public static final String TREVOR_IID = "191624092";
    public static final String RONAK_IID = "181054351";
    public static final String INSTACHAT_IID = "260710584";
    public static final String INSTACHAT_USERNAME = "instachat__";
    public static final String INSTACHAT_PIC_URL = "http://images.instagram.com/profiles/profile_260710584_75sq_1355124007.jpg";

    public static final String AMAZON_BUCKET_DP_IC = "dp.ic";
    //public static final String AMAZON_BUCKET_VC_IC = "vc.ic"; use later
    public static final String AMAZON_BUCKET_GC_IC = "gc.ic";
    public static final String AMAZON_BUCKET_IH_USR = "ih.usr";

    public static final int REQ_CODE_ACCESS_LOCATION = 50;
    public static final int REQ_CODE_RECORD_AUDIO = 51;
    public static final int REQ_CODE_ACCESS_CAMERA = 52;
    public static final int REQ_CODE_READ_EXTERNAL_STORAGE = 53;

    public static final String FILE_PROVIDER = "com.instachat.android.fileprovider";
    public static final String FILE_PROVIDER_IMAGES_SUBDIR = "/images"; //look at file_provider_paths.xml

    public static final String KEY_PHOTO_TYPE = "key_phototype";
    public static final String KEY_FRIENDLY_MESSAGE = "key_fm_text";
    public static final String KEY_FRIENDLY_MESSAGE_DATABASE = "key_fm_database";
    public static final String KEY_STARTING_POS = "key_starting_pos";

    public static String USER_INFO_REF(int userid) {
        return "/users/" + userid + "/info";
    }

    public static String PRIVATE_CHAT_REF(int toUserid) {
        final int myUserid = Preferences.getInstance().getUserId();
        return "/directs/" + (toUserid > myUserid ? (myUserid + "_" + toUserid) : (toUserid + "_" + myUserid));
    }

    public static String PRIVATE_CHAT_TYPING_REF(int toUserid) {
        final int myUserid = Preferences.getInstance().getUserId();
        return "/direct_typing/" + (toUserid > myUserid ? (myUserid + "_" + toUserid) : (toUserid + "_" + myUserid));
    }

    public static String MY_PRIVATE_CHATS_SUMMARY_PARENT_REF() {
        return "/users/" + Preferences.getInstance().getUserId() + "/private_summaries/";
    }

    public static String MY_PRIVATE_REQUESTS_REF() {
        return "/users/" + Preferences.getInstance().getUserId() + "/private_requests/";
    }

    public static String PRIVATE_REQUEST_STATUS_PARENT_REF(int fromUserid, int toUserid) {
        return "/users/" + toUserid + "/private_requests/" + fromUserid;
    }

    public static String MY_BLOCKS_REF() {
        return "/users/" + Preferences.getInstance().getUserId() + "/blocks/";
    }

    public static String GROUP_CHAT_REF(long groupid) {
        return "/public_group_messages/" + groupid;
    }

    public static String GROUP_CHAT_USERS_REF(long groupid) {
        return "/public_group_users/" + groupid;
    }

    public static String GROUP_CHAT_USERS_TYPING_REF(long groupid, int userid) {
        return "/public_group_users_typing/" + groupid + "/" + userid;
    }

    public static String GROUP_CHAT_USERS_TYPING_PARENT_REF(long groupid) {
        return "/public_group_users_typing/" + groupid;
    }

    public static String REPORTS_REF(int userid) {
        return "/reports/" + userid;
    }

    public static String PUBLIC_CHATS_SUMMARY_PARENT_REF = "/public_group_summaries/";

    /**
     * use-case: list of users that liked the given message
     * captures the like count in each user stored at this location
     *
     * @param friendlyMessageId
     * @return
     */
    public static final String MESSAGE_LIKES_REF(String friendlyMessageId) {
        return "/message_likes/" + friendlyMessageId + "/";  //users will be children
    }

    /**
     * use-case: lifetime total of how many likes a given user issued
     *
     * @param userid
     * @return
     */
    public static final String USER_TOTAL_GIVEN_LIKES_REF(int userid) {
        return "/users/" + userid + "/total_likes_given/"; //some integer
    }

    /**
     * use-case: lifetime total of how many likes a given user issued
     *
     * @param userid
     * @return
     */
    public static final String USER_TOTAL_LIKES_RECEIVED_REF(int userid) {
        return "/users/" + userid + "/total_likes_received/"; //some integer
    }

    //keep track of users a given user received likes from
    public static final String USER_RECEIVED_LIKES_REF(int userid) {
        return "/users/" + userid + "/received_user_likes/"; //unique users
    }

    //keep track of users that gave likes to a given user
    public static final String USER_GIVEN_LIKES_REF(int userid) {
        return "/users/" + userid + "/given_user_likes/"; //unique users
    }

    public static String DP_STORAGE_BASE_REF(int userid) {
        return "/users/" + userid + "/dp/";
    }

    public static final long DEFAULT_PUBLIC_GROUP_ID = 1;
    public static final int MAX_PIC_SIZE_BYTES = 900000;
    public static final int MAX_PROFILE_PIC_SIZE_BYTES = 400000;
    public static final float MAX_FULLSCREEN_FONT_SIZE = 199f;

    public static final String KEY_GROUPID = "_key_group_id";
    public static final String KEY_GROUP_NAME = "key_group_name";
    public static final String KEY_USERID = "key_user_id";
    public static final String KEY_GCM_MSG_TYPE = "key_gcm_type";
    public static final String KEY_SHARE_PHOTO_URI = "key_share_photo_uri";
    public static final String KEY_SHARE_MESSAGE = "key_share_message";
    public static final String KEY_PROFILE_PIC_URL = "key_profile_pic_url";
    public static final String KEY_USERNAME = "key_username";
    public static final String KEY_AUTO_ADD_PERSON = "key_auto_add_person";

    public enum GcmMessageType {
        msg, notify_friend_in
    }

    public static final String ACTION_USER_TYPING = "action_user_typing";
    public static final String KEY_GCM_MESSAGE = "msg"; //DO NOT CHANGE, SERVER DEPENDENT
    public static final String KEY_TO_USERID = "toid";//DO NOT CHANGE, SERVER DEPENDENT
    public static final String CHILD_UNREAD_MESSAGES = "unread_messages";
    public static final String CHILD_LIKES = "likes";
    public static final String CHILD_TYPING = "isTyping";
    public static final String CHILD_USERNAME = "username";
    public static final String CHILD_MESSAGE_CONSUMED_BY_PARTNER = "consumedByPartner";
    public static final String CHILD_ACCEPTED = "accepted";
    public static final String CHILD_LAST_ONLINE = "lastOnline";

    /**
     * Any message received by a user whose last message
     * in the conversation is the same user, then instead
     * of creating a new message line, append it to the last
     * message.
     */
    public static final boolean IS_SUPPORT_MESSAGE_APPENDING = true;

    public static final int USER_ONLINE = 0;
    public static final int USER_OFFLINE = 1;
    public static final int USER_AWAY = 2;
    public static final long TWELVE_HOURS = 1000 * 60 * 60 * 12L;

    public static String KEY_TEXT_REPLY = "key_text_reply";
    public static String FIELD_LAST_MESSAGE_SENT_TIMESTAMP = "lastMessageSentTimestamp";
    public static String FIELD_CURRENT_GROUP_ID = "currentGroupId";
    public static String FIELD_CURRENT_GROUP_NAME = "currentGroupName";

    public static final String GOOGLE_API_KEY = "AIzaSyAHnphNjr0gqXkxps262WCzNmyG_P6OE24";

}
