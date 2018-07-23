package com.instachat.android.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.instachat.android.TheApp;
import com.instachat.android.data.model.User;
import com.instachat.android.util.MLog;

import java.util.ArrayList;

/**
 * @author kkawai
 */
public final class DmDb {

    private static final String TAG = "DmDb";
    private static final int DB_VERSION = 2;
    private static DmDb instance;
    private static final String TABLE_NAME = "dm";
    private static final int MAX = 100;

    private DbOpenHelper sqlHelper;

    private DmDb(final Context context) {
        sqlHelper = new DbOpenHelper(context, null, null, DB_VERSION);
    }

    public synchronized static DmDb getInstance() {
        if (instance == null)
            instance = new DmDb(TheApp.getInstance());
        return instance;
    }

    public synchronized static void closeDb() {
        if (instance != null && instance.sqlHelper != null) {
            instance.sqlHelper.close();
            instance = null;
            MLog.i(TAG, "database closed");
        }
    }

    private static final class DmColumns implements BaseColumns {

        // no instances please
        private DmColumns() {
        }

        public static final String COL_ID = "db_id";
        public static final String COL_USERID = "userid";
        public static final String COL_NAME = "username";
        public static final String COL_DP = "dp";
        public static final String COL_UNREAD_MSG_COUNT = "unread_count";
        public static final String COL_TIMESTAMP = "ts";

        /**
         * The default sort order for this table
         */
        private static final String DEFAULT_SORT_ORDER = COL_TIMESTAMP + " DESC";

    }

    public static final String getDbName() {
        return "dm.db";
    }

    private class DbOpenHelper extends SQLiteOpenHelper {

        public DbOpenHelper(final Context context, final String name, final CursorFactory factory, final int version) {
            super(context, getDbName(), null, version);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            try {
                db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + DmColumns.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + DmColumns.COL_NAME + " TEXT, "
                        + DmColumns.COL_USERID + " TEXT, "
                        + DmColumns.COL_DP + " TEXT, "
                        + DmColumns.COL_UNREAD_MSG_COUNT + " INTEGER DEFAULT 0,"
                        + DmColumns.COL_TIMESTAMP + " INTEGER DEFAULT 0); ");
            } catch (final Throwable t) {
                MLog.e(TAG, "Error creating database.  Very bad: ", t);
            }

