package com.instachat.android.view;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;

class Dot {
    private Paint mPaint;
    int mCurrentColorIndex;
    private int mDotRadius;
    private AnimatedDotLoadingView mParent;
    float cx;
    float cy;
    int position;
    ValueAnimator positionAnimator;
    ValueAnimator colorAnimator;

    Dot(AnimatedDotLoadingView parent, int dotRadius, int position) {
        this.position = position;
        this.mParent = parent;
        this.mCurrentColorIndex = 0;
        this.mDotRadius = dotRadius;
        this.mPaint = new Paint(1);
        this.mPaint.setColor(this.mParent.mColors[this.mCurrentColorIndex].intValue());
        this.mPaint.setShadowLayer(5.5F, 6.0F, 6.0F, -16777216);
        this.mPaint.setStyle(Style.FILL);
    }

    public void setColorIndex(int index) {
        this.mCurrentColorIndex = index;
        this.mPaint.setColor(this.mParent.mColors[index].intValue());
    }

    public void setColor(int color) {
        this.mPaint.setColor(color);
    }

    public int getCurrentColor() {
        return this.mParent.mColors[this.mCurrentColorIndex].intValue();
    }

    public int incrementAndGetColor() {
        this.incrementColorIndex();
        return this.getCurrentColor();
    }

    void applyNextColor() {
        ++this.mCurrentColorIndex;
        if (this.mCurrentColorIndex >= this.mParent.mColors.length) {
            this.mCurrentColorIndex = 0;
        }

        this.mPaint.setColor(this.mParent.mColors[this.mCurrentColorIndex].intValue());
    }

    int incrementColorIndex() {
        ++this.mCurrentColorIndex;
        if (this.mCurrentColorIndex >= this.mParent.mColors.length) {
            this.mCurrentColorIndex = 0;
        }

        return this.mCurrentColorIndex;
    }

    public void draw(Canvas canvas) {
        canvas.drawCircle(this.cx, this.cy, (float) this.mDotRadius, this.mPaint);
    }
}