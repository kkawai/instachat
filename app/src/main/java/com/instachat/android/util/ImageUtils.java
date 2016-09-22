package com.instachat.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Debug;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * This class contains various utilities to manipulate Bitmaps. The methods of
 * this class, although static, are not thread safe and cannot be invoked by
 * several threads at the same time. Synchronization is required by the caller.
 */
public final class ImageUtils {

    private static final String TAG = ImageUtils.class.getSimpleName();

    private static final float PHOTO_BORDER_WIDTH = 1.4f;
    private static final int PHOTO_BORDER_COLOR = 0xffffffff;

    private static final float ROTATION_ANGLE_MIN = 2.5f;
    private static final float ROTATION_ANGLE_EXTRA = 5.5f;

    private static final Random sRandom = new Random();
    private static final Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);
    private static final Paint sStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        sStrokePaint.setStrokeWidth(PHOTO_BORDER_WIDTH);
        sStrokePaint.setStyle(Paint.Style.STROKE);
        sStrokePaint.setColor(PHOTO_BORDER_COLOR);
    }

    /**
     * Rotate specified Bitmap by a random angle. The angle is either negative
     * or positive, and ranges, in degrees, from 2.5 to 8. After rotation a
     * frame is overlaid on top of the rotated image.
     * <p>
     * This method is not thread safe.
     *
     * @param bitmap The Bitmap to rotate and apply a frame onto.
     * @return A new Bitmap whose dimension are different from the original
     * bitmap.
     */
    public static Bitmap rotateAndFrame(final Bitmap bitmap) {
        final boolean positive = sRandom.nextFloat() >= 0.5f;
        final float angle = (ROTATION_ANGLE_MIN + sRandom.nextFloat()
                * ROTATION_ANGLE_EXTRA)
                * (positive ? 1.0f : -1.0f);
        final double radAngle = Math.toRadians(angle);

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        final double cosAngle = Math.abs(Math.cos(radAngle));
        final double sinAngle = Math.abs(Math.sin(radAngle));

        final int strokedWidth = (int) (bitmapWidth + 2 * PHOTO_BORDER_WIDTH);
        final int strokedHeight = (int) (bitmapHeight + 2 * PHOTO_BORDER_WIDTH);

        final int width = (int) (strokedHeight * sinAngle + strokedWidth
                * cosAngle);
        final int height = (int) (strokedWidth * sinAngle + strokedHeight
                * cosAngle);

        final float x = (width - bitmapWidth) / 2.0f;
        final float y = (height - bitmapHeight) / 2.0f;

        final Bitmap decored = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(decored);

        canvas.rotate(angle, width / 2.0f, height / 2.0f);
        canvas.drawBitmap(bitmap, x, y, sPaint);
        canvas.drawRect(x, y, x + bitmapWidth, y + bitmapHeight, sStrokePaint);

        //bitmap.recycle();

        return decored;
    }

    /**
     * not working right now; only works in sd
     * @param bitmap
     * @return
     */
//	public static Bitmap enhanceImageWithShadow(final Bitmap bitmap) {
//		final BlurMaskFilter blurFilter = new BlurMaskFilter(20, BlurMaskFilter.Blur.OUTER);
//		final Paint shadowPaint = new Paint();
//		shadowPaint.setMaskFilter(blurFilter);
//
//		final int[] offsetXY = new int[2];
//		final Bitmap shadowImage = bitmap.extractAlpha(shadowPaint, offsetXY);
//		final Bitmap shadowImage32 = shadowImage.copy(Bitmap.Config.ARGB_8888, true);
//
//		final Canvas c = new Canvas(shadowImage32);
//		c.drawBitmap(bitmap, -offsetXY[0], -offsetXY[1], null);
//
//		shadowImage.recycle();
//		bitmap.recycle();
//		return shadowImage32;
//	}

    /**
     * Scales the specified Bitmap to fit within the specified dimensions. After
     * scaling, a frame is overlaid on top of the scaled image.
     *
     * This method is not thread safe.
     *
     * @param bitmap
     *            The Bitmap to scale to fit the specified dimensions and to
     *            apply a frame onto.
     * @param width
     *            The maximum width of the new Bitmap.
     * @param height
     *            The maximum height of the new Bitmap.
     *
     * @return A scaled version of the original bitmap, whose dimension are less
     *         than or equal to the specified width and height.
     */
