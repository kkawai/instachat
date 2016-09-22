package com.instachat.android.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.instachat.android.R;

public class IhProgressiveProgressDialog extends Dialog {
	
	private ProgressBar progress;
	private DisplayMetrics dm;
	private int max;

	public IhProgressiveProgressDialog(final Context context, final int theme) {
		super(context, theme);
		dm = context.getResources().getDisplayMetrics();
	}
	
	public IhProgressiveProgressDialog(final Context context) {
		super(context, 0);
		dm = context.getResources().getDisplayMetrics();
	}	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ih_progressive_transparent_progress);
		getWindow().getDecorView().setBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));
		progress = (ProgressBar)findViewById(R.id.progress);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dm.widthPixels-100, LayoutParams.WRAP_CONTENT);
		progress.setLayoutParams(params);
	}
	
	public void setProgress(final int update) {
		progress.setProgress(update);
	}
	
	public void setMax(final int max) {
		this.max = max;
	}

	@Override
	public void show() {		
		super.show();
		progress.setMax(max);
	}

}