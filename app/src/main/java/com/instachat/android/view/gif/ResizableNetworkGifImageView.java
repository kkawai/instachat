package com.instachat.android.view.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.android.volley.VolleyError;
import com.instachat.android.util.AnimationUtil;
import com.instachat.android.util.MLog;

public final class ResizableNetworkGifImageView extends GifImageView {

    private static final String TAG = ResizableNetworkGifImageView.class.getCanonicalName();

    private static final boolean IS_FADE_IN_ANIMATION_ENABLED = false;
    private boolean mAlreadyFadedIn;
    private int mDefaultImageId;
    private String mUrl;
    private int mErrorImageId;
    private GifImageLoader.GifImageContainer mGifImageContainer;
    private GifImageLoader mGifImageLoader;

    public ResizableNetworkGifImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public ResizableNetworkGifImageView(final Context context) {
        super(context);
    }

    public void setImageUrl(final String url, final GifImageLoader imageLoader) {
        mUrl = url;
        mGifImageLoader = imageLoader;
        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(false);
    }

    /**
     * Sets the default image resource ID to be used for this view until the attempt to load it
     * completes.
     */
    public void setDefaultImageResId(final int defaultImage) {
        mDefaultImageId = defaultImage;
    }

    /**
     * Sets the error image resource ID to be used for this view in the event that the image
     * requested fails to load.
     */
    public void setErrorImageResId(final int errorImage) {
        mErrorImageId = errorImage;
    }

    /**
     * onMeasure overridden to provide full screen width bleed etc.
     */
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {

        final Drawable d = getDrawable();

        if (d != null) {
            // ceil not round - avoid thin vertical gaps along the left/right
            // edges
            final int width = View.MeasureSpec.getSize(widthMeasureSpec);
            final int height = (int) Math.ceil((float) width * (float) d.getIntrinsicHeight() / (float) d.getIntrinsicWidth());
            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void setImageBitmap(final Bitmap bm) {

        super.setImageBitmap(bm);
        if (!mAlreadyFadedIn && bm != null) {
            mAlreadyFadedIn = true;

            if (IS_FADE_IN_ANIMATION_ENABLED)
                AnimationUtil.fadeInAnimation(this);
        }
    }

    private void setDefaultImageOrNull() {
        if (mDefaultImageId != 0) {
            setImageResource(mDefaultImageId);
        } else {
            setImageBitmap(null);
        }
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mGifImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mGifImageContainer.cancelRequest();
            setImageBitmap(null);
            // also clear out the container so we can reload the image if necessary.
            mGifImageContainer = null;
        }
        clear();
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     *
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    private void loadImageIfNecessary(final boolean isInLayoutPass) {
        final int width = getWidth();
        final int height = getHeight();

        boolean wrapWidth = false, wrapHeight = false;
        if (getLayoutParams() != null) {
            wrapWidth = getLayoutParams().width == LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == LayoutParams.WRAP_CONTENT;
        }

        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        final boolean isFullyWrapContent = wrapWidth && wrapHeight;
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mGifImageContainer != null) {
                mGifImageContainer.cancelRequest();
                mGifImageContainer = null;
            }
            setDefaultImageOrNull();
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mGifImageContainer != null && mGifImageContainer.getRequestUrl() != null) {
            if (mGifImageContainer.getRequestUrl().equals(mUrl)) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mGifImageContainer.cancelRequest();
                setDefaultImageOrNull();
            }
        }

        // Calculate the max image width / height to use while ignoring WRAP_CONTENT dimens.
        final int maxWidth = wrapWidth ? 0 : width;
        final int maxHeight = wrapHeight ? 0 : height;

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        final GifImageLoader.GifImageContainer newContainer = mGifImageLoader.get(mUrl, new GifImageLoader.GifImageListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                if (mErrorImageId != 0) {
                    setImageResource(mErrorImageId);
                }
            }

            @Override
            public void onResponse(final GifImageLoader.GifImageContainer response, final boolean isImmediate) {
                // If this was an immediate response that was delivered inside of a layout
                // pass do not set the image immediately as it will trigger a requestLayout
                // inside of a layout. Instead, defer setting the image by posting back to
                // the main thread.
                if (isImmediate && isInLayoutPass) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            onResponse(response, false);
                        }
                    });
                    return;
                }

                if (response.getGif() != null) {
                    try {
                        setBytes(response.getGif());
                        startAnimation();
                    } catch (final Exception e) {
                        setImageResource(mDefaultImageId);
                        MLog.e(TAG, "", e);
                    }
                } else if (mDefaultImageId != 0) {
                    setImageResource(mDefaultImageId);
                }
            }
        }, maxWidth, maxHeight);

        // update the ImageContainer to be the new bitmap container.
        mGifImageContainer = newContainer;
    }

}