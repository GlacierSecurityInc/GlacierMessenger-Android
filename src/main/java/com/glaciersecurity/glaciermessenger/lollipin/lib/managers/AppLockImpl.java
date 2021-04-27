package com.glaciersecurity.glaciermessenger.lollipin.lib.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.util.Base64;

import com.glaciersecurity.glaciermessenger.lollipin.lib.Log;
import com.glaciersecurity.glaciermessenger.lollipin.lib.PinActivity;
import com.glaciersecurity.glaciermessenger.lollipin.lib.PinCompatActivity;
import com.glaciersecurity.glaciermessenger.lollipin.lib.PinFragmentActivity;
import com.glaciersecurity.glaciermessenger.lollipin.lib.encryption.Encryptor;
import com.glaciersecurity.glaciermessenger.lollipin.lib.enums.Algorithm;
import com.glaciersecurity.glaciermessenger.lollipin.lib.interfaces.LifeCycleInterface;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

public class AppLockImpl<T extends AppLockActivity> extends AppLock implements LifeCycleInterface {
    public final static String IMAGE_FILENAME = "AppLockImpl.ser";
    public final static String DATABASE_NAME = "LollipinDB";
    public final static String TABLE_NAME = "Lollipin";
    public final static String STORAGE_DIRCTORY = "Messenger";

    public static final String TAG = "AppLockImpl";

    private static final String LIFE_CYCLE_LISTENER_PREFERENCE_KEY = "LIFE_CYCLE_LISTENER";

    /**
     * The {@link SharedPreferences} mkey used to store the password
     */
    private static final String EMERGENCY_PASSWORD_PREFERENCE_KEY = "EMERGENCY_PASSCODE";
    private static final String EMERGENCY_KEY = "2288";

    /**
     * The {@link SharedPreferences} mkey used to store the password
     */
    private static final String PASSWORD_PREFERENCE_KEY = "PASSCODE";
    /**
     * The {@link SharedPreferences} mkey used to store the {@link Algorithm}
     */
    private static final String PASSWORD_ALGORITHM_PREFERENCE_KEY = "ALGORITHM";
    /**
     * The {@link SharedPreferences} mkey used to store the last active time
     */
    private static final String LAST_ACTIVE_MILLIS_PREFERENCE_KEY = "LAST_ACTIVE_MILLIS";
    /**
     * The {@link SharedPreferences} mkey used to store the timeout
     */
    private static final String TIMEOUT_MILLIS_PREFERENCE_KEY = "TIMEOUT_MILLIS_PREFERENCE_KEY";
    /**
     * The {@link SharedPreferences} mkey used to store the logo resource id
     */
    private static final String LOGO_ID_PREFERENCE_KEY = "LOGO_ID_PREFERENCE_KEY";
    /**
     * The {@link SharedPreferences} mkey used to store the forgot option
     */
    private static final String SHOW_FORGOT_PREFERENCE_KEY = "SHOW_FORGOT_PREFERENCE_KEY";

    /**
     * The {@link SharedPreferences} mkey used to store the only background timeout option
     */
    private static final String ONLY_BACKGROUND_TIMEOUT_PREFERENCE_KEY = "ONLY_BACKGROUND_TIMEOUT_PREFERENCE_KEY";
    /**
     * The {@link SharedPreferences} mkey used to store whether the user has backed out of the {@link AppLockActivity}
     */
    private static final String PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY = "PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY";
    /**
     * The {@link SharedPreferences} mkey used to store the dynamically generated password salt
     */
    private static final String PASSWORD_SALT_PREFERENCE_KEY = "PASSWORD_SALT_PREFERENCE_KEY";
    /**
     * The {@link SharedPreferences} mkey used to store whether the caller has enabled fingerprint authentication.
     * This value defaults to true for backwards compatibility.
     */
    private static final String FINGERPRINT_AUTH_ENABLED_PREFERENCE_KEY = "FINGERPRINT_AUTH_ENABLED_PREFERENCE_KEY";
    /**
     * The default password salt
     */
    private static final String DEFAULT_PASSWORD_SALT = "7xn7@c$";
    /**
     * The mkey algorithm used to generating the dynamic salt
     */
    private static final String KEY_ALGORITHM = "PBEWithMD5AndDES";
    /**
     * The mkey length of the salt
     */
    private static final int KEY_LENGTH = 256;
    /**
     * The number of iterations used to generate a dynamic salt
     */
    private static final int KEY_ITERATIONS = 20;

