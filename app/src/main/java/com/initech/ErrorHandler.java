package com.initech;

import android.content.Context;

import com.initech.util.AnalyticsHelper;
import com.initech.util.MLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class ErrorHandler implements java.lang.Thread.UncaughtExceptionHandler {

	private static final String TAG = ErrorHandler.class.getSimpleName();
	private Thread.UncaughtExceptionHandler defaultHandler;
	private static final String LOG_FILE_NAME = "crash.txt";
	private Context context;

	public ErrorHandler(Context context) {
		MLog.enable(TAG);
		this.context = context;
		defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		
		ex.printStackTrace();
		MLog.i(TAG, "Uncaught Exception/Error: ", ex, " In thread: ", thread);
		MLog.e(TAG, "", ex);
		try {
			// logToFile(ex);
			// MLog.i(TAG, "logs are written to file successfully");
		} catch (Exception e) {
			MLog.e(TAG, "Exception in writing crash to log file: ", e);
		}
		
		Throwable cause = ex.getCause();
		if(cause == null) {
			cause = ex;
		}

		StackTraceElement[] es = cause.getStackTrace();
		String msg = cause.toString();
		String firstLine = "";
		String lastLine = "";
		if (es != null && es.length > 0) {
			msg = msg + " at " + es[0].toString();
			StringBuffer error = new StringBuffer();
			for (StackTraceElement st : es) {
				if (st.getClassName().startsWith("com.instachat")) {
					if (firstLine.length() == 0) {
						firstLine = st.toString();
					}
					lastLine = st.toString();
				}
				error.append(st.toString());
			}
			AnalyticsHelper.logError(Events.UNCAUGHT_ERROR, error.toString(),
					cause.toString());
		}

		MLog.i(TAG, "Sending app crash event");
		HashMap<String, String> errorData = new HashMap<String, String>();
		errorData.put("Error_Msg", msg);
		errorData.put("Error_First_Line", firstLine);
		errorData.put("Error_Origin", lastLine);
		AnalyticsHelper.logEvent(Events.APP_CRASHED, errorData);
		AnalyticsHelper.onEndSession(context);
		defaultHandler.uncaughtException(thread, ex);
	}

	public void logToFile(Throwable t) throws Exception {
		File file = getCrashLogFile();
		if (file.exists())
			file.delete();
		PrintWriter pw = new PrintWriter(file);
		t.printStackTrace(pw);
		pw.flush();
		pw.close();
	}

	public ArrayList<String> getCrashReport() throws Exception {
		ArrayList<String> logs = new ArrayList<String>();
		File file = getCrashLogFile();
		if (!file.exists())
			return logs;
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = br.readLine()) != null) {
			logs.add(line);
		}
		br.close();
		return logs;
	}

	public void clearCrashLogs() {
		File file = getCrashLogFile();
		if (file.exists())
			file.delete();
	}

	private File getCrashLogFile() {
		File dir = context.getFilesDir();
		return new File(dir.getAbsolutePath() + File.separator + LOG_FILE_NAME);
	}

	public void destroy() {
		context = null;
		Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
		defaultHandler = null;
	}
}
