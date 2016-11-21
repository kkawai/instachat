package com.instachat.android.adapter;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.instachat.android.MyApp;
import com.instachat.android.db.RssDb;
import com.instachat.android.model.FriendlyMessage;
import com.instachat.android.model.Rss;
import com.instachat.android.util.MLog;
import com.leocardz.link.preview.library.LinkPreviewCallback;
import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

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
        //viewHolder.messageTextView.setText(R.string.fetching_web_clipping);
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
                //viewHolder.messageTextView.setText(R.string.web_clipping);
                viewHolder.webLinkDescription.setText(sourceContent.getDescription() + "");
                viewHolder.webLinkTitle.setText(sourceContent.getTitle() + "");
                viewHolder.webLinkUrl.setText(sourceContent.getCannonicalUrl() + "");
                if (sourceContent.getImages().size() > 0) {
                    final Context c = viewHolder.webLinkImageView.getContext();
                    try {
                        Glide.with(viewHolder.webLinkImageView.getContext()).
                                load(sourceContent.getImages().get(0)).
                                bitmapTransform(
                                        new CenterCrop(c),
                                        new RoundedCornersTransformation(c, 30, 0, RoundedCornersTransformation.CornerType.ALL)).
                                crossFade().
                                into(viewHolder.webLinkImageView);
                    } catch (Exception e) {
                        //activity probably destroyed and processing web link took too long
                    }
                    rss.setImageUrl(sourceContent.getImages().get(0));
                }
                MLog.d(TAG, "saved link in rss db. basic link: " + rss.getBasicLink() + " main rss link: " + rss.getLink());
                RssDb.getInstance().insertRss(rss);
            }
        };

        Rss rss = RssDb.getInstance().getRssByLink(friendlyMessage.getText().trim());
        if (rss != null) {
            //viewHolder.messageTextView.setText(R.string.web_clipping);
            viewHolder.webLinkDescription.setText(rss.getDescr() + "");
            viewHolder.webLinkTitle.setText(rss.getTitle() + "");
            viewHolder.webLinkUrl.setText(rss.getBasicLink() + "");
            if (rss.getImageUrl() != null && rss.getImageUrl().toLowerCase().startsWith("http")) {
                Glide.with(MyApp.getInstance()).load(rss.getImageUrl()).into(viewHolder.webLinkImageView);
            }
            MLog.d(TAG, "found link in rss db. basic link: " + rss.getBasicLink() + " main rss link: " + rss.getLink());
        } else {
            //fetch the content from the site
            mTextCrawler.makePreview(callback, friendlyMessage.getText().trim());
        }
    }
}
