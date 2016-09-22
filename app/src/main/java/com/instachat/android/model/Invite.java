package com.instachat.android.model;

import android.content.ContentValues;


import java.sql.Timestamp;
import java.util.Date;

public class Invite extends DomainObject {

    private static final long serialVersionUID = 2546919512298413613L;

    private int id;
    private String instagramId;
    private Timestamp tstamp = new Timestamp(new Date().getTime());

    public Invite() {
        super();
    }

    public Invite(ContentValues contentValues) {
        Integer idVal = contentValues.getAsInteger(UserManager.InvitesColumns.ID);
        if (idVal != null)
            id = idVal.intValue();
        instagramId = contentValues.getAsString(UserManager.InvitesColumns.INSTAGRAM_ID);
        tstamp = new Timestamp(contentValues.getAsLong(UserManager.InvitesColumns.TIME_STAMP));
    }

    public ContentValues getContentValues() {

        ContentValues values = new ContentValues();
        if (id != 0)
            values.put(UserManager.UserColumns.ID, id);
        values.put(UserManager.UserColumns.INSTAGRAM_ID, instagramId);
        values.put(UserManager.UserColumns.TIME_STAMP, tstamp.getTime());
        return values;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInstagramId() {
        return instagramId;
    }

    public void setInstagramId(String id) {
        this.instagramId = id;
    }

    public Timestamp getTstamp() {
        return tstamp;
    }

    public void setTstamp(Timestamp tstamp) {
        this.tstamp = tstamp;
    }
}
