package com.instachat.android.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.instachat.android.TheApp;
import com.instachat.android.util.MLog;
import com.instachat.android.util.UserPreferences;

/**
 * @author kkawai
 */
public final class OneTimeMessageDb {

    private static final String TAG = "OneTimeMessageDb";
    private static final int DB_VERSION = 1;
    private static OneTimeMessageDb instance;
    private static final String TABLE_NAME = "one_time_message";

    private DbOpenHelper sqlHelper;

    private OneTimeMessageDb(final Context context) {
        sqlHelper = new DbOpenHelper(context, null, null, DB_VERSION);
    }

    public synchronized static OneTimeMessageDb getInstance() {
        if (instance == null)
            instance = new OneTimeMessageDb(TheApp.getInstance());
        return instance;
    }

    public synchronized static void closeDb() {
        if (instance != null && instance.sqlHelper != null) {
            instance.sqlHelper.close();
            instance = null;
            MLog.i(TAG, "database closed");
        }
    }

    private static final class DbColumns implements BaseColumns {

        // no instances please
        private DbColumns() {
        }

        public static final String COL_ID = "id";
        public static final String COL_MESSAGE_ID = "msg_id";

    }

    private static final String getDbName() {
        return UserPreferences.getInstance().getUserId() + ".db";
    }

    private class DbOpenHelper extends SQLiteOpenHelper {

        public DbOpenHelper(final Context context, final String name, final CursorFactory factory, final int version) {
            super(context, getDbName(), null, version);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            try {
                db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + DbColumns.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + DbColumns.COL_MESSAGE_ID + " TEXT);");
            } catch (final Throwable t) {
                MLog.e(TAG, "Error creating database.  Very bad: ", t);
            }

            try {
                db.execSQL(String.format("CREATE INDEX %s ON %s(%s);", TABLE_NAME + "_msg_id_index", TABLE_NAME, DbColumns.COL_MESSAGE_ID));
            } catch (final Throwable t) {
                MLog.e(TAG, "Error creating database index: ", t);
            }
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
//            if (newVersion == DB_VERSION) {
//                try {
//                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD "
//                            + DbColumns.COL_ORIGINAL_CATEGORY + " TEXT");
//                    MLog.i(TAG, TABLE_NAME + " table upgraded. onUpgrade() oldVersion=",
//                            oldVersion, " newVersion=", newVersion);
//                } catch (final Exception e) {
//                    MLog.e(TAG, "Error in altering users table: ", e);
//                }
//            }
        }
    }

    /**
     * @param messageId
     */
    public synchronized void insertMessageId(final String messageId) {

        ContentValues contentValues = new ContentValues();
        contentValues.put(DbColumns.COL_MESSAGE_ID, messageId);

        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final long rowId = db.replace(TABLE_NAME, null, contentValues);
            MLog.i(TAG, "insertMessageId inserted at " + rowId + " [" + messageId + "]");
        } catch (Throwable t) {
            Log.e(TAG, "Error in storing message id: ", t);
        }
    }

    public synchronized int deleteAll() {
        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final int rowsDeleted = db.delete(TABLE_NAME, "1", null);
            return rowsDeleted;
        } catch (final Throwable t) {
            MLog.e(TAG, "Error in deleting all records: ", t);
        }
        return 0;
    }

    private static final String QUERY_COUNT = "select count(*) from " + TABLE_NAME
            + " where " + DbColumns.COL_MESSAGE_ID + " = '%s'";

    public synchronized boolean messageExists(final String messageId) {

        int count = 0;
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final Cursor c = db.rawQuery(String.format(QUERY_COUNT, messageId), null);
            if (c.moveToNext()) {
                count = c.getInt(0);
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting rss: ", t);
        }
        return count > 0;
    }
}
