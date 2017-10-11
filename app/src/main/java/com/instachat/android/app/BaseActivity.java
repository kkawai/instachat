package com.instachat.android.app;

import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import com.instachat.android.R;
import com.instachat.android.app.activity.ActivityState;
import com.instachat.android.MyApp;

import java.util.LinkedList;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * Created by kevin on 8/9/2016.
 */
public class BaseActivity extends AppCompatActivity implements ActivityState {
    private boolean mIsDestroyed; //for <= sdk 16
    private List<Disposable> disposableList = new LinkedList<>();

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        mIsDestroyed = false;
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onDestroy() {
        MyApp.getInstance().getRequestQueue().cancelAll(this);
        mIsDestroyed = true;
        for (Disposable disposable : disposableList) {
            disposable.dispose();
        }
        super.onDestroy();
    }

    @Override
    public boolean isActivityDestroyed() {
        if (Build.VERSION.SDK_INT >= 17)
            return isDestroyed() || isFinishing();
        return mIsDestroyed || isFinishing();
    }

    public void addDisposable(Disposable disposable) {
        disposableList.add(disposable);
    }

    public void showSmallProgressCircle() {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (progressBar != null && progressBar.getVisibility() != View.VISIBLE) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    public void hideSmallProgressCircle() {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (progressBar != null && progressBar.getVisibility() != View.GONE) {
            progressBar.setVisibility(View.GONE);
        }
    }
}
