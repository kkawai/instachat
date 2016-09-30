package com.instachat.android.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kevin on 9/24/2016.
 */
public class PrivateChatSummary {

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
    private long lastMessageTime;
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

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
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
        if (privateChatSummary.getLastMessageTime() != 0) {
            map.put("lastMessageTime", privateChatSummary.getLastMessageTime());
        }
        if (privateChatSummary.getUnreadMessageCount() >= 0) {
            map.put("unreadMessageCount", privateChatSummary.getUnreadMessageCount());
        }
        return map;
    }
}
