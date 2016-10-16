package com.instachat.android.model;

import com.brandongogetap.stickyheaders.exposed.StickyHeader;

/**
 * Created by kevin on 9/26/2016.
 */
public class GroupChatHeader implements StickyHeader {
    public GroupChatHeader(String name) {
        this.name = name;
    }

    public String name;
}
