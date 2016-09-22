package com.instachat.android.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import com.instachat.android.MyApp;
import com.instachat.android.model.Rss;
import com.instachat.android.util.MLog;
import com.instachat.android.util.StringUtil;

import java.util.ArrayList;

/**
 * @author kkawai
 */
public final class RssDb {

    private static final String TAG = "RssDb";
    private static final int DB_VERSION = 2;
    private static RssDb instance;
    private static final String TABLE_NAME = "rss";

    private DbOpenHelper sqlHelper;

    private RssDb(final Context context) {
        sqlHelper = new DbOpenHelper(context, null, null, DB_VERSION);
    }

    public synchronized static RssDb getInstance() {
        if (instance == null)
            instance = new RssDb(MyApp.getInstance());
        return instance;
    }

    public synchronized static void closeDb() {
        if (instance != null && instance.sqlHelper != null) {
            instance.sqlHelper.close();
            instance = null;
            MLog.i(TAG, "database closed");
        }
    }

    public static final class RssColumns implements BaseColumns {

        // no instances please
        private RssColumns() {
        }

        public static final String COL_ID = "id";
        public static final String COL_AUTHOR = "author";
        public static final String COL_CATEGORY = "category";
        public static final String COL_ORIGINAL_CATEGORY = "original_category";
        public static final String COL_DESCR = "descr";
        public static final String COL_IMAGE_URL = "image_url";
        public static final String COL_LINK = "link";
        public static final String COL_LINK_BASIC = "basic_link";
        public static final String COL_PUB_DATE = "pub_date";
        public static final String COL_TITLE = "title";
        public static final String COL_VIDEO_URL = "video_url";

        /**
         * The default sort order for this table
         */
        private static final String DEFAULT_SORT_ORDER = COL_PUB_DATE + " DESC";

    }

    public static final String getDbName() {
        return "rss.db";
    }

    private class DbOpenHelper extends SQLiteOpenHelper {

        public DbOpenHelper(final Context context, final String name, final CursorFactory factory, final int version) {
            super(context, getDbName(), null, version);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            try {
                db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + RssColumns.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + RssColumns.COL_AUTHOR + " TEXT, "
                        + RssColumns.COL_CATEGORY + " TEXT, "
                        + RssColumns.COL_ORIGINAL_CATEGORY + " TEXT, "
                        + RssColumns.COL_DESCR + " TEXT, "
                        + RssColumns.COL_IMAGE_URL + " TEXT, "
                        + RssColumns.COL_LINK + " TEXT, "
                        + RssColumns.COL_LINK_BASIC + " TEXT, "
                        + RssColumns.COL_PUB_DATE + " INTEGER DEFAULT 0, "
                        + RssColumns.COL_TITLE + " TEXT, "
                        + RssColumns.COL_VIDEO_URL + " TEXT);");
            } catch (final Throwable t) {
                MLog.e(TAG, "Error creating database.  Very bad: ", t);
            }

//         try {
//            db.execSQL(String.format("CREATE INDEX %s ON %s(%s);", TABLE_NAME + "_artist_name_index", TABLE_NAME, RssColumns.COL_ARTIST_NAME));
//         } catch (final Throwable t) {
//            MLog.e(TAG, "Error creating database index: ", t);
//         }
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            if (newVersion == DB_VERSION) {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD "
                            + RssColumns.COL_ORIGINAL_CATEGORY + " TEXT");
                    MLog.i(TAG, TABLE_NAME + " table upgraded. onUpgrade() oldVersion=",
                            oldVersion, " newVersion=", newVersion);
                } catch (final Exception e) {
                    MLog.e(TAG, "Error in altering users table: ", e);
                }
            }
        }
    }

    /**
     * @param rss
     */
    public synchronized void insertRss(final Rss rss) {

        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final long rowId = db.replace(TABLE_NAME, null, rss.getContentValues());
            MLog.i(TAG, "rss: " + rss + " inserted at " + rowId + " [" + rss.getCategory() + "]");
        } catch (Throwable t) {
            Log.e(TAG, "Error in storing rss: ", t);
        }
    }

    public synchronized void insertRss(final String originalCategory, final ArrayList<Rss> rssList) {
        for (int i = 0; i < rssList.size(); i++) {
            final Rss rss = rssList.get(i);
            rss.setOriginalCategory(originalCategory);
            insertRss(rss);
        }
    }

    public synchronized int deleteAll() {
        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            final int rowsDeleted = db.delete(TABLE_NAME, "1", null);
            return rowsDeleted;
        } catch (final Throwable t) {
            MLog.e(TAG, "Error in deleting all rss: ", t);
        }
        return 0;
    }

    public synchronized ArrayList<Rss> getRss(final String originalCategory) {

        final ArrayList<Rss> rssList = new ArrayList<>();
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            //final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            //qb.setTables(TABLE_NAME);
            final String sql = String.format("select * from " + TABLE_NAME + "  order by " + RssColumns.DEFAULT_SORT_ORDER);
            final Cursor c = db.rawQuery(sql, null);
            final ContentValues contentValues = new ContentValues();
            while (c.moveToNext()) {
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
                final Rss rss = new Rss(contentValues);
                if (rss.getOriginalCategory().equals(originalCategory))
                    rssList.add(rss);
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting rss: ", t);
        }
        return rssList;
    }

    public synchronized Rss getRssByLink(final String link) {

        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            //final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            //qb.setTables(TABLE_NAME);
            final String sql = String.format("select * from " + TABLE_NAME
                    + " where " + RssColumns.COL_LINK + " = '" + link + "'");
            final Cursor c = db.rawQuery(sql, null);
            final ContentValues contentValues = new ContentValues();
            if (c.moveToNext()) {
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
                Rss rss = new Rss(contentValues);
                return rss;
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting rss: ", t);
        }
        return null;
    }

    public synchronized int deleteRss(final String originalCategory) {

        final ArrayList<Rss> rssList = getRss(originalCategory);
        int deleteCount = 0;
        try {
            final SQLiteDatabase db = sqlHelper.getWritableDatabase();
            for (int i = 0; i < rssList.size(); i++) {
                final Rss rss = rssList.get(i);
                deleteCount += db.delete(TABLE_NAME, RssColumns.COL_ID + "=" + rss.getId(), null);
            }
        } catch (final Throwable t) {
            MLog.e(TAG, "Error deleting rss: ", t);
        }
        return deleteCount;
    }

    public synchronized ArrayList<String> getOriginalCategories() {

        final ArrayList<String> categories = new ArrayList<>();
        try {
            final SQLiteDatabase db = sqlHelper.getReadableDatabase();
            final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(TABLE_NAME);
            final String sql = String.format("select distinct(%s) from %s",
                    RssColumns.COL_ORIGINAL_CATEGORY, TABLE_NAME);
            final Cursor c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                categories.add(StringUtil.unescapeQuotes(c.getString(0)));
            }
            c.close();
        } catch (Throwable t) {
            MLog.e(TAG, "Error in getting rss categories: ", t);
        }
        return categories;
    }
}