            try {
                db.execSQL(String.format("CREATE INDEX %s ON %s(%s);", TABLE_NAME + "_userid_index", TABLE_NAME, DmColumns.COL_USERID));
            } catch (final Throwable t) {
                MLog.e(TAG, "Error creating database index: ", t);
            }
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
//            if (newVersion == DB_VERSION) {
//                try {
//                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD "
//                            + COL_NEW_SHIT + " TEXT");
//                    MLog.i(TAG, TABLE_NAME + " table upgraded. onUpgrade() oldVersion=",
//                            oldVersion, " newVersion=", newVersion);
//                } catch (final Exception e) {
//                    MLog.e(TAG, "Error in altering users table: ", e);
//                }
//            }
        }
    }

    public void insertUser(User user) {

        User existing = getUserBy(user.getId() + "");
        if (existing != null) {
            updateUser(user);
            return;
        }

        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final long rowId = db.replace(TABLE_NAME, null, contentValuesForInsert(user));
            MLog.i(TAG, "inserted dm: rowId: " + rowId);
            deleteMax();
        } catch (Throwable t) {
            Log.e(TAG, "Error in storing dm: ", t);
        }
    }

    public void updateUser(User user) {
        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            String args[] = {user.getId() + ""};
            final ContentValues values = new ContentValues();
            values.put(DmColumns.COL_NAME, user.getUsername() + "");
            values.put(DmColumns.COL_DP, user.getProfilePicUrl() + "");
            values.put(DmColumns.COL_TIMESTAMP, System.currentTimeMillis());
            values.put(DmColumns.COL_UNREAD_MSG_COUNT, user.getUnreadMessageCount());
            int count = db.update(TABLE_NAME, values, DmColumns.COL_USERID + " = ?", args);
            MLog.d(TAG, "updated user count: " + count + " userId: " + user.getUsername());
        } catch (final Throwable t) {
            MLog.e(TAG, "updateUser: ", t);
        }
    }

    public User getUserBy(String userId) {

        User user = null;
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final String sql = String.format("select * from " + TABLE_NAME
                    + " where " + DmColumns.COL_USERID + " = '" + userId + "'");
            final Cursor c = db.rawQuery(sql, null);
            final ContentValues contentValues = new ContentValues();
            if (c.moveToNext()) {
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
                user = from(contentValues);
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting rss: ", t);
        }
        return user;
    }

    private ContentValues contentValuesForInsert(User user) {
        final ContentValues values = new ContentValues();
        values.put(DmColumns.COL_NAME, user.getUsername() + "");
        values.put(DmColumns.COL_USERID, user.getId() + "");
        values.put(DmColumns.COL_DP, user.getProfilePicUrl() + "");
        values.put(DmColumns.COL_TIMESTAMP, System.currentTimeMillis());
        return values;
    }

    private User from(final ContentValues contentValues) {
        User user = new User();
        String id = contentValues.getAsString(DmColumns.COL_USERID);
        if (id != null) {
            user.setId(Integer.parseInt(id));
        }
        user.setUsername(contentValues.getAsString(DmColumns.COL_NAME));
        user.setProfilePicUrl(contentValues.getAsString(DmColumns.COL_DP));
        user.setUsername(contentValues.getAsString(DmColumns.COL_NAME));
        user.setUnreadMessageCount(contentValues.getAsInteger(DmColumns.COL_UNREAD_MSG_COUNT));
        return user;
    }

    public int deleteAll() {
        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final int rowsDeleted = db.delete(TABLE_NAME, "1", null);
            return rowsDeleted;
        } catch (final Throwable t) {
            MLog.e(TAG, "Error in deleting all rss: ", t);
        }
        return 0;
    }

    public ArrayList<User> getUsers() {

        final ArrayList<User> list = new ArrayList<>(MAX);
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            //final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            //qb.setTables(TABLE_NAME);
            final String sql = String.format("select * from " + TABLE_NAME + "  order by " + DmColumns.DEFAULT_SORT_ORDER);
            final Cursor c = db.rawQuery(sql, null);
            final ContentValues contentValues = new ContentValues();
            while (c.moveToNext()) {
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
                final User user = from(contentValues);
                list.add(user);
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting rss: ", t);
        }
        return list;
    }

    public int deleteUser(final String userId) {

        int deleteCount = 0;
        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            int count = db.delete(TABLE_NAME, DmColumns.COL_USERID + " = ?", new String[]{userId});
            MLog.d(TAG, "deleted user count: " + count + " userId: " + userId);
        } catch (final Throwable t) {
            MLog.e(TAG, "Error deleting rss: ", t);
        }
        return deleteCount;
    }

    private void deleteMax() {
        int count = 0;
        final SQLiteDatabase readDb = sqlHelper.getReadableDatabase();
        try {
            final String sql = String.format("select count(*) from " + TABLE_NAME);
            final Cursor c = readDb.rawQuery(sql, null);
            if (c != null) {
                c.moveToFirst();
                count = c.getInt(0);
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "deleteMax(1): ", t);
        }
        MLog.d(TAG, "current count: " + count + " MAX: " + MAX);
        if (count > MAX) {
            try {
                long minTimestamp = 0;
                final String sql = String.format("select min(ts) from " + TABLE_NAME);
                final Cursor c = readDb.rawQuery(sql, null);
                if (c != null) {
                    c.moveToFirst();
                    minTimestamp = c.getLong(0);
                }
                c.close();
                final SQLiteDatabase writeDb = sqlHelper.getWritableDatabase();
                int delCount = writeDb.delete(TABLE_NAME, DmColumns.COL_TIMESTAMP + " = ?", new String[]{minTimestamp + ""});
                MLog.d(TAG, "deleteMax delete count: " + delCount);
            } catch (Throwable t) {
                MLog.e(TAG, "deleteMax(1): ", t);
            }
        }
    }

}
