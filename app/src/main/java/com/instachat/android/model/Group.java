package com.instachat.android.model;

/**
 * Created by kevin on 9/22/2016.
 */
public class Group {

    private static final String TAG = "Group";

    private String id;
    private String name;
    private String imageUrl;
    private String description;
    private String category;
    private String lastMessage;
    private String lastMessageTime;
    private long time;
    private int ownerUserid;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getOwnerUserid() {
        return ownerUserid;
    }

    public void setOwnerUserid(int ownerUserid) {
        this.ownerUserid = ownerUserid;
    }
}
