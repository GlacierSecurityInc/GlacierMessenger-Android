package com.glaciersecurity.glaciermessenger.lollipin.lib.interfaces;

import android.app.Activity;

import com.glaciersecurity.glaciermessenger.lollipin.lib.PinActivity;
import com.glaciersecurity.glaciermessenger.lollipin.lib.managers.AppLockActivity;
import com.glaciersecurity.glaciermessenger.lollipin.lib.managers.AppLockImpl;

import java.io.Serializable;

/**
 * Created by stoyan on 1/12/15.
 * Allows to follow the LifeCycle of the {@link PinActivity}
 * Implemented by {@link AppLockImpl} in order to
 * determine when the app was launched for the last time and when to launch the
 * {@link AppLockActivity}
 */
public interface LifeCycleInterface extends Serializable {

    /**
     * Called in {@link android.app.Activity#onResume()}
     */
    public void onActivityResumed(Activity activity);

    /**
     * Called in {@link android.app.Activity#onPause()}
     */
    public void onActivityPaused(Activity activity);
}
