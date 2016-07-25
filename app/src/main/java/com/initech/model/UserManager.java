package com.initech.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import com.initech.MyApp;
import com.initech.util.MLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class UserManager {

    private static final String TAG = UserManager.class.getSimpleName();
    private static final int DB_VERSION = 4;
    private static UserManager instance;
    public static final String DATABASE_NAME = "users.db";
    public static final String USERS_TABLE_NAME = "users";
    public static final String INVITES_TABLE_NAME = "invites";

    private DbOpenHelper sqlHelper;

    private UserManager() {
        //MLog.enable(TAG);
        sqlHelper = new DbOpenHelper(MyApp.getInstance(), null, null,
                DB_VERSION);
    }

    public static UserManager getInstance() {
        if (instance == null)
            instance = new UserManager();
        return instance;
    }

    public static void closeDb() {
        if (instance != null && instance.sqlHelper != null) {
            instance.sqlHelper.close();
            instance = null;
            MLog.i(TAG, "UserManager database closed..");
        }
    }

    public void invited(final Invite invite) {
        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final long rowId = db.replace(INVITES_TABLE_NAME, null,
                    invite.getContentValues());
            MLog.i(TAG, "Invite ", invite.getInstagramId(), " insert at ",
                    rowId);
        } catch (Throwable t) {
            MLog.e(TAG, "Error in storing invite: ", t);
        }
    }

    public void store(final User user) {
        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final long rowId = db.replace(USERS_TABLE_NAME, null,
                    user.getContentValues());
            MLog.i(TAG, "User ", user.getUsername(), " insert at ", rowId);
        } catch (Throwable t) {
            MLog.e(TAG, "Error in storing user: ", t);
        }
    }

    public int deleteAllUsers() {
        try {
            SQLiteDatabase db = sqlHelper.getWritableDatabase();
            int rowsDeleted = db.delete(USERS_TABLE_NAME, "1", null);
            MLog.i(TAG, "Deleted ", rowsDeleted, " users");
            return rowsDeleted;
        } catch (Throwable t) {
            MLog.e(TAG, "Error in deleting all users: ", t);
        }
        return 0;

    }

    public void deleteUser(final User user) {
        deleteUser(user.getInstagramId());
    }

    public void deleteUser(final String iid) {
        try {
            SQLiteDatabase db = sqlHelper.getWritableDatabase();
            int rowsDeleted = db.delete(USERS_TABLE_NAME,
                    UserColumns.INSTAGRAM_ID + " = " + iid, null);
            MLog.i(TAG, "Deleted ", rowsDeleted, " users");
        } catch (Throwable t) {
            MLog.e(TAG, "Error in deleting user: ", t);
        }
    }

    public synchronized void resetUnread(final String iid) {

        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final ContentValues values = new ContentValues();
            values.put(UserColumns.UNREAD, 0);

            // updating row
            final int rowsUpdated = db.update(USERS_TABLE_NAME, values,
                    UserColumns.INSTAGRAM_ID + " = " + iid, null);
            MLog.i(TAG, "UserManager resetUnread() update rows=", rowsUpdated);
        } catch (Throwable t) {
            MLog.e(TAG, "Error in decrementUnread: ", t);
        }

    }

    public synchronized void decrementUnread(final String iid) {

        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            db.execSQL(String.format(
                    "update %s set %s = %s - 1 where %s = %s AND %s > 0",
                    USERS_TABLE_NAME, UserColumns.UNREAD, UserColumns.UNREAD,
                    UserColumns.INSTAGRAM_ID, iid, UserColumns.UNREAD));
            MLog.i(TAG, "UserManager decrementUnread()");
        } catch (Throwable t) {
            MLog.e(TAG, "Error in decrementUnread: ", t);
        }
    }

    public synchronized void incrementUnread(final String iid) {

        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            db.execSQL(String.format("update %s set %s = %s + 1 where %s = %s",
                    USERS_TABLE_NAME, UserColumns.UNREAD, UserColumns.UNREAD,
                    UserColumns.INSTAGRAM_ID, iid));

            MLog.i(TAG, "UserManager incrementUnread()");
        } catch (Throwable t) {
            MLog.e(TAG, "Error in incrementUnread: ", t);
        }
    }

    public synchronized int getTotalUnread() {
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final Cursor cursor = db.rawQuery(String.format(
                    "select sum(%s) from %s ", UserColumns.UNREAD,
                    USERS_TABLE_NAME), null);
            cursor.moveToFirst();
            final int count = cursor.getInt(0);
            cursor.close();
            return count;
        } catch (final Exception e) {
            MLog.e(TAG, "Error in getTotalUnread: ", e);
        }
        return 0;
    }

    public synchronized void setInstalled(final List<String> list,
                                          final boolean isInstalled, final boolean isFollow) {

        if (list == null || list.size() == 0) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (final String iid : list) {
            sb.append("'").append(iid).append("'").append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(')');

        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();

            db.execSQL(String
                    .format("update %s set %s = %d where %s = %d",
                            USERS_TABLE_NAME, UserColumns.IS_INSTALLED, 0,
                            isFollow ? UserColumns.IS_FOLLOWS
                                    : UserColumns.IS_FOLLOWER, 1));

            db.execSQL(String.format("update %s set %s = %d where %s in %s",
                    USERS_TABLE_NAME, UserColumns.IS_INSTALLED, isInstalled ? 1
                            : 0, UserColumns.INSTAGRAM_ID, sb.toString()));

            MLog.i(TAG, "UserManager setInstalled(" + isInstalled);
        } catch (final Throwable t) {
            MLog.e(TAG, "Error in setInstalled (list): ", t);
        }
    }

    public User getUser(final String iid) {
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.INSTAGRAM_ID + "=" + iid);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            final ContentValues contentValues = new ContentValues();
            if (c.moveToNext()) {
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
                c.close();
                return new User(contentValues);
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getUser user: ", t);
        }
        return null;
    }

    public Vector<User> getFollowers() {

        final Vector<User> users = new Vector<User>();
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.IS_FOLLOWER + " = " + 1);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            final ContentValues contentValues = new ContentValues();
            while (c.moveToNext()) {
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
                users.add(new User(contentValues));
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting followers users : ", t);
        }
        return users;
    }

    public List<User> getStrictFollowers() {

        final List<User> users = new ArrayList<User>();
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.IS_FOLLOWER + " = " + 1 + " AND "
                    + UserColumns.IS_FOLLOWS + " = " + 0);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            final ContentValues contentValues = new ContentValues();
            while (c.moveToNext()) {
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
                users.add(new User(contentValues));
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting strict followers users : ", t);
        }
        return users;
    }

    public Vector<User> getFollows() {

        final Vector<User> users = new Vector<User>();
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.IS_FOLLOWS + " = " + 1);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            final ContentValues contentValues = new ContentValues();
            while (c.moveToNext()) {
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
                final User user = new User(contentValues);
                users.add(user);
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting following users : ", t);
        }
        return users;
    }

    public ArrayList<String> getFollowsIds() {

        final ArrayList<String> ids = new ArrayList<String>();
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.IS_FOLLOWS + " = " + 1);
            final Cursor c = qb.query(db,
                    new String[]{UserColumns.INSTAGRAM_ID}, null, null,
                    null, null, UserColumns.DEFAULT_SORT_ORDER);
            while (c.moveToNext()) {
                ids.add(c.getString(0));
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting following users : ", t);
        }
        return ids;
    }

    public boolean isFollow(String iid) {
        boolean isFollow = false;
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.INSTAGRAM_ID + "=" + iid + " AND "
                    + UserColumns.IS_FOLLOWS + " = " + 1);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            if (c.moveToNext()) {
                isFollow = true;
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting user: ", t);
        }
        return isFollow;
    }

    public boolean isFollower(String iid) {
        boolean isFollow = false;
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.INSTAGRAM_ID + "=" + iid + " AND "
                    + UserColumns.IS_FOLLOWER + " = " + 1);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            if (c.moveToNext()) {
                isFollow = true;
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting user: ", t);
        }
        return isFollow;
    }

    public boolean isStrictFollower(String iid) {
        boolean isFollow = false;
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.INSTAGRAM_ID + "=" + iid + " AND "
                    + UserColumns.IS_FOLLOWER + " = " + 1 + " AND "
                    + UserColumns.IS_FOLLOWS + " = " + 0);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            if (c.moveToNext()) {
                isFollow = true;
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting user: ", t);
        }
        return isFollow;
    }

    public boolean isFriend(String iid) {
        boolean isFriend = false;
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.INSTAGRAM_ID + "=" + iid + " AND "
                    + UserColumns.IS_FOLLOWS + " = " + 1 + " AND "
                    + UserColumns.IS_FOLLOWER + " = " + 1);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            if (c.moveToNext()) {
                isFriend = true;
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting user: ", t);
        }
        return isFriend;
    }

    public boolean isInvited(String iid) {
        boolean isInvited = false;
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(INVITES_TABLE_NAME);
            qb.appendWhere(UserColumns.INSTAGRAM_ID + "=" + iid);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    InvitesColumns.DEFAULT_SORT_ORDER);
            if (c.moveToNext()) {
                isInvited = true;
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting invite: ", t);
        }
        return isInvited;
    }

    public static final class UserColumns implements BaseColumns {

        // no instances please
        private UserColumns() {
        }

        public static final String ID = "id";
        public static final String INSTAGRAM_ID = "instagramId";
        public static final String INSTAGRAM_ACCESS_TOKEN = "iaccesstoken";
        public static final String USER_NAME = "username";
        public static final String PASSWORD = "password";
        public static final String FULL_NAME = "fullName";
        public static final String EMAIL = "email";
        public static final String LOCATION = "location";
        public static final String WEB_SITE = "website";
        public static final String PROFILE_PIC_URL = "profilePicUrl";
        public static final String AGE = "age";
        public static final String GENDER = "gender";
        public static final String BIO = "bio";
        public static final String IS_FOLLOWS = "isFollowing";
        public static final String IS_FOLLOWER = "isFollowedBy";
        public static final String IS_BLOCKED = "isBlocked";
        public static final String IS_STARRED = "isStarred";
        public static final String SHOW_LOCATION = "showlocation";
        public static final String PRIVILEGES = "privileges";
        public static final String FACEBOOK_ID = "fbid";
        public static final String FACEBOOK_ACCESS_TOKEN = "fbaccesstoken";
        public static final String TWITTER_ID = "tid";
        public static final String TWITTER_ACCESS_TOKEN = "taccesstoken";
        public static final String TWITTER_TOKEN_SECRET = "ttokensecret";
        public static final String TIME_STAMP = "tstamp";
        public static final String UNREAD = "unread";
        public static final String IS_INSTALLED = "isInstalled";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "fullName ASC";
    }

    public static final class InvitesColumns implements BaseColumns {

        // no instances please
        private InvitesColumns() {
        }

        public static final String ID = "id";
        public static final String INSTAGRAM_ID = "instagramId";
        public static final String TIME_STAMP = "tstamp";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "instagramId ASC";
    }

    private class DbOpenHelper extends SQLiteOpenHelper {

        public DbOpenHelper(Context context, String name,
                            CursorFactory factory, int version) {
            super(context, DATABASE_NAME, null, version);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL("CREATE TABLE " + USERS_TABLE_NAME + " ("
                        + UserColumns.ID
                        + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + UserColumns.INSTAGRAM_ID + " TEXT, "
                        + UserColumns.INSTAGRAM_ACCESS_TOKEN + " TEXT, "
                        + UserColumns.USER_NAME + " TEXT, "
                        + UserColumns.PASSWORD + " TEXT, "
                        + UserColumns.FULL_NAME + " TEXT, " + UserColumns.EMAIL
                        + " TEXT, " + UserColumns.LOCATION + " TEXT, "
                        + UserColumns.WEB_SITE + " TEXT, "
                        + UserColumns.PROFILE_PIC_URL + " TEXT, "
                        + UserColumns.AGE + " INTEGER, " + UserColumns.GENDER
                        + " TEXT, " + UserColumns.BIO + " TEXT, "
                        + UserColumns.IS_FOLLOWS + " INTEGER DEFAULT 0, "
                        + UserColumns.IS_FOLLOWER + " INTEGER DEFAULT 0, "
                        + UserColumns.IS_BLOCKED + " INTEGER DEFAULT 0, "
                        + UserColumns.IS_STARRED + " INTEGER DEFAULT 0, "
                        + UserColumns.SHOW_LOCATION + " INTEGER DEFAULT 0, "
                        + UserColumns.PRIVILEGES + " TEXT, "
                        + UserColumns.FACEBOOK_ID + " TEXT, "
                        + UserColumns.FACEBOOK_ACCESS_TOKEN + " TEXT, "
                        + UserColumns.TWITTER_ID + " TEXT, "
                        + UserColumns.TWITTER_ACCESS_TOKEN + " TEXT, "
                        + UserColumns.TWITTER_TOKEN_SECRET + " TEXT, "
                        + UserColumns.UNREAD + " INTEGER DEFAULT 0, "
                        + UserColumns.IS_INSTALLED + " INTEGER DEFAULT 0, "
                        + UserColumns.TIME_STAMP + " TIMESTAMP" + ");");

            } catch (Throwable t) {
                MLog.e(TAG, "Error in creating users table: ", t);
            }

            try {
                db.execSQL("CREATE TABLE " + INVITES_TABLE_NAME + " ("
                        + InvitesColumns.ID
                        + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + InvitesColumns.INSTAGRAM_ID + " TEXT, "
                        + InvitesColumns.TIME_STAMP + " TIMESTAMP" + ");");

            } catch (Throwable t) {
                MLog.e(TAG, "Error in creating invites table: ", t);
            }
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                              final int newVersion) {
            if (newVersion == 4) {
                try {
                    db.execSQL("ALTER TABLE " + USERS_TABLE_NAME + " ADD "
                            + UserColumns.IS_INSTALLED + " INTEGER DEFAULT 0");
                    MLog.i(TAG, "UserManager onUpgrade oldVersion=",
                            oldVersion, " newVersion=", newVersion);
                } catch (final Exception e) {
                    MLog.e(TAG, "Error in altering users table: ", e);
                }
            }
        }
    }

    public boolean isBlocked(String iid) {
        boolean isBlocked = false;
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(USERS_TABLE_NAME);
            qb.appendWhere(UserColumns.INSTAGRAM_ID + "=" + iid + " AND "
                    + UserColumns.IS_BLOCKED + " = " + 1);
            final Cursor c = qb.query(db, null, null, null, null, null,
                    UserColumns.DEFAULT_SORT_ORDER);
            if (c.moveToNext()) {
                isBlocked = true;
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting user: ", t);
        }
        return isBlocked;
    }

}
