package com.instachat.android.adapter;

import com.instachat.android.model.PrivateChatSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 12/13/2016.
 * <p>
 * Since firebase is callback based, we don't know when all the data will be returned.  Data comes
 * back callback by callback, so we don't have a list of data.
 *
 * Manage a list of data and act on it in time chunks to avoid OOM and over taxing the cpu
 * especially if you have a lot of contacts (>200).
 */

public class UserPresenceManager {

    private List<PrivateChatSummary> mUsers = new ArrayList<>(200);

    public void queue(PrivateChatSummary privateChatSummary) {

        /**
         * when the first item is added, start a timer.
         * If 80 items are added or 3 seconds has elapsed (whichever comes first)
         * then process all the items in batches of 80.  Stop the timer when processing
         * has begun.
         * When all items have been processed, remove them from the queue and
         * reset the timer back to zero.
         *
         * If new items come in while processing, check if they are already being
         * processed. If not, add them to a holding queue.  When processing has finished
         * add the holding queue items to the main queue and start the timer.
         */

    }


}
