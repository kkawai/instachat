package com.initech.view;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.initech.util.MLog;

public class AutoResizeTextView extends AppCompatTextView {
    private static final int NO_LINE_LIMIT = -1;
    private final RectF _availableSpaceRect;
    private final AutoResizeTextView.SizeTester _sizeTester;
    private float _maxTextSize;
    private float _spacingMult;
    private float _spacingAdd;
    private float _minTextSize;
    private int _widthLimit;
    private int _maxLines;
    private boolean _initialized;
    private TextPaint _paint;

    public AutoResizeTextView(Context context) {
        this(context, (AttributeSet) null, 16842884);
    }

    public AutoResizeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842884);
    }

    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this._availableSpaceRect = new RectF();
        this._spacingMult = 1.0F;
        this._spacingAdd = 0.0F;
        this._initialized = false;
        this._minTextSize = TypedValue.applyDimension(2, 12.0F, this.getResources().getDisplayMetrics());
        this._maxTextSize = this.getTextSize();
        this._paint = new TextPaint(this.getPaint());
        if (this._maxLines == 0) {
            this._maxLines = -1;
        }

        this._sizeTester = new AutoResizeTextView.SizeTester() {
            final RectF textRect = new RectF();

            @TargetApi(16)
            public int onTestSize(int suggestedSize, RectF availableSpace) {
                AutoResizeTextView.this._paint.setTextSize((float) suggestedSize);
                TransformationMethod transformationMethod = AutoResizeTextView.this.getTransformationMethod();
                String text;
                if (transformationMethod != null) {
                    text = transformationMethod.getTransformation(AutoResizeTextView.this.getText(), AutoResizeTextView.this).toString();
                } else {
                    text = AutoResizeTextView.this.getText().toString();
                }

                boolean singleLine = AutoResizeTextView.this.getMaxLines() == 1;
                if (singleLine) {
                    this.textRect.bottom = AutoResizeTextView.this._paint.getFontSpacing();
                    this.textRect.right = AutoResizeTextView.this._paint.measureText(text);
                } else {
                    StaticLayout layout = new StaticLayout(text, AutoResizeTextView.this._paint, AutoResizeTextView.this._widthLimit, Layout.Alignment.ALIGN_NORMAL, AutoResizeTextView.this._spacingMult, AutoResizeTextView.this._spacingAdd, true);
                    if (AutoResizeTextView.this.getMaxLines() != -1 && layout.getLineCount() > AutoResizeTextView.this.getMaxLines()) {
                        return 1;
                    }

                    this.textRect.bottom = (float) layout.getHeight();
                    int maxWidth = -1;
                    int lineCount = layout.getLineCount();

                    for (int i = 0; i < lineCount; ++i) {
                        int end = layout.getLineEnd(i);
                        if (text.length() - 1 >= end && i < lineCount - 1 && end > 0 && !AutoResizeTextView.this.isValidWordWrap(text.charAt(end - 1), text.charAt(end))) {
                            return 1;
                        }

                        if ((float) maxWidth < layout.getLineRight(i) - layout.getLineLeft(i)) {
                            maxWidth = (int) layout.getLineRight(i) - (int) layout.getLineLeft(i);
                        }
                    }

                    this.textRect.right = (float) maxWidth;
                }

                this.textRect.offsetTo(0.0F, 0.0F);
                return availableSpace.contains(this.textRect) ? -1 : 1;
            }
        };
        this._initialized = true;
    }

    public boolean isValidWordWrap(char before, char after) {
        return before == 32 || before == 45;
    }

    public void setAllCaps(boolean allCaps) {
        super.setAllCaps(allCaps);
        this.adjustTextSize();
    }

    public void setTypeface(Typeface tf) {
        super.setTypeface(tf);
        this.adjustTextSize();
    }

    public void setTextSize(float size) {
        this._maxTextSize = size;
        this.adjustTextSize();
    }

    public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        this._maxLines = maxLines;
        this.adjustTextSize();
    }

    public int getMaxLines() {
        return this._maxLines;
    }

    public void setSingleLine() {
        super.setSingleLine();
        this._maxLines = 1;
        this.adjustTextSize();
    }

    public void setSingleLine(boolean singleLine) {
        super.setSingleLine(singleLine);
        if (singleLine) {
            this._maxLines = 1;
        } else {
            this._maxLines = -1;
        }

        this.adjustTextSize();
    }

    public void setLines(int lines) {
        super.setLines(lines);
        this._maxLines = lines;
        this.adjustTextSize();
    }

    public void setTextSize(int unit, float size) {
        Context c = this.getContext();
        Resources r;
        if (c == null) {
            r = Resources.getSystem();
        } else {
            r = c.getResources();
        }

        this._maxTextSize = TypedValue.applyDimension(unit, size, r.getDisplayMetrics());
        this.adjustTextSize();
    }

    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        this._spacingMult = mult;
        this._spacingAdd = add;
    }

    public void setMinTextSize(float minTextSize) {
        this._minTextSize = minTextSize;
        this.adjustTextSize();
    }

    private void adjustTextSize() {
        if (this._initialized) {
            int startSize = (int) this._minTextSize;
            int heightLimit = this.getMeasuredHeight() - this.getCompoundPaddingBottom() - this.getCompoundPaddingTop();
            this._widthLimit = this.getMeasuredWidth() - this.getCompoundPaddingLeft() - this.getCompoundPaddingRight();
            if (this._widthLimit > 0) {
                this._paint = new TextPaint(this.getPaint());
                this._availableSpaceRect.right = (float) this._widthLimit;
                this._availableSpaceRect.bottom = (float) heightLimit;
                this.superSetTextSize(startSize);
            }
        }
    }

    private void superSetTextSize(int startSize) {
        int textSize = this.binarySearch(startSize, (int) this._maxTextSize, this._sizeTester, this._availableSpaceRect);
        super.setTextSize(0, (float) textSize);
    }

    private int binarySearch(int start, int end, AutoResizeTextView.SizeTester sizeTester, RectF availableSpace) {
        int lastBest = start;
        int lo = start;
        int hi = end - 1;

        while (lo <= hi) {
            int mid = lo + hi >>> 1;
            int midValCmp = sizeTester.onTestSize(mid, availableSpace);
            if (midValCmp < 0) {
                lastBest = lo;
                lo = mid + 1;
            } else {
                if (midValCmp <= 0) {
                    return mid;
                }

                hi = mid - 1;
                lastBest = hi;
            }
        }

        return lastBest;
    }

    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        this.adjustTextSize();
    }

    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        super.onSizeChanged(width, height, oldwidth, oldheight);
        if (width != oldwidth || height != oldheight) {
            this.adjustTextSize();
        }

    }

    private interface SizeTester {
        int onTestSize(int var1, RectF var2);
    }
}
