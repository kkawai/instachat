package com.instachat.android.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.instachat.android.MyApp;

/**
 * @author kkawai
 */
public class MyNetworkImageView extends NetworkImageView {

    private static final String TAG = MyNetworkImageView.class.getSimpleName();

    private String mImageUrl;

    public MyNetworkImageView(final Context context) {
        super(context);
    }

    public MyNetworkImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public MyNetworkImageView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageUrl(final String url, final ImageLoader imageLoader) {
        super.setImageUrl(url, imageLoader);
        mImageUrl = url;
    }

    /**
     * kk, for some reason, the base volley NetworkImageView does not
     * have this method.  so, i have to make this.
     *
     * @return
     */
    public final String getImageUrl() {
        return mImageUrl;
    }

    public final void setImageUrl(final String url) {
        setImageUrl(url, MyApp.getInstance().getImageLoader());
    }
}
