package com.instachat.android.view.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.instachat.android.util.MLog;

public class GifImageView extends ImageView implements Runnable {

    private static final String TAG = GifImageView.class.getName();
    private GifDecoder mGifDecoder;
    private Bitmap mTmpBitmap;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsAnimating = false;
    private boolean mIsShouldClear = false;
    private Thread mAnimationThread;

    private final Runnable updateResults = new Runnable() {
        @Override
        public void run() {
            if (mTmpBitmap != null && !mTmpBitmap.isRecycled()) {
                setImageBitmap(mTmpBitmap);
            }
        }
    };

    private final Runnable cleanupRunnable = new Runnable() {
        @Override
        public void run() {
            mTmpBitmap = null;
            mGifDecoder = null;
            mAnimationThread = null;
        }
    };

    public GifImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public GifImageView(final Context context) {
        super(context);
    }

    public void setBytes(final byte[] bytes) {
        mGifDecoder = new GifDecoder();
        try {
            mGifDecoder.read(bytes);
        } catch (final OutOfMemoryError e) {
            mGifDecoder = null;
            MLog.e(TAG, e.getMessage(), e);
            return;
        }

        if (canStart()) {
            mAnimationThread = new Thread(this);
            mAnimationThread.start();
        }
    }

    public void startAnimation() {
        mIsAnimating = true;

        if (canStart()) {
            mAnimationThread = new Thread(this);
            mAnimationThread.start();
        }
    }

    public boolean isAnimating() {
        return mIsAnimating;
    }

    public void stopAnimation() {
        mIsAnimating = false;

        if (mAnimationThread != null) {
            mAnimationThread.interrupt();
            mAnimationThread = null;
        }
    }

    public final void clear() {
        mIsShouldClear = true;
        stopAnimation();
        mGifDecoder = null;
    }

    private boolean canStart() {
        return mIsAnimating && mGifDecoder != null && mAnimationThread == null;
    }

    @Override
    public void run() {
        if (mGifDecoder == null) return;
        final int n = mGifDecoder.getFrameCount();
        do {

            for (int i = 0; i < n; i++) {
                if (!mIsAnimating)
                    break;
                try {
                    mTmpBitmap = mGifDecoder.getNextFrame();
                    if (!mIsAnimating)
                        break;
                    mHandler.post(updateResults);
                } catch (final Throwable t) {
                    //suppress
                }
                if (!mIsAnimating)
                    break;

                try {
                    mGifDecoder.advance();
                    Thread.sleep(mGifDecoder.getNextDelay());
                } catch (final Throwable t) {
                    // suppress
                }
            }
        } while (mIsAnimating);

        if (mIsShouldClear)
            mHandler.post(cleanupRunnable);
    }

}
