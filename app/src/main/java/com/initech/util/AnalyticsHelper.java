package com.initech.util;

import android.content.Context;

import com.flurry.android.FlurryAgent;
import com.initech.Constants;
import com.initech.ErrorHandler;
import com.initech.Events;

import java.util.Map;

public class AnalyticsHelper {

	private static final String TAG = AnalyticsHelper.class.getSimpleName();
	private static ErrorHandler errorHandler;
	private static Context currentContext;

	public static synchronized void onStartSession(final Context context) {

		ThreadWrapper.executeInWorkerThread(new Runnable() {

			@Override
			public void run() {
				if (currentContext != null) {
					onEndSession(currentContext);
				}
				MLog.i(TAG, "Event: start session");
				FlurryAgent.setCaptureUncaughtExceptions(false);
				//errorHandler = new ErrorHandler(context);
				//Thread.setDefaultUncaughtExceptionHandler(errorHandler);
				FlurryAgent.onStartSession(context, Constants.FLURRY_KEY);
				currentContext = context;
			}

		});

	}

	public static synchronized void onEndSession(final Context context) {

		ThreadWrapper.executeInWorkerThread(new Runnable() {

			@Override
			public void run() {
				// If the current context is null that means the session is not
				// running.
				// If the context is not the same context which was used to
				// start
				// session, get out.
				if (currentContext == null || currentContext != context)
					return;
				MLog.i(TAG, "Event: end session");
				FlurryAgent.onEndSession(currentContext);
				if (errorHandler != null) {
					errorHandler.destroy();
					errorHandler = null;
				}
				currentContext = null;
			}
		});

	}

	public static void logEvent(final String name) {

		ThreadWrapper.executeInWorkerThread(new Runnable() {

			@Override
			public void run() {
				MLog.i(TAG, "Event: ", name);
				FlurryAgent.logEvent(name);
			}
		});

	}

	public static void logEvent(final String name, final boolean isTimed) {

		ThreadWrapper.executeInWorkerThread(new Runnable() {

			@Override
			public void run() {
				MLog.i(TAG, "Event: ", name, isTimed);
				FlurryAgent.logEvent(name, isTimed);
			}
		});

	}

	public static void logEvent(final String name, final Map<String, String> params) {

		ThreadWrapper.executeInWorkerThread(new Runnable() {

			@Override
			public void run() {
				MLog.i(TAG, "Event: ", name, " : ", params);
				FlurryAgent.logEvent(name, params);
			}
		});

	}

	public static void endTimedEvent(final String name) {

		ThreadWrapper.executeInWorkerThread(new Runnable() {

			@Override
			public void run() {
				MLog.i(TAG, "Timed Event: ", name);
				FlurryAgent.endTimedEvent(name);
			}
		});

	}

	public static void logError(final String errorId, final String msg, final String errorClass) {

		ThreadWrapper.executeInWorkerThread(new Runnable() {

			@Override
			public void run() {
				FlurryAgent.onError(errorId, msg, errorClass);
			}
		});

	}

	public static void destroyErrorHandler() {

		ThreadWrapper.executeInWorkerThread(new Runnable() {

			@Override
			public void run() {
				if (errorHandler != null) {
					errorHandler.destroy();
				}
			}
		});

	}
	
	public static void logException(final String className, final Throwable t) {
		try {
			FlurryAgent.onError(Events.APP_THROWABLE_TRAPPED, className, t);
			MLog.e(className, className + " failed: ", t);
		}catch(final Throwable x) {
			MLog.e(TAG, "", x);
		}
	}
}
