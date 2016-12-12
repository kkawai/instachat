package com.instachat.android.view.gif;

import android.support.v4.util.LruCache;

public final class GifLruCache implements GifImageLoader.GifImageCache {

    private final static int CACHE_MEMORY_DENOMINATOR = 16; // e.g. 1/16 of app's memory to be used for this cache
    private LruCache<String, byte[]> mMemoryCache;

    public GifLruCache() {

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / CACHE_MEMORY_DENOMINATOR;
        mMemoryCache = new LruCache<String, byte[]>(cacheSize) {
            @Override
            protected int sizeOf(final String key, final byte[] gif) {
                // The cache size will be measured in kilobytes rather than number of items.
                return gif.length / 1024;
            }
        };

    }

    @Override
    public byte[] getGif(final String url) {
        return mMemoryCache.get(url);
    }

    @Override
    public void putGif(final String url, final byte[] gif) {
        if (getGif(url) == null) {
            mMemoryCache.put(url, gif);
        }
    }

}
