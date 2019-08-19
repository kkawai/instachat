package com.instachat.android.app.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.instachat.android.R;
import com.instachat.android.databinding.ItemMessageBinding;
import com.instachat.android.view.PeriscopeLayout;
import com.sackcentury.shinebuttonlib.ShineButton;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by kevin on 9/3/2016.
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {
    public View messageTextParent;
    public TextView messageTextView;
    public TextView messengerTextView;
    public TextView messageTimeTextView;
    public ImageView messagePhotoView;
    public FrameLayout messagePhotoViewParent;
    public View messagePhotoWarningView;
    public CircleImageView messengerImageView;
    public PeriscopeLayout periscopeLayout;
    public View periscopeParent;
    public ShineButton likesButton;
    public View likesButtonParent;
    public TextView likesCount;
    public ImageView messageReadConfirmationView;

    /*
     * Web link content
     */
    public ImageView webLinkImageView;
    public TextView webLinkTitle;
    public TextView webLinkUrl;
    public TextView webLinkDescription;
    public View webLinkParent;

    public MessageViewHolder(View v) {
        super(v);

        /**
         * Still using 'findViewById' because MessageViewHolder is being re-used for a
         * variety of different layout xml that happen to have the same views and ids.
         * Not a great solution, but a 'for now' compromise when cutting everything over
         * to data binding and mvvm architecture.
         *
         * Ideally, we would want a different ViewHolder for each different layout
         * and adapter respectively.  And the unique binding to go into each ViewHolder.
         */
        messageTextParent = itemView.findViewById(R.id.contentMessageChat);
        messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
        messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
        messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        messageTimeTextView = (TextView) itemView.findViewById(R.id.messageTimeTextView);
        messagePhotoView = (ImageView) itemView.findViewById(R.id.messagePhotoView);
        messagePhotoViewParent = (FrameLayout) itemView.findViewById(R.id.messagePhotoViewParent);
        messagePhotoWarningView = itemView.findViewById(R.id.messagePhotoWarningView);
        periscopeParent = itemView.findViewById(R.id.periscopeParent);
        periscopeLayout = (PeriscopeLayout) itemView.findViewById(R.id.periscope);
        likesButton = (ShineButton) itemView.findViewById(R.id.likeButton);
        likesButtonParent = itemView.findViewById(R.id.likeButtonParent);
        likesCount = (TextView) itemView.findViewById(R.id.likesCount);
        messageReadConfirmationView = (ImageView)itemView.findViewById(R.id.messageReadConfirmationView);

        webLinkParent = itemView.findViewById(R.id.web_clipping_parent);
        webLinkImageView = (ImageView) itemView.findViewById(R.id.web_link_image);
        webLinkTitle = (TextView) itemView.findViewById(R.id.title);
        webLinkUrl = (TextView) itemView.findViewById(R.id.url);
        webLinkDescription = (TextView) itemView.findViewById(R.id.description);
    }
}