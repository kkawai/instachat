package com.instachat.android.model;

import com.google.firebase.database.DataSnapshot;
import com.instachat.android.util.MLog;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kevin on 9/24/2016.
 */
public class GroupChatSummary implements Comparable<GroupChatSummary> {

    private static final String TAG = "GroupChatSummary";

    private long id;

    private String name;
    private String dpid;
    private String imageUrl;
    private long lastMessageTime;
    private String lastMessage;
    private int unreadMessageCount = -1;
    private int usersInRoomCount;
    private String description;
    private int totalLikes;
    private int order;

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setUnreadMessageCount(int unreadMessageCount) {
        this.unreadMessageCount = unreadMessageCount;
    }

    public int getUsersInRoomCount() {
        return usersInRoomCount;
    }

    public void setUsersInRoomCount(int usersInRoomCount) {
        if (usersInRoomCount < 0)
            return;
        this.usersInRoomCount = usersInRoomCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return returns only the fields you want
     * to update in cloud database
     */
    public Map<String, Object> getUpdateMap() {
        Map<String, Object> map = new HashMap<>(8);
        if (getName() != null)
            map.put("name", getName());
        if (getDpid() != null)
            map.put("dpid", getDpid());
        if (getImageUrl() != null)
            map.put("imageUrl", getImageUrl());
        if (getLastMessage() != null)
            map.put("lastMessage", getLastMessage());
        if (getLastMessageTime() != 0) {
            map.put("lastMessageTime", getLastMessageTime());
        }
        if (getUnreadMessageCount() >= 0) {
            map.put("unreadMessageCount", getUnreadMessageCount());
        }
        map.put("usersInRoomCount", usersInRoomCount);
        if (totalLikes != 0)
            map.put("totalLikes", totalLikes);
        return map;
    }

    public int getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(int totalLikes) {
        this.totalLikes = totalLikes;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GroupChatSummary)) {
            return false;
        }
        GroupChatSummary other = (GroupChatSummary)obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return (int)id;
    }

    @Override
    public int compareTo(GroupChatSummary groupChatSummary) {
        if (this.order < groupChatSummary.order) {
            return -1;
        } else if (this.order > groupChatSummary.order) {
            return 1;
        } else {
            return this.name.compareTo(groupChatSummary.name);
        }
    }
}
