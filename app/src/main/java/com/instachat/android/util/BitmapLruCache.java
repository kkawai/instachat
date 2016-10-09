package com.instachat.android.util;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public final class BitmapLruCache extends LruCache<String, Bitmap> implements ImageCache {

	public BitmapLruCache(int maxSize) {
		super(maxSize);
	}
	
	@Override
	public int sizeOf(String key, Bitmap value) {
		return value.getRowBytes() * value.getHeight() / 1024; // convert byte to MB
	}

	@Override
	public Bitmap getBitmap(String url) {
		return (Bitmap) this.get(url);
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		if (this.get(url) == null) {
			this.put(url, bitmap);
		}
	}

}
