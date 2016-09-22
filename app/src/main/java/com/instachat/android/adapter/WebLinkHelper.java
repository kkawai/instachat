package com.instachat.android.adapter;

import com.bumptech.glide.Glide;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.MyApp;
import com.instachat.android.db.RssDb;
import com.instachat.android.model.Rss;
import com.instachat.android.util.MLog;
import com.leocardz.link.preview.library.LinkPreviewCallback;
import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;

/**
 * Created by kevin on 9/20/2016.
 */
public class WebLinkHelper {

    public static final String TAG = "WebLinkHelper";
    private TextCrawler mTextCrawler;

    public WebLinkHelper() {
        mTextCrawler = new TextCrawler();
    }

    public void populateWebLinkPost(final MessageViewHolder viewHolder, final FriendlyMessage friendlyMessage, int position) {
        LinkPreviewCallback callback = new LinkPreviewCallback() {
            @Override
            public void onPre() {

            }

            @Override
            public void onPos(SourceContent sourceContent, boolean b) {
                Rss rss = new Rss();
                rss.setCategory(friendlyMessage.getUserid() + "");
                rss.setOriginalCategory(friendlyMessage.getName());
                rss.setDescr(sourceContent.getDescription());
                rss.setTitle(sourceContent.getTitle());
                rss.setBasicLink(sourceContent.getCannonicalUrl());
                rss.setLink(friendlyMessage.getText().trim());
                viewHolder.webLinkDescription.setText(sourceContent.getDescription() + "");
                viewHolder.webLinkTitle.setText(sourceContent.getTitle() + "");
                viewHolder.webLinkUrl.setText(sourceContent.getCannonicalUrl() + "");
                viewHolder.webLinkContent.setText(friendlyMessage.getText().trim());
                if (sourceContent.getImages().size() > 0) {
                    Glide.with(MyApp.getInstance()).load(sourceContent.getImages().get(0)).into(viewHolder.webLinkImageView);
                    rss.setImageUrl(sourceContent.getImages().get(0));
                }
                MLog.d(TAG, "saved link in rss db. basic link: " + rss.getBasicLink() + " main rss link: " + rss.getLink());
                RssDb.getInstance().insertRss(rss);
            }
        };

        Rss rss = RssDb.getInstance().getRssByLink(friendlyMessage.getText().trim());
        if (rss != null) {
            viewHolder.webLinkDescription.setText(rss.getDescr() + "");
            viewHolder.webLinkTitle.setText(rss.getTitle() + "");
            viewHolder.webLinkUrl.setText(rss.getBasicLink() + "");
            viewHolder.webLinkContent.setText(rss.getLink() + "");
            if (rss.getImageUrl() != null && rss.getImageUrl().toLowerCase().startsWith("http")) {
                Glide.with(MyApp.getInstance()).load(rss.getImageUrl()).into(viewHolder.webLinkImageView);
            }
            MLog.d(TAG, "found link in rss db. basic link: " + rss.getBasicLink() + " main rss link: " + rss.getLink());
            return;
        }
        mTextCrawler.makePreview(callback, friendlyMessage.getText().trim());
    }
}
