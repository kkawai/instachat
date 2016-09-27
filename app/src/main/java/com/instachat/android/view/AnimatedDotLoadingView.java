package com.instachat.android.view;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.bhargavms.dotloader.CubicBezierInterpolator;
import com.bhargavms.dotloader.R.styleable;

import java.lang.ref.WeakReference;

public class AnimatedDotLoadingView extends View {
    private static final int DELAY_BETWEEN_DOTS = 80;
    private static final int BOUNCE_ANIMATION_DURATION = 500;
    private static final int COLOR_ANIMATION_DURATION = 80;
    private Dot[] mDots;
    Integer[] mColors;
    private int mDotRadius;
    private Rect mClipBounds;
    private float mCalculatedGapBetweenDotCenters;
    private float mFromY;
    private float mToY;
    private Interpolator bounceAnimationInterpolator = new CubicBezierInterpolator(0.62F, 0.28F, 0.23F, 0.99F);

    public void setNumberOfDots(int numberOfDots) {
        Dot[] newDots = new Dot[numberOfDots];
        if (numberOfDots < this.mDots.length) {
            System.arraycopy(this.mDots, 0, newDots, 0, numberOfDots);
        } else {
            System.arraycopy(this.mDots, 0, newDots, 0, this.mDots.length);

            for (int i = this.mDots.length; i < numberOfDots; ++i) {
                newDots[i] = new Dot(this, this.mDotRadius, i);
                newDots[i].cx = newDots[i - 1].cx + this.mCalculatedGapBetweenDotCenters;
                newDots[i].cy = newDots[i - 1].cy / 2.0F;
                newDots[i].setColorIndex(newDots[i - 1].mCurrentColorIndex);
                newDots[i].positionAnimator = this.clonePositionAnimatorForDot(newDots[0].positionAnimator, newDots[i]);
                newDots[i].colorAnimator = this.cloneColorAnimatorForDot(newDots[0].colorAnimator, newDots[i]);
                newDots[i].positionAnimator.start();
            }
        }

        this.mDots = newDots;
    }

    private ValueAnimator cloneColorAnimatorForDot(ValueAnimator colorAnimator, Dot dot) {
        ValueAnimator valueAnimator = colorAnimator.clone();
        valueAnimator.removeAllUpdateListeners();
        valueAnimator.addUpdateListener(new AnimatedDotLoadingView.DotColorUpdater(dot, this));
        return valueAnimator;
    }

