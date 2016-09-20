package com.google.firebase.codelab.friendlychat.adapter;

import com.bumptech.glide.Glide;
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.initech.MyApp;
import com.leocardz.link.preview.library.LinkPreviewCallback;
import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;

/**
 * Created by kevin on 9/20/2016.
 */
public class WebLinkHelper {

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
                viewHolder.webLinkDescription.setText(sourceContent.getDescription());
                viewHolder.webLinkTitle.setText(sourceContent.getTitle());
                viewHolder.webLinkUrl.setText(sourceContent.getCannonicalUrl());
                viewHolder.webLinkContent.setText(sourceContent.getFinalUrl());
                if (sourceContent.getImages().size() > 0) {
                    Glide.with(MyApp.getInstance()).load(sourceContent.getImages().get(0)).into(viewHolder.webLinkImageView);
                }
            }
        };
        mTextCrawler.makePreview(callback, friendlyMessage.getText());
    }
}
