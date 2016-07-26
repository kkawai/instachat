package com.initech.util;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

public class ThreadWrapper {

	private static final String TAG = ThreadWrapper.class.getSimpleName();

	static Handler mHandler;

	public static void init() {
		mHandler = new Handler();
	}
	
	public static void executeInWorkerThread(final Runnable task) {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			// UI thread
			Thread worker = new Thread() {
				public void run() {
					try {
						task.run();
					} catch (Throwable t) {
						MLog.e(TAG, "uncaught error in thread", t);
					}
				}
			};
			worker.start();
		} else {
			// Worker
			try {
				task.run();
			} catch (Throwable t) {
				MLog.e(TAG, "uncaught error in thread", t);
			}
		}
	}

	public static void executeInUiThread(final Runnable task) {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			// Worker thread
			mHandler.post(new Runnable() {
				public void run() {
					try {
						task.run();
					} catch (Throwable t) {
						MLog.e(TAG, "uncaught error in ui thread", t);
					}
				}
			});
		} else {
			// UI thread
			task.run();
		}
	}

	public static void executeInUiThread(Activity activity, Runnable task) {
		executeInUiThread(task);
	}

}
