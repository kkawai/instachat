package com.instachat.android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class BlurBuilder {
//   private static final float BITMAP_SCALE = 0.4f;
//   private static final float BLUR_RADIUS = 7.5f;
   private static final float BITMAP_SCALE = 0.19f;
   private static final float BLUR_RADIUS = 20f;

   @TargetApi(17)
   public static Bitmap blur(final Context context, final Bitmap image) {
      int width = Math.round(image.getWidth() * BITMAP_SCALE);
      int height = Math.round(image.getHeight() * BITMAP_SCALE);

      final Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
      final Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

      RenderScript rs = RenderScript.create(context);
      ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
      Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
      Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
      theIntrinsic.setRadius(BLUR_RADIUS);
      theIntrinsic.setInput(tmpIn);
      theIntrinsic.forEach(tmpOut);
      tmpOut.copyTo(outputBitmap);

      return outputBitmap;
   }
}