package com.instachat.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import com.instachat.android.R;

import java.util.Random;

/**
 * Created by kevin on 10/23/2016.
 * <p>
 * Pool the drawables used by PeriscopeLayout to efficiently
 * re-use the same drawables over and over.
 * Useful in applications that have lots of periscopes
 * on the same screen or multiple in a recycler or list view.
 * <p>
 * Also cache everything else the Periscope layout needs.
 */

public class PeriscopeDrawablePool {

    private PeriscopeDrawablePool() {
    }

    private static Drawable[] mDrawables;
    private static Random mRandom = new Random();
    private static Interpolator line = new LinearInterpolator();//线性
    private static Interpolator acc = new AccelerateInterpolator();//加速
    private static Interpolator dce = new DecelerateInterpolator();//减速
    private static Interpolator accdec = new AccelerateDecelerateInterpolator();//先加速后减速
    private static Interpolator[] mInterpolators;
    private static RelativeLayout.LayoutParams mLp;
    private static int mDrawableWidth;
    private static int mDrawableHeight;

    private static Interpolator[] getInterpolators() {
        if (mInterpolators == null) {
            mInterpolators = new Interpolator[4];
            mInterpolators[0] = line;
            mInterpolators[1] = acc;
            mInterpolators[2] = dce;
            mInterpolators[3] = accdec;
        }
        return mInterpolators;
    }

    public static RelativeLayout.LayoutParams getLayoutParams(Context context) {
        if (mLp == null) {
            mLp = new RelativeLayout.LayoutParams(getDrawableIntrinsicWidth(context), getDrawableIntrinsicHeight(context));
            mLp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);//这里的TRUE 要注意 不是true
            mLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        }
        return mLp;
    }

    public static Interpolator getRandomInterpolator() {
        return getInterpolators()[mRandom.nextInt(4)];
    }

    private static Drawable[] getPeriscopeDrawables(Context context) {
        if (mDrawables == null) {
            mDrawables = new Drawable[3];
            Drawable red = context.getResources().getDrawable(R.drawable.pl_red);
            Drawable yellow = context.getResources().getDrawable(R.drawable.pl_yellow);
            Drawable blue = context.getResources().getDrawable(R.drawable.pl_blue);
            mDrawables[0] = red;
            mDrawables[1] = yellow;
            mDrawables[2] = blue;
        }
        return mDrawables;
    }

    public static Drawable getRandomDrawable(Context context) {
        return getPeriscopeDrawables(context)[mRandom.nextInt(3)];
    }

    public static int getDrawableIntrinsicHeight(Context context) {
        if (mDrawableHeight == 0)
            mDrawableHeight = getPeriscopeDrawables(context)[0].getIntrinsicHeight();
        return mDrawableHeight;
    }

    public static int getDrawableIntrinsicWidth(Context context) {
        if (mDrawableWidth == 0)
            mDrawableWidth = getPeriscopeDrawables(context)[0].getIntrinsicWidth();
        return mDrawableWidth;
    }
}
