/**
 * A modified version of google's volley library ImageLoader class;
 * Load Gif images, rather than Bitmap images.
 */
package com.instachat.android.view.gif;

import android.os.Handler;
import android.os.Looper;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.instachat.android.api.ApiGetBytesRequest;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Helper that handles loading and caching images from remote URLs.
 * <p/>
 * The simple way to use this class is to call {@link GifImageLoader#get(String, GifImageLoader.GifImageListener)}
 * and to pass in the default image listener provided by
 * {@link GifImageLoader#getImageListener(GifImageView, int, int)}. Note that all function calls to
 * this class must be made from the main thead, and all responses will be delivered to the main
 * thread as well.
 */
public final class GifImageLoader {
    /**
     * RequestQueue for dispatching ImageRequests onto.
     */
    private final RequestQueue mRequestQueue;

    /**
     * Amount of time to wait after first response arrives before delivering all responses.
     */
    private int mBatchResponseDelayMs = 100;

    /**
     * The cache implementation to be used as an L1 cache before calling into volley.
     */
    private final GifImageCache mCache;

    /**
     * HashMap of Cache keys -> BatchedImageRequest used to track in-flight requests so
     * that we can coalesce multiple requests to the same URL into a single network request.
     */
    private final HashMap<String, BatchedGifImageRequest> mInFlightRequests =
            new HashMap<String, BatchedGifImageRequest>();

    /**
     * HashMap of the currently pending responses (waiting to be delivered).
     */
    private final HashMap<String, BatchedGifImageRequest> mBatchedResponses =
            new HashMap<String, BatchedGifImageRequest>();

    /**
     * Handler to the main thread.
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Runnable for in-flight response delivery.
     */
    private Runnable mRunnable;

    /**
     * Simple cache adapter interface. If provided to the ImageLoader, it
     * will be used as an L1 cache before dispatch to Volley. Implementations
     * must not block. Implementation with an LruCache is recommended.
     */
    public interface GifImageCache {
        public byte[] getGif(String url);

        public void putGif(String url, byte[] gif);
    }

    /**
     * Constructs a new ImageLoader.
     *
     * @param queue      The RequestQueue to use for making image requests.
     * @param imageCache The cache to use as an L1 cache.
     */
    public GifImageLoader(final RequestQueue queue, final GifImageCache imageCache) {
        mRequestQueue = queue;
        mCache = imageCache;
    }

    /**
     * The default implementation of ImageListener which handles basic functionality
     * of showing a default image until the network response is received, at which point
     * it will switch to either the actual image or the error image.
     *
     * @param gifImageView      The imageView that the listener is associated with.
     * @param defaultImageResId Default image resource ID to use, or 0 if it doesn't exist.
     * @param errorImageResId   Error image resource ID to use, or 0 if it doesn't exist.
     */
    public static GifImageListener getImageListener(final GifImageView gifImageView,
                                                    final int defaultImageResId, final int errorImageResId) {
        return new GifImageListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                if (errorImageResId != 0) {
                    gifImageView.setImageResource(errorImageResId);
                }
            }

            @Override
            public void onResponse(final GifImageContainer response, final boolean isImmediate) {
                if (response.getGif() != null) {
                    gifImageView.setBytes(response.getGif());
                } else if (defaultImageResId != 0) {
                    gifImageView.setImageResource(defaultImageResId);
                }
            }
        };
    }

    /**
     * Interface for the response handlers on image requests.
     * <p/>
     * The call flow is this:
     * 1. Upon being  attached to a request, onResponse(response, true) will
     * be invoked to reflect any cached data that was already available. If the
     * data was available, response.getgif() will be non-null.
     * <p/>
     * 2. After a network response returns, only one of the following cases will happen:
     * - onResponse(response, false) will be called if the image was loaded.
     * or
     * - onErrorResponse will be called if there was an error loading the image.
     */
    public interface GifImageListener extends ErrorListener {
        /**
         * Listens for non-error changes to the loading of the image request.
         *
         * @param response    Holds all information pertaining to the request, as well
         *                    as the gif (if it is loaded).
         * @param isImmediate True if this was called during ImageLoader.get() variants.
         *                    This can be used to differentiate between a cached image loading and a network
         *                    image loading in order to, for example, run an animation to fade in network loaded
         *                    images.
         */
        public void onResponse(final GifImageContainer response, final boolean isImmediate);
    }

    /**
     * Checks if the item is available in the cache.
     *
     * @param requestUrl The url of the remote image
     * @param maxWidth   The maximum width of the returned image.
     * @param maxHeight  The maximum height of the returned image.
     * @return True if the item exists in cache, false otherwise.
     */
    public boolean isCached(final String requestUrl, final int maxWidth, final int maxHeight) {
        throwIfNotOnMainThread();

        String cacheKey = getCacheKey(requestUrl, maxWidth, maxHeight);
        return mCache.getGif(cacheKey) != null;
    }

    /**
     * Returns an ImageContainer for the requested URL.
     * <p/>
     * The ImageContainer will contain either the specified default gif or the loaded gif.
     * If the default was returned, the {@link GifImageLoader} will be invoked when the
     * request is fulfilled.
     *
     * @param requestUrl The URL of the image to be loaded.
     * @param listener   The listener for the load request
     */
    public GifImageContainer get(final String requestUrl, final GifImageListener listener) {
        return get(requestUrl, listener, 0, 0);
    }

    /**
     * Issues a gif request with the given URL if that image is not available
     * in the cache, and returns a gif container that contains all of the data
     * relating to the request (as well as the default image if the requested
     * image is not available).
     *
     * @param requestUrl    The url of the remote image
     * @param imageListener The listener to call when the remote image is loaded
     * @param maxWidth      The maximum width of the returned image.
     * @param maxHeight     The maximum height of the returned image.
     * @return A container object that contains all of the properties of the request, as well as
     * the currently available image (default if remote is not loaded).
     */
    public GifImageContainer get(final String requestUrl, final GifImageListener imageListener,
                                 final int maxWidth, final int maxHeight) {
        // only fulfill requests that were initiated from the main thread.
        throwIfNotOnMainThread();

        final String cacheKey = getCacheKey(requestUrl, maxWidth, maxHeight);

        // Try to look up the request in the cache of remote images.
        final byte[] cachedGif = mCache.getGif(cacheKey);
        if (cachedGif != null) {
            // Return the cached gif.
            final GifImageContainer container = new GifImageContainer(cachedGif, requestUrl, null, null);
            imageListener.onResponse(container, true);
            return container;
        }

        // The gif did not exist in the cache, fetch it!
        final GifImageContainer imageContainer =
                new GifImageContainer(null, requestUrl, cacheKey, imageListener);

        // Update the caller to let them know that they should use the default gif.
        imageListener.onResponse(imageContainer, true);

        // Check to see if a request is already in-flight.
        final BatchedGifImageRequest request = mInFlightRequests.get(cacheKey);
        if (request != null) {
            // If it is, add this request to the list of listeners.
            request.addContainer(imageContainer);
            return imageContainer;
        }

        // The request is not already in flight. Send the new request to the network and
        // track it.
        final Request<?> newRequest =
                new ApiGetBytesRequest(requestUrl, new Listener<byte[]>() {
                    @Override
                    public void onResponse(final byte[] response) {
                        onGetImageSuccess(cacheKey, response);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        onGetImageError(cacheKey, error);
                    }
                });

        mRequestQueue.add(newRequest);
        mInFlightRequests.put(cacheKey,
                new BatchedGifImageRequest(newRequest, imageContainer));
        return imageContainer;
    }

    /**
     * Sets the amount of time to wait after the first response arrives before delivering all
     * responses. Batching can be disabled entirely by passing in 0.
     *
     * @param newBatchedResponseDelayMs The time in milliseconds to wait.
     */
    public void setBatchedResponseDelay(final int newBatchedResponseDelayMs) {
        mBatchResponseDelayMs = newBatchedResponseDelayMs;
    }

    /**
     * Handler for when an image was successfully loaded.
     *
     * @param cacheKey The cache key that is associated with the image request.
     * @param response The gif that was returned from the network.
     */
    private void onGetImageSuccess(final String cacheKey, final byte[] response) {
        // cache the image that was fetched.
        mCache.putGif(cacheKey, response);

        // remove the request from the list of in-flight requests.
        final BatchedGifImageRequest request = mInFlightRequests.remove(cacheKey);

        if (request != null) {
            // Update the response gif.
            request.mResponseGif = response;

            // Send the batched response
            batchResponse(cacheKey, request);
        }
    }

    /**
     * Handler for when an image failed to load.
     *
     * @param cacheKey The cache key that is associated with the image request.
     */
    private void onGetImageError(final String cacheKey, final VolleyError error) {
        // Notify the requesters that something failed via a null result.
        // Remove this request from the list of in-flight requests.
        final BatchedGifImageRequest request = mInFlightRequests.remove(cacheKey);

        if (request != null) {
            // Set the error for this request
            request.setError(error);

            // Send the batched response
            batchResponse(cacheKey, request);
        }
    }

    /**
     * Container object for all of the data surrounding an image request.
     */
    public class GifImageContainer {
        /**
         * The most relevant gif for the container. If the image was in cache, the
         * Holder to use for the final gif (the one that pairs to the requested URL).
         */
        private byte[] mGif;

        private final GifImageListener mListener;

        /**
         * The cache key that was associated with the request
         */
        private final String mCacheKey;

        /**
         * The request URL that was specified
         */
        private final String mRequestUrl;

        /**
         * Constructs a gifContainer object.
         *
         * @param gif        The final gif (if it exists).
         * @param requestUrl The requested URL for this container.
         * @param cacheKey   The cache key that identifies the requested URL for this container.
         */
        public GifImageContainer(final byte[] gif, final String requestUrl,
                                 final String cacheKey, final GifImageListener listener) {
            mGif = gif;
            mRequestUrl = requestUrl;
            mCacheKey = cacheKey;
            mListener = listener;
        }

        /**
         * Releases interest in the in-flight request (and cancels it if no one else is listening).
         */
        public void cancelRequest() {
            if (mListener == null) {
                return;
            }

            BatchedGifImageRequest request = mInFlightRequests.get(mCacheKey);
            if (request != null) {
                boolean canceled = request.removeContainerAndCancelIfNecessary(this);
                if (canceled) {
                    mInFlightRequests.remove(mCacheKey);
                }
            } else {
                // check to see if it is already batched for delivery.
                request = mBatchedResponses.get(mCacheKey);
                if (request != null) {
                    request.removeContainerAndCancelIfNecessary(this);
                    if (request.mContainers.size() == 0) {
                        mBatchedResponses.remove(mCacheKey);
                    }
                }
            }
        }

        /**
         * Returns the gif associated with the request URL if it has been loaded, null otherwise.
         */
        public byte[] getGif() {
            return mGif;
        }

        /**
         * Returns the requested URL for this container.
         */
        public String getRequestUrl() {
            return mRequestUrl;
        }
    }

    /**
     * Wrapper class used to map a Request to the set of active ImageContainer objects that are
     * interested in its results.
     */
    private class BatchedGifImageRequest {
        /**
         * The request being tracked
         */
        private final Request<?> mRequest;

        /**
         * The result of the request being tracked by this item
         */
        private byte[] mResponseGif;

        /**
         * Error if one occurred for this response
         */
        private VolleyError mError;

        /**
         * List of all of the active ImageContainers that are interested in the request
         */
        private final LinkedList<GifImageContainer> mContainers = new LinkedList<GifImageContainer>();

        /**
         * Constructs a new BatchedImageRequest object
         *
         * @param request   The request being tracked
         * @param container The ImageContainer of the person who initiated the request.
         */
        public BatchedGifImageRequest(final Request<?> request, final GifImageContainer container) {
            mRequest = request;
            mContainers.add(container);
        }

        /**
         * Set the error for this response
         */
        public void setError(final VolleyError error) {
            mError = error;
        }

        /**
         * Get the error for this response
         */
        public VolleyError getError() {
            return mError;
        }

        /**
         * Adds another ImageContainer to the list of those interested in the results of
         * the request.
         */
        public void addContainer(final GifImageContainer container) {
            mContainers.add(container);
        }

        /**
         * Detatches the gif container from the request and cancels the request if no one is
         * left listening.
         *
         * @param container The container to remove from the list
         * @return True if the request was canceled, false otherwise.
         */
        public boolean removeContainerAndCancelIfNecessary(final GifImageContainer container) {
            mContainers.remove(container);
            if (mContainers.size() == 0) {
                mRequest.cancel();
                return true;
            }
            return false;
        }
    }

    /**
     * Starts the runnable for batched delivery of responses if it is not already started.
     *
     * @param cacheKey The cacheKey of the response being delivered.
     * @param request  The BatchedImageRequest to be delivered.
     */
    private void batchResponse(final String cacheKey, final BatchedGifImageRequest request) {
        mBatchedResponses.put(cacheKey, request);
        // If we don't already have a batch delivery runnable in flight, make a new one.
        // Note that this will be used to deliver responses to all callers in mBatchedResponses.
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for (final BatchedGifImageRequest bir : mBatchedResponses.values()) {
                        for (final GifImageContainer container : bir.mContainers) {
                            // If one of the callers in the batched request canceled the request
                            // after the response was received but before it was delivered,
                            // skip them.
                            if (container.mListener == null) {
                                continue;
                            }
                            if (bir.getError() == null) {
                                container.mGif = bir.mResponseGif;
                                container.mListener.onResponse(container, false);
                            } else {
                                container.mListener.onErrorResponse(bir.getError());
                            }
                        }
                    }
                    mBatchedResponses.clear();
                    mRunnable = null;
                }

            };
            // Post the runnable.
            mHandler.postDelayed(mRunnable, mBatchResponseDelayMs);
        }
    }

    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("ImageLoader must be invoked from the main thread.");
        }
    }

    /**
     * Creates a cache key for use with the L1 cache.
     *
     * @param url       The URL of the request.
     * @param maxWidth  The max-width of the output.
     * @param maxHeight The max-height of the output.
     */
    private static String getCacheKey(final String url, final int maxWidth, final int maxHeight) {
        return new StringBuilder(url.length() + 12).append("#W").append(maxWidth)
                .append("#H").append(maxHeight).append(url).toString();
    }
}
