package com.instachat.android.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.instachat.android.R;
import com.instachat.android.util.DefaultSubscriber;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

final class IhIndeterminateProgressDialog extends Dialog {

    private boolean useTimer = true;
    private String message;
    private TextView messageView;

    public void setUserTimer(final boolean useTimer) {
        this.useTimer = useTimer;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

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
            messageView = (TextView) findViewById(R.id.progressMessage);
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
        Observable
                .timer(6, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultSubscriber<Long>("progress dialog timer") {
                    @Override
                    public void handleOnCompleted() {
                        dismiss();
                    }
                });
    }

    @Override
    public void dismiss() {
        try {
            if (isShowing()) {
                super.dismiss();
            }
        } catch (final Throwable t) {
        }
    }
}
