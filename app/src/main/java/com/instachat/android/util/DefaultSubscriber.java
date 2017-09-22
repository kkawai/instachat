package com.instachat.android.util;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Subscriber;

/**
 * A default RX subscriber which provides basic logging and catches exceptions thrown from message
 * handlers. This class also adds a "before any" event handler, which will be called immediately
 * before the first call to handleOnNext, handleOnError, or handleOnCompleted.
 *
 * @param <T> the type of items the Subscriber expects to observe
 */
public class DefaultSubscriber<T> extends Subscriber<T> {

    private static final String TAG = "DefaultSubscriber";

    /**
     * Used for logging, to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     */
    private final String tag;

    /**
     * True if no events have been emitted yet. Used to control calls to handleBeforeAny.
     */
    private final AtomicBoolean beforeFirstEvent = new AtomicBoolean(true);

    /**
     * Create a new DefaultSubscriber with the given tag used for logging.
     *
     * @param tag Used for logging, to identify the source of a log message.
     */
    public DefaultSubscriber(String tag) {
        super();
        if (tag != null && tag.length() > 0)
            this.tag = tag;
        else
            this.tag = TAG;
    }

    @Override
    public final void onCompleted() {
        onBeforeAny();
        MLog.d(tag, "onCompleted");
        try {
            handleOnCompleted();
        } catch (Throwable t) {
            MLog.e(tag, "Exception thrown from handleOnCompleted", t);
        }
    }

    @Override
    public final void onError(Throwable error) {
        onBeforeAny();
        MLog.e(tag, "onError", error);
        try {
            handleOnError(error);
        } catch (Throwable t) {
            MLog.e(tag, "Exception thrown from handleOnError", t);
        }
    }

    @Override
    public final void onNext(T value) {
        onBeforeAny();
        MLog.d(tag, "onNext");
        try {
            handleOnNext(value);
        } catch (Throwable t) {
            MLog.e(tag, "Exception thrown from handleOnNext", t);
        }
    }

    private final void onBeforeAny() {
        // Use beforeFirstEvent to call onBeforeAny only once.
        if (!beforeFirstEvent.getAndSet(false)) {
            return;
        }
        MLog.d(tag, "onBeforeAny");
        try {
            handleBeforeAny();
        } catch (Throwable t) {
            MLog.e(tag, "Exception thrown from handleBeforeAny", t);
        }
    }

    /**
     * Notifies the Observer before the first call to onNext, onCompleted, or onError.
     * <p>
     * This method will be called at most one time, and will only be called prior to a call to one
     * of the other handler methods.
     */
    public void handleBeforeAny() {
    }

    /**
     * Provides the Observer with a new item to observe.
     * <p>
     * The {@link rx.Observable} may call this method 0 or more times.
     * <p>
     * The {@code Observable} will not call this method again after it calls either {@link #onCompleted} or
     * {@link #onError}.
     *
     * @param value the item emitted by the Observable
     */
    public void handleOnNext(T value) {
    }

    /**
     * Notifies the Observer that the {@link rx.Observable} has experienced an error condition.
     * <p>
     * If the {@link rx.Observable} calls this method, it will not thereafter call {@link #onNext} or
     * {@link #onCompleted}.
     *
     * @param error the exception encountered by the Observable
     */
    public void handleOnError(Throwable error) {
    }

    /**
     * Notifies the Observer that the {@link rx.Observable} has finished sending push-based notifications.
     * <p>
     * The {@link rx.Observable} will not call this method if it calls {@link #onError}.
     */
    public void handleOnCompleted() {
    }
}
