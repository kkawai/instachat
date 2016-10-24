package com.instachat.android.adapter;

import com.ath.fuel.AppSingleton;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by kevin on 10/23/2016.
 * <p>
 */
@AppSingleton
public class MessagesRecyclerAdapterHelper {
    private final Map<String, Integer> mConsumedLikesMap = new Hashtable<>(1000);

    public Map<String, Integer> getConsumedLikesMap() {
        return mConsumedLikesMap;
    }
}
