package com.google.firebase.codelab.friendlychat;

import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by kevin on 8/9/2016.
 */
public class BaseActivity extends AppCompatActivity {
    private boolean mIsDestroyed;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        mIsDestroyed = false;
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onDestroy() {
        mIsDestroyed = true;
        super.onDestroy();
    }

    boolean isActivityDestroyed() {
        if (Build.VERSION.SDK_INT >= 17)
            return isDestroyed();
        return mIsDestroyed || isFinishing();
    }
}
