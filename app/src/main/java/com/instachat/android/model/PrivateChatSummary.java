package com.instachat.android.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kevin on 9/24/2016.
 */
public class PrivateChatSummary implements Comparable {

    private static final String TAG = "PrivateChatSummary";

    /**
     * The 'id' of the PrivateChatSummary is the same as the id
     * of the person whom you are having the conversation with
     */
    private String id;

    /**
     * information of the user with whom you are chatting with
     */
    private String name;
    private String dpid;
    private String imageUrl;
    private long lastMessageSentTimestamp;
    private String lastMessage;
    private int unreadMessageCount = -1;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDpid() {
        return dpid;
    }

    public void setDpid(String dpid) {
        this.dpid = dpid;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getLastMessageSentTimestamp() {
        return lastMessageSentTimestamp;
    }

    public void setLastMessageSentTimestamp(long timestamp) {
        this.lastMessageSentTimestamp = timestamp;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadMessageCount() {
        return unreadMessageCount;
    }

    public void setUnreadMessageCount(int unreadMessageCount) {
        this.unreadMessageCount = unreadMessageCount;
    }

    /**
     * @return returns only the fields you want
     * to update in cloud database
     */
    public static Map<String, Object> toMap(PrivateChatSummary privateChatSummary) {
        Map<String, Object> map = new HashMap<>(8);
        if (privateChatSummary.getName() != null)
            map.put("name", privateChatSummary.getName());
        if (privateChatSummary.getDpid() != null)
            map.put("dpid", privateChatSummary.getDpid());
        if (privateChatSummary.getImageUrl() != null)
            map.put("imageUrl", privateChatSummary.getImageUrl());
        if (privateChatSummary.getLastMessage() != null)
            map.put("lastMessage", privateChatSummary.getLastMessage());
        if (privateChatSummary.getLastMessageSentTimestamp() != 0) {
            map.put("lastMessageSentTimestamp", privateChatSummary.getLastMessageSentTimestamp());
        }
        if (privateChatSummary.getUnreadMessageCount() >= 0) {
            map.put("unreadMessageCount", privateChatSummary.getUnreadMessageCount());
        }
        return map;
    }

    @Override
    public int compareTo(Object o) {
        PrivateChatSummary other = (PrivateChatSummary)o;

        if (getLastMessageSentTimestamp() != 0 || other.getLastMessageSentTimestamp() != 0) {
            if (getLastMessageSentTimestamp() > other.getLastMessageSentTimestamp()) {
                return -1;
            } else if (getLastMessageSentTimestamp() < other.getLastMessageSentTimestamp()) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return getName().compareTo(other.getName());
        }
    }
}
