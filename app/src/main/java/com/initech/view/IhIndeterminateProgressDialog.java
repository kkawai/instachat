package com.initech.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.codelab.friendlychat.R;
import com.initech.util.ThreadWrapper;

final class IhIndeterminateProgressDialog extends Dialog {

	private boolean useTimer = true;
	private String message;
	private TextView messageView;
	
	public void setUserTimer(final boolean useTimer) {
		this.useTimer= useTimer;
	}
	public void setMessage(final String message) {
		this.message = message;
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				if (isShowing()) {
					try {
						dismiss();
					} catch (final Throwable t) {
					}
				}
			} catch (final Exception e) {
			}
		}
	};

	@Override
	public void show() {
		show(true);
	}
	
	public void show(final boolean doTimeout) {
		try {
			super.show();
			if (doTimeout) {
				startShowTimer();
			}
		} catch (final Exception e) {
		}
	}	

	public IhIndeterminateProgressDialog(final Context context, final int theme) {
		super(context, theme);
	}
	
	public IhIndeterminateProgressDialog(final Context context) {
		super(context);
	}	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().getDecorView().setBackgroundColor(
				getContext().getResources().getColor(
						android.R.color.transparent));
		setContentView(R.layout.ih_indeterminate_transparent_progress);
		if (message != null) {
			messageView = (TextView)findViewById(R.id.progressMessage);
			messageView.setText(message);
			messageView.setVisibility(View.VISIBLE);
		} else {
			messageView.setVisibility(View.GONE);
		}
	}

	/*
	 * Don't allow this crap to display longer than 6 seconds no matter what!
	 * Nobody has patience anymore. There's still parts of the code that don't
	 * call dismiss in some situations and no time to debug that.
	 */
	private void startShowTimer() {
		if (!useTimer) {
			return;
		}
		ThreadWrapper.executeInWorkerThread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(6000);
					handler.sendEmptyMessage(0);
				} catch (Exception e) {
				}
			}
		});
	}
	
	@Override
	public void dismiss() {
		try {
			super.dismiss();
		}catch(final Throwable t){}
	}
}
