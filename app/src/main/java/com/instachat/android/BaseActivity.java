package com.instachat.android;

import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by kevin on 8/9/2016.
 */
public class BaseActivity extends AppCompatActivity implements ActivityState {
    private boolean mIsDestroyed;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        mIsDestroyed = false;
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onDestroy() {
        MyApp.getInstance().getRequestQueue().cancelAll(this);
        mIsDestroyed = true;
        super.onDestroy();
    }

    @Override
    public boolean isActivityDestroyed() {
        if (Build.VERSION.SDK_INT >= 17)
            return isDestroyed() || isFinishing();
        return mIsDestroyed || isFinishing();
    }
}