    /**
     * The {@link SharedPreferences} used to store the password, the last active time etc...
     */
    // private SharedPreferences mSharedPreferences;

    /**
     * The activity class that extends {@link AppLockActivity}
     */
    private Class<T> mActivityClass;

    /**
     * Static instance of {@link AppLockImpl}
     */
    private static AppLockImpl mInstance;

    /**
     * Static method that allows to get back the current static Instance of {@link AppLockImpl}
     *
     * @param context       The current context of the {@link Activity}
     * @param activityClass The activity extending {@link AppLockActivity}
     * @return The instance.
     */
    public static AppLockImpl getInstance(Context context, Class<? extends AppLockActivity> activityClass) {
        synchronized (LockManager.class) {
            if (mInstance == null) {
                mInstance = new AppLockImpl<>(context, activityClass);
            }
        }
        return mInstance;
    }

    private AppLockImpl(Context context, Class<T> activityClass) {
        super();
        this.mActivityClass = activityClass;
    }

    @Override
    public void setTimeout(long timeout) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY, value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + TIMEOUT_MILLIS_PREFERENCE_KEY + "','" + String.valueOf(timeout) + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        /* SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(TIMEOUT_MILLIS_PREFERENCE_KEY, timeout);
        editor.apply();*/
    }

    public String getSalt() {
        String salt = null;

        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + PASSWORD_SALT_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                salt = c.getString(c.getColumnIndex("value"));
            }
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        // String salt = mSharedPreferences.getString(PASSWORD_SALT_PREFERENCE_KEY, null);
        if (salt == null) {
            salt = generateSalt();
            setSalt(salt);
        }
        return salt;
    }

    private void setSalt(String salt) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + PASSWORD_SALT_PREFERENCE_KEY + "','" + salt + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private String generateSalt() {;
        byte[] salt = new byte[KEY_LENGTH];
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(System.currentTimeMillis());
            sr.nextBytes(salt);
            return Arrays.toString(salt);
        } catch (Exception e) {
            salt = DEFAULT_PASSWORD_SALT.getBytes();
        }
        return Base64.encodeToString(salt, Base64.DEFAULT);
    }

    @Override
    public long getTimeout() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + TIMEOUT_MILLIS_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                lollipinDB.close();
                return Long.parseLong(c.getString(c.getColumnIndex("value")));
            } else {
                lollipinDB.close();
                return DEFAULT_TIMEOUT;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            return DEFAULT_TIMEOUT;
        }

        //return mSharedPreferences.getLong(TIMEOUT_MILLIS_PREFERENCE_KEY, DEFAULT_TIMEOUT);
    }

    @Override
    public void setLogoId(int logoId) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + LOGO_ID_PREFERENCE_KEY + "','" + Integer.valueOf(logoId) + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getLogoId() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + TIMEOUT_MILLIS_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                lollipinDB.close();
                return Integer.parseInt(c.getString(c.getColumnIndex("value")));
            } else {
                lollipinDB.close();
                return LOGO_ID_NONE;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return LOGO_ID_NONE;
        // return mSharedPreferences.getInt(LOGO_ID_PREFERENCE_KEY, LOGO_ID_NONE);
    }

    @Override
    public void setShouldShowForgot(boolean showForgot) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + SHOW_FORGOT_PREFERENCE_KEY + "','" + String.valueOf(showForgot) + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean pinChallengeCancelled() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();

                boolean bvar = Boolean.parseBoolean(c.getString(c.getColumnIndex("value")));
                lollipinDB.close();
                return bvar;
            } else {
                lollipinDB.close();
                return false;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return false;
        // return mSharedPreferences.getBoolean(PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY, false);
    }

    @Override
    public void setPinChallengeCancelled(boolean backedOut) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY + "','" + String.valueOf(backedOut) + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldShowForgot() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + SHOW_FORGOT_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                boolean bvar = Boolean.parseBoolean(c.getString(c.getColumnIndex("value")));
                lollipinDB.close();
                return bvar;
            } else {
                lollipinDB.close();
                return true;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            return true;
        }


        // return mSharedPreferences.getBoolean(SHOW_FORGOT_PREFERENCE_KEY, true);
    }

    @Override
    public boolean onlyBackgroundTimeout() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + ONLY_BACKGROUND_TIMEOUT_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount()) > 0) {
                c.moveToFirst();
                boolean bvar = Boolean.parseBoolean(c.getString(c.getColumnIndex("value")));
                lollipinDB.close();
                return bvar;
            } else {
                lollipinDB.close();
                return false;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setOnlyBackgroundTimeout(boolean onlyBackgroundTimeout) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + ONLY_BACKGROUND_TIMEOUT_PREFERENCE_KEY + "','" + String.valueOf(onlyBackgroundTimeout) + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enable() {
        PinActivity.setListener(this);
        PinCompatActivity.setListener(this);
        PinFragmentActivity.setListener(this);
    }

    @Override
    public void disable() {
        PinActivity.clearListeners();
        PinCompatActivity.clearListeners();
        PinFragmentActivity.clearListeners();
    }

    @Override
    public void disableAndRemoveConfiguration() {
        PinActivity.clearListeners();
        PinCompatActivity.clearListeners();
        PinFragmentActivity.clearListeners();

        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READWRITE);
            lollipinDB.delete(TABLE_NAME, null, null);
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }


    }

    @Override
    public long getLastActiveMillis() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + LAST_ACTIVE_MILLIS_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                lollipinDB.close();
                return Long.parseLong(c.getString(c.getColumnIndex("value")));
            } else {
                lollipinDB.close();
                return 0;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            return 0;
        }

        // return mSharedPreferences.getLong(LAST_ACTIVE_MILLIS_PREFERENCE_KEY, 0);
    }

    @Override
    public boolean isFingerprintAuthEnabled() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + FINGERPRINT_AUTH_ENABLED_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                boolean bvar = Boolean.parseBoolean(c.getString(c.getColumnIndex("value")));
                lollipinDB.close();
                return bvar;
            } else {
                lollipinDB.close();
                return true;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return true;
        // return mSharedPreferences.getBoolean(FINGERPRINT_AUTH_ENABLED_PREFERENCE_KEY, true);
    }

    @Override
    public void setFingerprintAuthEnabled(boolean enabled) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + FINGERPRINT_AUTH_ENABLED_PREFERENCE_KEY + "','" + String.valueOf(enabled) + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        /* SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(FINGERPRINT_AUTH_ENABLED_PREFERENCE_KEY, enabled);
        editor.apply();*/
    }

    @Override
    public void setLastActiveMillis() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + LAST_ACTIVE_MILLIS_PREFERENCE_KEY + "','" + String.valueOf(System.currentTimeMillis()) + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean checkPasscode(String passcode) {
        String algorithmKey = "";

        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + PASSWORD_ALGORITHM_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                algorithmKey = c.getString(c.getColumnIndex("value"));
            }
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        Algorithm algorithm = Algorithm.getFromText(algorithmKey);

        String salt = getSalt();
        passcode = salt + passcode + salt;
        passcode = Encryptor.getSHA(passcode, algorithm);
        String storedPasscode = "";
        String emergStoredPasscode = "";

        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);

            // retrieve user specified mkey
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + PASSWORD_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                storedPasscode = c.getString(c.getColumnIndex("value"));
            }

            // retrieve emergency mkey
            c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + EMERGENCY_PASSWORD_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                emergStoredPasscode = c.getString(c.getColumnIndex("value"));
            }

            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        if ((storedPasscode.equalsIgnoreCase(passcode)) || (emergStoredPasscode.equalsIgnoreCase(passcode))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean setPasscode(String passcode) {
        String salt = getSalt();
        if (passcode == null) {
            try {
                File root = android.os.Environment.getExternalStorageDirectory();
                File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
                SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
                lollipinDB.rawQuery("DELETE FROM " + TABLE_NAME + " where mkey = '" + PASSWORD_PREFERENCE_KEY + "';", null);
                lollipinDB.rawQuery("DELETE FROM " + TABLE_NAME + " where mkey = '" + EMERGENCY_PASSWORD_PREFERENCE_KEY + "';", null);
                lollipinDB.close();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }

            this.disable();
        } else {
            passcode = salt + passcode + salt;
            setAlgorithm(Algorithm.SHA256);
            passcode = Encryptor.getSHA(passcode, Algorithm.SHA256);
            /* editor.putString(PASSWORD_PREFERENCE_KEY, passcode);
            editor.apply(); */

            try {
                File root = android.os.Environment.getExternalStorageDirectory();
                File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
                SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
                lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS Lollipin(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
                lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + PASSWORD_PREFERENCE_KEY + "','" + passcode + "');");
                lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + EMERGENCY_PASSWORD_PREFERENCE_KEY + "','" + Encryptor.getSHA(salt + EMERGENCY_KEY + salt, Algorithm.SHA256) + "');");
                lollipinDB.close();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }

            this.enable();
        }

        return true;
    }

    /**
     * Set the algorithm used in {@link #setPasscode(String)}
     */
    private void setAlgorithm(Algorithm algorithm) {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openOrCreateDatabase(dir + "/" + DATABASE_NAME, null);
            lollipinDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(mkey VARCHAR PRIMARY KEY,value VARCHAR);");
            lollipinDB.execSQL("REPLACE INTO " + TABLE_NAME + " VALUES('" + PASSWORD_ALGORITHM_PREFERENCE_KEY + "','" + algorithm.getValue() + "');");
            lollipinDB.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isPasscodeSet() {
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + STORAGE_DIRCTORY);
            SQLiteDatabase lollipinDB = SQLiteDatabase.openDatabase(dir + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = lollipinDB.rawQuery("SELECT * FROM " + TABLE_NAME + " where mkey = '" + PASSWORD_PREFERENCE_KEY + "';", null);
            if ((c != null) && (c.getCount() > 0)) {
                lollipinDB.close();
                return true;
            } else {
                lollipinDB.close();
                return false;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean isIgnoredActivity(Activity activity) {
        String clazzName = activity.getClass().getName();

        // ignored activities
        if (mIgnoredActivities.contains(clazzName)) {
            Log.d(TAG, "ignore activity " + clazzName);
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldLockSceen(Activity activity) {
        // previously backed out of pin screen
        if (pinChallengeCancelled()) {
            Log.d(TAG, "Lollipin shouldLockSceen() called-1");
            return true;
        }

        // already unlock
        if (activity instanceof AppLockActivity) {
            Log.d(TAG, "Lollipin shouldLockSceen() called-2");
            AppLockActivity ala = (AppLockActivity) activity;
            if (ala.getType() == UNLOCK_PIN) {
                Log.d(TAG, "Lollipin shouldLockSceen() called-3");
                Log.d(TAG, "already unlock activity");
                return false;
            }
        }

        // no pass code set
        if (!isPasscodeSet()) {
            Log.d(TAG, "Lollipin shouldLockSceen() called-4");
            Log.d(TAG, "lock passcode not set.");
            return false;
        }

        // not enough timeout
        long lastActiveMillis = getLastActiveMillis();
        long passedTime = System.currentTimeMillis() - lastActiveMillis;
        long timeout = getTimeout();

        if (lastActiveMillis > 0 && passedTime <= timeout) {
            Log.d(TAG, "Lollipin shouldLockSceen() called-5");
            Log.d(TAG, "No lock, not enough time have passed: " + passedTime + " < " + timeout);
            return false;
        }
        Log.d(TAG, "Lollipin shouldLockSceen() called-6");

        return true;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (isIgnoredActivity(activity)) {
            return;
        }

        String clazzName = activity.getClass().getName();
        Log.d(TAG, "onActivityPaused " + clazzName);

        if ((onlyBackgroundTimeout() || !shouldLockSceen(activity)) && !(activity instanceof AppLockActivity)) {
            setLastActiveMillis();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isIgnoredActivity(activity)) {
            return;
        }

        String clazzName = activity.getClass().getName();
        Log.d(TAG, "onActivityResumed " + clazzName);

        if (shouldLockSceen(activity)) {
            Log.d(TAG, "mActivityClass.getClass() " + mActivityClass);
            Intent intent = new Intent(activity.getApplicationContext(),
                    mActivityClass);
            intent.putExtra(EXTRA_TYPE, UNLOCK_PIN);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.getApplication().startActivity(intent);
        }

        if (!shouldLockSceen(activity) && !(activity instanceof AppLockActivity)) {
            setLastActiveMillis();
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