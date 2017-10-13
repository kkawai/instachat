package com.instachat.android.app.adapter;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by kevin on 10/23/2016.
 * <p>
 */
public class MessagesRecyclerAdapterHelper {
    private final Map<String, Integer> mConsumedLikesMap = new Hashtable<>(1000);

    public Map<String, Integer> getConsumedLikesMap() {
        return mConsumedLikesMap;
    }
}
