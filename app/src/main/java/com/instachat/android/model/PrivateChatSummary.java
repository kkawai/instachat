package com.instachat.android.model;

import com.instachat.android.Constants;

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
    private int onlineStatus = -1;
    private long lastOnline;
    private boolean accepted;

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

    public int getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(int onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    /**
     * @return returns only the fields you want
     * to update in cloud database
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(8);
        if (getName() != null)
            map.put("name", getName());
        if (getDpid() != null)
            map.put("dpid", getDpid());
        if (getImageUrl() != null)
            map.put("imageUrl", getImageUrl());
        if (getLastMessage() != null)
            map.put("lastMessage", getLastMessage());
        if (getLastMessageSentTimestamp() != 0) {
            map.put(Constants.FIELD_LAST_MESSAGE_SENT_TIMESTAMP, getLastMessageSentTimestamp());
        }
        if (getUnreadMessageCount() >= 0) {
            map.put("unreadMessageCount", getUnreadMessageCount());
        }
        if (getOnlineStatus() >= 0) {
            map.put("onlineStatus", getOnlineStatus());
        }
        if (isAccepted()) {
            map.put(Constants.CHILD_ACCEPTED, isAccepted());
        }
        return map;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PrivateChatSummary)) {
            return false;
        }
        PrivateChatSummary other = (PrivateChatSummary) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Integer.parseInt(id);
    }

    @Override
    public int compareTo(Object o) {
        PrivateChatSummary other = (PrivateChatSummary) o;

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
