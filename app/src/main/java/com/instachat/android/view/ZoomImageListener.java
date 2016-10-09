package com.instachat.android.view;

/**
 * Created by kevin on 10/8/2016.
 */

public interface ZoomImageListener {
    void onImageTouched();

    void onRequestDisallowInterceptTouchEvent(boolean isZoomed);

    void onSetImageBitmap();
}