//	public static Bitmap scaleAndFrame(final Bitmap bitmap, final int width,
//			final int height) {
//
//		final int bitmapWidth = bitmap.getWidth();
//		final int bitmapHeight = bitmap.getHeight();
//
//		final float scale = Math.min((float) width / (float) bitmapWidth,
//				(float) height / (float) bitmapHeight);
//
//		final int scaledWidth = (int) (bitmapWidth * scale);
//		final int scaledHeight = (int) (bitmapHeight * scale);
//
//		final Bitmap decored = Bitmap.createScaledBitmap(bitmap, scaledWidth,
//				scaledHeight, true);
//		final Canvas canvas = new Canvas(decored);
//
//		final int offset = (int) (PHOTO_BORDER_WIDTH / 2);
//		sStrokePaint.setAntiAlias(false);
//		canvas.drawRect(offset, offset, scaledWidth - offset - 1, scaledHeight
//				- offset - 1, sStrokePaint);
//		sStrokePaint.setAntiAlias(true);
//
//		return decored;
//
//	}

    /**
     * caller must recycle the passed in bitmap if not used anymore
     * after this call!!!
     */
    public static Bitmap scale(final Bitmap bitmap, final int width,
                               final int height) {
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        final float scale = Math.min((float) width / (float) bitmapWidth,
                (float) height / (float) bitmapHeight);

        final int scaledWidth = (int) (bitmapWidth * scale);
        final int scaledHeight = (int) (bitmapHeight * scale);

        final Bitmap decored = Bitmap.createScaledBitmap(bitmap, scaledWidth,
                scaledHeight, true);
        // final Canvas canvas = new Canvas(decored);

        // final int offset = (int) (PHOTO_BORDER_WIDTH / 2);
        //sStrokePaint.setAntiAlias(false);
        // canvas.drawRect(offset, offset, scaledWidth - offset - 1,
        // scaledHeight - offset - 1, sStrokePaint);
        //sStrokePaint.setAntiAlias(true);
        return decored;
    }

    public static boolean isScreenPortrait(final Context context) {
        return getDisplay(context).getHeight() > getDisplay(context).getWidth();
    }

    public static int getScreenWidth(final Context context) {
        return getDisplay(context).getWidth();
    }

    public static int getScreenHeight(final Context context) {
        return getDisplay(context).getHeight();
    }

    public static Display getDisplay(final Context context) {
        return ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    public static Bitmap getBitmap(final Context context, final Uri uri, final int maxBytes) throws IOException {

        InputStream in = null;
        try {

            final int IMAGE_MAX_SIZE = maxBytes;
            in = context.getContentResolver().openInputStream(uri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();

            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            MLog.i(TAG, "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            Bitmap b = null;
            in = context.getContentResolver().openInputStream(uri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();
                MLog.i(TAG, "1th scale operation dimenions - width: " + width + ",  height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                        (int) y, true);
                b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            in.close();

            MLog.i(TAG, "bitmap size - width: " + b.getWidth() + ", height: " +
                    b.getHeight());
            return b;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {

                }
            }
        }
    }

    public static Bitmap convertBytesToBitmap(final byte[] inBytes) throws Exception {
        return BitmapFactory.decodeByteArray(inBytes, 0, inBytes.length);
    }

    public static Bitmap readBitmapFromFile(final String inFilepath) {
        return readBitmapFromFile(new File(inFilepath));
    }

    /**
     * Checks if a bitmap with the specified size fits in memory
     *
     * @param bmpwidth Bitmap width
     * @param bmpheight Bitmap height
     * @param bmpdensity Bitmap bpp (use 2 as default)
     * @return true if the bitmap fits in memory false otherwise
     */
    private final static double FOURMEGS = (double) 4 * 1024 * 1024;

    private static boolean bitmapFitsInMemory(final long bmpwidth, final long bmpheight, final float bmpdensity) {
        final float reqsize = bmpwidth * bmpheight * bmpdensity;
        final long allocNativeHeap = Debug.getNativeHeapAllocatedSize();

        final long heapPad = (long) Math.max(FOURMEGS, Runtime.getRuntime().maxMemory() * 0.1);
        if ((reqsize + allocNativeHeap + heapPad) >= Runtime.getRuntime().maxMemory()) {
            return false;
        }
        return true;

    }

    public static boolean bitmapFitsInMemory(final String path, final float bmpdensity) {
        return bitmapFitsInMemory(path, bmpdensity, 1);
    }

    public static int bitmapRequiredMemory(final File path, final float bmpdensity, final int sampleSize) {
        final BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;
        bmpFactoryOptions.inSampleSize = sampleSize;
        BitmapFactory.decodeFile(path.getPath(), bmpFactoryOptions);
        return (int) (bmpFactoryOptions.outHeight * bmpFactoryOptions.outWidth * bmpdensity);
    }

    public static boolean bitmapFitsInMemory(final String path, final float bmpdensity, final int sampleSize) {
        final BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;
        bmpFactoryOptions.inSampleSize = sampleSize;
        BitmapFactory.decodeFile(path, bmpFactoryOptions);
        if (!ImageUtils.bitmapFitsInMemory(bmpFactoryOptions.outWidth, bmpFactoryOptions.outHeight, bmpdensity)) {
            MLog.w(TAG, "ImageUtils.bitmapFitsInMemory() Aborting bitmap load for avoiding memory crash..");
            System.gc();
            return false;
        }
        return true;
    }

    public static Bitmap readBitmapFromFile(final File file) {

        try {
            return BitmapFactory.decodeStream(new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE));
        } catch (final Throwable e) {
            MLog.i(TAG, "ImageUtils readBitmapFromFile() failed", e);
            return null;
        }
    }

    public static Drawable readDrawableFromFile(final File file) {
        try {
            return Drawable.createFromStream(
                    new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE), "src");
        } catch (final Exception e) {
            MLog.i(TAG, "ImageUtils.readDrawableFromFile() failed", e);
            return null;
        }
    }

    public static void writeBitmapToFile(final Bitmap inBitmap, final String filepath) throws Exception {
        writeBitmapToFile(inBitmap, new File(filepath));
    }

    public static void writeBitmapToFile(final Bitmap inBitmap, final File file) throws Exception {

        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        final FileOutputStream fos = new FileOutputStream(file);
        final boolean result = inBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.close();
        if (!result) {
            throw new Exception("could not write file bytes: " + file.getName());
        }
    }

    public static Bitmap getRoundedCornerBitmap(final Bitmap bitmap, final int pixels) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Should be compatible with GB
     *
     * @param view
     * @return
     */
    public static Bitmap getBitmapFromView(final View view) {
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public static final int IO_BUFFER_SIZE = 4 * 1024;
}
