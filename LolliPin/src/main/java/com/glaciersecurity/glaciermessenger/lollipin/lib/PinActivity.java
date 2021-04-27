package com.glaciersecurity.glaciermessenger.lollipin.lib;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.glaciersecurity.glaciermessenger.lollipin.lib.interfaces.LifeCycleInterface;
import com.glaciersecurity.glaciermessenger.lollipin.lib.managers.AppLockActivity;
import com.glaciersecurity.glaciermessenger.lollipin.lib.managers.AppLockImpl;
import com.glaciersecurity.glaciermessenger.lollipin.lib.managers.LockManager;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by stoyan and olivier on 1/12/15.
 * You must extend this Activity in order to support this library.
 * Then to enable PinCode blocking, you must call
 * {@link LockManager#enableAppLock(android.content.Context, Class)}
 */
public class PinActivity extends Activity {
    private static LifeCycleInterface mLifeCycleListener;
    private final BroadcastReceiver mPinCancelledReceiver;
    private static boolean newInstall = false;  // DO NOT CHANGE INITIAL VALUE

    private static final String LIFE_CYCLE_LISTENER_PREFERENCE_KEY = "LIFE_CYCLE_LISTENER";

    public PinActivity() {
        super();
        mPinCancelledReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TAG", "PIN onCreate()");
        IntentFilter filter = new IntentFilter(AppLockActivity.ACTION_CANCEL);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPinCancelledReceiver, filter);

        // check if permissions are granted before accessing database and image file
        if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED) {
            // if new install delete all data associated with PIN
            if (newInstall) {
                // remove serialized object capture
                deleteImageFiles();

                newInstall = false;
            } else
                mLifeCycleListener = getImageFile();
        } else {
            // this is how we trick this to thinking it's a new install
            // if it requires permissions, then it's a new install
            // This is to help distinguish closing app vs reinstalling app
            newInstall = true;
        }
    }

    @Override
    protected void onResume() {
        if (mLifeCycleListener != null) {
            mLifeCycleListener.onActivityResumed(PinActivity.this);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mLifeCycleListener != null) {
            mLifeCycleListener.onActivityPaused(PinActivity.this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("TAG", "PIN onDestroy()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPinCancelledReceiver);

        if (mLifeCycleListener != null) {
            saveImageFile();
        }
    }

    public static void setListener(LifeCycleInterface listener) {
        if (mLifeCycleListener != null) {
            mLifeCycleListener = null;
        }

        mLifeCycleListener = listener;
    }

    public static void clearListeners() {
        mLifeCycleListener = null;
        deleteImageFiles();
    }

    public static boolean hasListeners() {
        return (mLifeCycleListener != null);
    }

    /**
     * delete file associated with PIN image to include database
     */
    private static void deleteImageFiles() {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/" + AppLockImpl.STORAGE_DIRCTORY);
        File file = new File(dir, AppLockImpl.IMAGE_FILENAME);
        file.delete();

        file = new File(dir, AppLockImpl.DATABASE_NAME);
        file.delete();

        file = new File(dir, AppLockImpl.DATABASE_NAME + "-journal");
        file.delete();
    }

    /**
     * get file associated with PIN image
     */
    private static LifeCycleInterface getImageFile() {
        LifeCycleInterface tmpInterface = null;
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/" + AppLockImpl.STORAGE_DIRCTORY);

        File file = new File(dir, AppLockImpl.IMAGE_FILENAME);

        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fis);
                tmpInterface = (AppLockImpl) in.readObject();
                in.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return tmpInterface;
    }

    /**
     * save file associated with PIN image
     */
    private static void saveImageFile() {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/" + AppLockImpl.STORAGE_DIRCTORY);
        dir.mkdirs();
        File file = new File(dir, AppLockImpl.IMAGE_FILENAME);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(mLifeCycleListener);
            out.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ClassTypeAdapter extends TypeAdapter<Class<?>> {
        @Override
        public void write(JsonWriter jsonWriter, Class<?> clazz) throws IOException {
            if(clazz == null){
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.value(clazz.getName());
        }

        @Override
        public Class<?> read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            Class<?> clazz = null;
            try {
                clazz = Class.forName(jsonReader.nextString());
            } catch (ClassNotFoundException exception) {
                throw new IOException(exception);
            }
            return clazz;
        }
    }

    public class ClassTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            if(!Class.class.isAssignableFrom(typeToken.getRawType())) {
                return null;
            }
            return (TypeAdapter<T>) new ClassTypeAdapter();
        }
    }
}