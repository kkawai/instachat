package com.google.firebase.codelab.friendlychat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by kevin on 9/3/2016.
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {
    public TextView messageTextView;
    public TextView messengerTextView;
    public TextView messageTimeTextView;
    public ImageView messagePhotoView;
    public CircleImageView messengerImageView;

    public MessageViewHolder(View v) {
        super(v);
        messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
        messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
        messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        messageTimeTextView = (TextView) itemView.findViewById(R.id.messageTimeTextView);
        messagePhotoView = (ImageView) itemView.findViewById(R.id.messagePhotoView);
    }
}