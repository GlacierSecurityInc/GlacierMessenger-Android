package com.glaciersecurity.glaciermessenger.lollipin.lib.views;

import androidx.appcompat.app.AppCompatActivity;

import com.glaciersecurity.glaciermessenger.lollipin.lib.interfaces.LifeCycleInterface;
import com.glaciersecurity.glaciermessenger.lollipin.lib.managers.LockManager;

/**
 * Created by stoyan and olivier on 1/12/15.
 * You must extend this Activity in order to support this library.
 * Then to enable PinCode blocking, you must call
 * {@link LockManager#enableAppLock(android.content.Context, Class)}
 */
public class PinActionBarActivity extends AppCompatActivity {
    private static LifeCycleInterface mLifeCycleListener;

    @Override
    protected void onResume() {
        if (mLifeCycleListener != null) {
            mLifeCycleListener.onActivityResumed(PinActionBarActivity.this);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mLifeCycleListener != null) {
            mLifeCycleListener.onActivityPaused(PinActionBarActivity.this);
        }
        super.onPause();
    }

    public static void setListener(LifeCycleInterface listener) {
        if (mLifeCycleListener != null) {
            mLifeCycleListener = null;
        }
        mLifeCycleListener = listener;
    }

    public static void clearListeners() {
        mLifeCycleListener = null;
    }

    public static boolean hasListeners() {
        return (mLifeCycleListener != null);
    }
}
