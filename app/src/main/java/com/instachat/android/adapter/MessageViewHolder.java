package com.instachat.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.R;

import de.hdodenhof.circleimageview.CircleImageView;
import me.yifeiyuan.library.PeriscopeLayout;

/**
 * Created by kevin on 9/3/2016.
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {
    public TextView messageTextView;
    public TextView messengerTextView;
    public TextView messageTimeTextView;
    public ImageView messagePhotoView;
    public FrameLayout messagePhotoViewParent;
    public CircleImageView messengerImageView;
    public PeriscopeLayout periscopeLayout;

    /*
     * Web link content
     */
    public TextView webLinkContent;
    public ImageView webLinkImageView;
    public TextView webLinkTitle;
    public TextView webLinkUrl;
    public TextView webLinkDescription;

    public MessageViewHolder(View v) {
        super(v);
        messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
        messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
        messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        messageTimeTextView = (TextView) itemView.findViewById(R.id.messageTimeTextView);
        messagePhotoView = (ImageView) itemView.findViewById(R.id.messagePhotoView);
        messagePhotoViewParent = (FrameLayout) itemView.findViewById(R.id.messagePhotoViewParent);
        periscopeLayout = (PeriscopeLayout) itemView.findViewById(R.id.periscope);

        webLinkContent = (TextView) itemView.findViewById(R.id.post_content);
        webLinkImageView = (ImageView) itemView.findViewById(R.id.image_post);
        webLinkTitle = (TextView) itemView.findViewById(R.id.title);
        webLinkUrl = (TextView) itemView.findViewById(R.id.url);
        webLinkDescription = (TextView) itemView.findViewById(R.id.description);
    }
}