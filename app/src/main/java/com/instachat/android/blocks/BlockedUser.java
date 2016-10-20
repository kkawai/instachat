package com.instachat.android.blocks;

import java.util.HashMap;
import java.util.Map;

public final class BlockedUser {

    public BlockedUser(int id, String name, String dpid) {
        this.name = name;
        this.dpid = dpid;
        this.id = id;
    }

    public BlockedUser(){}

    String name;
    String dpid;
    public int id;

    public String getName() {
        return name;
    }

    public String getDpid() {
        return dpid;
    }

    public Map<String,Object> getUpdateMap() {
        Map<String,Object> map = new HashMap<>(2);
        map.put("name",name);
        if (dpid != null)
            map.put("dpid",dpid);
        return map;
    }
}