    private ValueAnimator clonePositionAnimatorForDot(ValueAnimator animator, final Dot dot) {
        ValueAnimator valueAnimator = animator.clone();
        valueAnimator.removeAllUpdateListeners();
        valueAnimator.addUpdateListener(new AnimatedDotLoadingView.DotYUpdater(dot, this));
        valueAnimator.setStartDelay((long) (80 * dot.position));
        valueAnimator.removeAllListeners();
        valueAnimator.addListener(new AnimatorListener() {
            private boolean alternate = true;

            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
            }

            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
                if (this.alternate) {
                    dot.colorAnimator.setObjectValues(new Object[]{AnimatedDotLoadingView.this.mColors[dot.mCurrentColorIndex], AnimatedDotLoadingView.this.mColors[dot.incrementColorIndex()]});
                    dot.colorAnimator.start();
                    this.alternate = false;
                } else {
                    this.alternate = true;
                }

            }
        });
        return valueAnimator;
    }

    public void resetColors() {
        Dot[] var1 = this.mDots;
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            Dot dot = var1[var3];
            dot.setColorIndex(0);
        }

    }

    public AnimatedDotLoadingView(Context context) {
        super(context);
        this.init(context, (AttributeSet) null);
    }

    public AnimatedDotLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context, attrs);
    }

    public AnimatedDotLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs);
    }

    @TargetApi(21)
    public AnimatedDotLoadingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init(context, attrs);
    }

    private void init(Context c, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = c.getTheme().obtainStyledAttributes(attrs, styleable.DotLoader, 0, 0);

            try {
                float dotRadius = a.getDimension(styleable.DotLoader_dot_radius, 0.0F);
                int numberOfPods = a.getInteger(styleable.DotLoader_number_of_dots, 1);
                int resId = a.getResourceId(styleable.DotLoader_color_array, 0);
                Integer[] colors;
                if (resId == 0) {
                    colors = new Integer[numberOfPods];

                    for (int var13 = 0; var13 < numberOfPods; ++var13) {
                        colors[var13] = Integer.valueOf(0);
                    }
                } else {
                    int[] temp = this.getResources().getIntArray(resId);
                    colors = new Integer[temp.length];

                    for (int i = 0; i < temp.length; ++i) {
                        colors[i] = Integer.valueOf(temp[i]);
                    }
                }

                this.init(numberOfPods, colors, (int) dotRadius);
            } finally {
                a.recycle();
            }

        }
    }

    private void _stopAnimations() {
        Dot[] var1 = this.mDots;
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            Dot dot = var1[var3];
            dot.positionAnimator.end();
            dot.colorAnimator.end();
        }

    }

    private void init(int numberOfDots, Integer[] colors, int dotRadius) {
        this.mColors = colors;
        this.mClipBounds = new Rect(0, 0, 0, 0);
        this.mDots = new Dot[numberOfDots];
        this.mDotRadius = dotRadius;

        for (int i = 0; i < numberOfDots; ++i) {
            this.mDots[i] = new Dot(this, dotRadius, i);
        }
    }

    public void initAnimation() {
        int i = 0;

        for (int size = this.mDots.length; i < size; ++i) {
            this.mDots[i].positionAnimator = this.createValueAnimatorForDot(this.mDots[i]);
            this.mDots[i].positionAnimator.setStartDelay((long) (80 * i));
            this.mDots[i].colorAnimator = this.createColorAnimatorForDot(this.mDots[i]);
        }

    }

    public void startAnimation() {
        this.post(new Runnable() {
            public void run() {
                AnimatedDotLoadingView.this._startAnimation();
            }
        });
    }

    public void stopAnimation() {
        this.post(new Runnable() {
            public void run() {
                AnimatedDotLoadingView.this._stopAnimations();
            }
        });
    }

    private void _startAnimation() {
        Dot[] var1 = this.mDots;
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            Dot mDot = var1[var3];
            mDot.positionAnimator.start();
        }

    }

    private ValueAnimator createValueAnimatorForDot(final Dot dot) {
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mFromY, this.mToY});
        animator.setInterpolator(this.bounceAnimationInterpolator);
        animator.setDuration(500L);
        animator.setRepeatCount(-1);
        animator.setRepeatMode(2);
        animator.addUpdateListener(new AnimatedDotLoadingView.DotYUpdater(dot, this));
        animator.addListener(new AnimatorListener() {
            private boolean alternate = true;

            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
            }

            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
                if (this.alternate) {
                    dot.colorAnimator.setObjectValues(new Object[]{AnimatedDotLoadingView.this.mColors[dot.mCurrentColorIndex], AnimatedDotLoadingView.this.mColors[dot.incrementColorIndex()]});
                    dot.colorAnimator.start();
                    this.alternate = false;
                } else {
                    this.alternate = true;
                }

            }
        });
        return animator;
    }

    private ValueAnimator createColorAnimatorForDot(Dot dot) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{this.mColors[dot.mCurrentColorIndex], this.mColors[dot.incrementColorIndex()]});
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(80L);
        animator.addUpdateListener(new AnimatedDotLoadingView.DotColorUpdater(dot, this));
        return animator;
    }

    private void invalidateOnlyRectIfPossible() {
        if (this.mClipBounds != null && this.mClipBounds.left != 0 && this.mClipBounds.top != 0 && this.mClipBounds.right != 0 && this.mClipBounds.bottom != 0) {
            this.invalidate(this.mClipBounds);
        } else {
            this.invalidate();
        }

    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(this.mClipBounds);
        Dot[] var2 = this.mDots;
        int var3 = var2.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            Dot mDot = var2[var4];
            mDot.draw(canvas);
        }

    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int desiredHeight = this.mDotRadius * 2 * 3 + this.getPaddingTop() + this.getPaddingBottom();
        int height;
        if (heightMode == 1073741824) {
            height = heightSize;
        } else if (heightMode == -2147483648) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        this.mCalculatedGapBetweenDotCenters = this.calculateGapBetweenDotCenters();
        int desiredWidth = (int) (this.mCalculatedGapBetweenDotCenters * (float) this.mDots.length);
        int width;
        if (widthMode == 1073741824) {
            width = widthSize;
        } else if (widthMode == -2147483648) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        int i = 0;

        for (int size = this.mDots.length; i < size; ++i) {
            this.mDots[i].cx = (float) this.mDotRadius + (float) i * this.mCalculatedGapBetweenDotCenters;
            this.mDots[i].cy = (float) (height - this.mDotRadius);
        }

        this.mFromY = (float) (height - this.mDotRadius);
        this.mToY = (float) this.mDotRadius;
        this.initAnimation();
        this.setMeasuredDimension(width, height);
    }

    private float calculateGapBetweenDotCenters() {
        return (float) (this.mDotRadius * 2 + this.mDotRadius);
    }

    private static class DotYUpdater implements AnimatorUpdateListener {
        private Dot mDot;
        private WeakReference<AnimatedDotLoadingView> mDotLoaderRef;

        private DotYUpdater(Dot dot, AnimatedDotLoadingView dotLoader) {
            this.mDot = dot;
            this.mDotLoaderRef = new WeakReference(dotLoader);
        }

        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            this.mDot.cy = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            AnimatedDotLoadingView dotLoader = (AnimatedDotLoadingView) this.mDotLoaderRef.get();
            if (dotLoader != null) {
                dotLoader.invalidateOnlyRectIfPossible();
            }

        }
    }

    private static class DotColorUpdater implements AnimatorUpdateListener {
        private Dot mDot;
        private WeakReference<AnimatedDotLoadingView> mDotLoaderRef;

        private DotColorUpdater(Dot dot, AnimatedDotLoadingView dotLoader) {
            this.mDot = dot;
            this.mDotLoaderRef = new WeakReference(dotLoader);
        }

        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            this.mDot.setColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
            AnimatedDotLoadingView dotLoader = (AnimatedDotLoadingView) this.mDotLoaderRef.get();
            if (dotLoader != null) {
                dotLoader.invalidateOnlyRectIfPossible();
            }

        }
    }
}
