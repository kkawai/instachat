package com.instachat.android.model;

import android.content.ContentValues;
import android.database.DatabaseUtils;

import com.instachat.android.db.RssDb;
import com.instachat.android.util.StringUtil;
import com.instachat.android.util.TimeUtil;

import java.util.Date;

/**
 * Created by kevin on 1/10/2016.
 */
public class Rss {

    private int id;
    private String title, link, basicLink, descr, imageUrl, videoUrl, category, author, originalCategory;
    private long pubDate;//Sat, 07 Sep 2002 00:00:01 GMT

    public static final String KEY_CATEGORY = "cat";
    public static final String KEY_ORIGINAL_CATEGORY = "o_cat";
    public static final String KEY_TITLE = "title";
    public static final String KEY_IMAGE = "image";

    public Rss() {
    }

    public Rss(final ContentValues contentValues) {
        final Integer id = contentValues.getAsInteger(RssDb.RssColumns.COL_ID);
        if (id != null) {
            setId(id);
        }
        setTitle(StringUtil.unescapeQuotes(contentValues.getAsString(RssDb.RssColumns.COL_TITLE)));
        setLink(contentValues.getAsString(RssDb.RssColumns.COL_LINK));
        setBasicLink(contentValues.getAsString(RssDb.RssColumns.COL_LINK_BASIC));
        setDescr(StringUtil.unescapeQuotes(contentValues.getAsString(RssDb.RssColumns.COL_DESCR)));
        setImageUrl(contentValues.getAsString(RssDb.RssColumns.COL_IMAGE_URL));
        setVideoUrl(contentValues.getAsString(RssDb.RssColumns.COL_VIDEO_URL));
        setCategory(StringUtil.unescapeQuotes(contentValues.getAsString(RssDb.RssColumns.COL_CATEGORY)));
        setOriginalCategory(StringUtil.unescapeQuotes(contentValues.getAsString(RssDb.RssColumns.COL_ORIGINAL_CATEGORY)));
        setAuthor(StringUtil.unescapeQuotes(contentValues.getAsString(RssDb.RssColumns.COL_AUTHOR)));
        setPubDate(contentValues.getAsLong(RssDb.RssColumns.COL_PUB_DATE));
    }

    public ContentValues getContentValues() {
        final ContentValues values = new ContentValues();
        if (id != 0) {
            values.put(RssDb.RssColumns.COL_ID, id);
        }
        values.put(RssDb.RssColumns.COL_TITLE, DatabaseUtils.sqlEscapeString(getTitle()));
        values.put(RssDb.RssColumns.COL_LINK, getLink());
        values.put(RssDb.RssColumns.COL_LINK_BASIC, getBasicLink());
        values.put(RssDb.RssColumns.COL_DESCR, DatabaseUtils.sqlEscapeString(getDescr()));
        values.put(RssDb.RssColumns.COL_IMAGE_URL, getImageUrl());
        values.put(RssDb.RssColumns.COL_VIDEO_URL, getVideoUrl());
        values.put(RssDb.RssColumns.COL_CATEGORY, DatabaseUtils.sqlEscapeString(getCategory()));
        values.put(RssDb.RssColumns.COL_ORIGINAL_CATEGORY, DatabaseUtils.sqlEscapeString(getOriginalCategory()));
        values.put(RssDb.RssColumns.COL_AUTHOR, DatabaseUtils.sqlEscapeString(getAuthor()));
        values.put(RssDb.RssColumns.COL_PUB_DATE, getPubDate());
        return values;
    }


    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescr() {
        return descr;
    }

    private static String IMG_TAG = "<img src=";
    private static String P_TAG = "<p>";

    public void setDescr(final String descr) {
        if (descr.contains(IMG_TAG)) {
            int i = descr.indexOf(IMG_TAG);
            String x = descr.substring(i + IMG_TAG.length() + 1, descr.length());
            imageUrl = x.substring(0, x.indexOf('"'));
        }
        if (descr.contains(P_TAG)) {
            int i = descr.indexOf(P_TAG);
            String x = descr.substring(i + P_TAG.length(), descr.length());
            this.descr = x.substring(0, x.indexOf("</p>"));
        } else {
            this.descr = descr;
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getPubDate() {
        return pubDate;
    }

    public void setPubDate(long pubDate) {
        this.pubDate = pubDate;
    }

    public String getTitle() {

        return title;
    }

    public String getAuthor() {
        return author != null ? author : "";
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTimeAgo() {
        if (pubDate == 0) return "now";
        return TimeUtil.getTimeAgo(new Date(pubDate));
    }

    public String getBasicLink() {
        return basicLink;
    }

    public void setBasicLink(String basicLink) {
        this.basicLink = basicLink;
    }

    @Override
    public String toString() {
        return "RSS. time ago: [" + getTimeAgo() + "] author: [" + author + "] title: [" + title + "] link: [" + link + "] descr: [" + descr + "] imageUrl: [" + imageUrl + "] videoUrl: [" + videoUrl + "] category: [" + category + "] pubDate: [" + pubDate + "]";
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getOriginalCategory() {
        return originalCategory;
    }

    public void setOriginalCategory(String originalCategory) {
        this.originalCategory = originalCategory;
    }
}