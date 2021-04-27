package com.glaciersecurity.glaciermessenger.cognito;

import android.content.Context;
import android.os.Environment;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.utils.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by glaciersecurity on 12/5/17.
 */

public class BackupAccountManager {
    // used for encrypt/decrypt
    final private static byte[] SALT = "C2SEA".getBytes();
    final private static char[] PASSPHRASE = {'G','L','A','C','I','E','R'};
    final private static String VOICE_ACCOUNT_FILENAME = "voiceAccount";
    final private static String MESSENGER_ACCOUNT_FILENAME = "messengerAccount";
    final private static String ALGORITHM = "AES/CBC/PKCS5Padding";
    final private static String ACCOUNT_FIELD_DELIMITER = "\n";
    final private static String ACCOUNT_DELIMITER = "<ACCOUNT>";

    final public static String COGNITO_USERNAME_KEY = "cognitousername";
    final public static String COGNITO_PASSWORD_KEY = "cognitopassword";
    final public static String COGNITO_ORGANIZATION_KEY = "cognitoorganization";
    final public static String USERNAME_KEY = "username";
    final public static String EXTENSION_KEY = "extension";
    final public static String PASSWORD_KEY = "password";
    final public static String DISPLAYNAME_KEY = "displayname";
    final public static String CONNECTION_KEY = "connection";
    final public static String EXTERNALNUMBER_KEY = "externalnumber";

    final public static String HA1_KEY = "ha1";
    final public static String REALM_KEY = "realm";


    // index to array for fields
    final public static int USERNAME_INDEX = 0;
    final public static int PASSWORD_INDEX = 1;
    final public static int EXTERNALNUMBER_INDEX = 2;
    final public static int CONNECTION_INDEX = 5;

    // determine if external directory or apps internal directory
    final public static int LOCATION_PUBLIC = 0;
    final public static int LOCATION_PRIVATE = 1;
    final public static int APPTYPE_VOICE = 0;
    final public static int APPTYPE_MESSENGER = 1;

    private static SecretKey key = null;
    private Context context = null;

    /**
     * Constructor
     *
     * @param context
     */
    public BackupAccountManager(Context context) {
        try {
            this.context = context;

            key = generateKey(PASSPHRASE, SALT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return account file depending on request for private or public directory and app type
     *
     * @return
     */
    private File getAccountFile() {
        return new File(context.getFilesDir(), MESSENGER_ACCOUNT_FILENAME);
    }

    /**
     * Generate encryption key
     *
     * @param passphraseOrPin
     * @param salt
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static SecretKey generateKey(char[] passphraseOrPin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Number of PBKDF2 hardening rounds to use. Larger values increase
        // computation time. You should select a value that causes computation
        // to take >100ms.
        final int iterations = 1000;

        // Generate a 256-bit key
        final int outputKeyLength = 256;

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations, outputKeyLength);
        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
        return secretKey;
    }

    /**
     * Decode specified file
     *
     * @param key
     * @param fileData
     * @return
     * @throws Exception
     */
    private static byte[] decodeData(SecretKey key, byte[] fileData)
            throws Exception {
        byte[] decrypted = null;
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        decrypted = cipher.doFinal(fileData);
        return decrypted;
    }


    /**
     * Read encrypted file
     *
     * @param encryptedFileName
     * @return
     */
    private byte[] readFile(String encryptedFileName) {
        byte[] contents = null;

        File file = new File(encryptedFileName);
        int size = (int) file.length();
        contents = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(
                    new FileInputStream(file));
            try {
                buf.read(contents);
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return contents;
    }


    /**
     * Delete public file
     *
     * @return
     */
    public boolean deleteAccountFiles() {
        boolean success = true;
        File privfile = new File(context.getFilesDir(), MESSENGER_ACCOUNT_FILENAME);

        if ((privfile != null) && (privfile.exists())) {
            success = privfile.delete();
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File pubfile = new File(dir + File.separator + MESSENGER_ACCOUNT_FILENAME);
        if ((pubfile != null) && (pubfile.exists())) {
            if (!pubfile.delete()) {
                success = false;
            }
        }

        return success;
    }

    /**
     * Decode file in external or internal directory depending on location value
     *
     * @return
     */
    private String decodeFile() {

        File file = getAccountFile();

        try {
            // check if return valid file
            if ((file != null) && (file.exists())) {
                byte[] decodedData = decodeData(key, readFile(file.toString()));
                return new String(decodedData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieve account information
     *
     * @return
     */
    public AccountInfo getAccountInfo() {
        AccountInfo accountInfo = new AccountInfo();

        String fileStr = decodeFile();

        if (fileStr != null) {
            // divide accounts into array
            String[] accounts = fileStr.split(ACCOUNT_DELIMITER);

            if (accounts.length > 0) {
                // Store cognito account information
                Account account = storeAccountAttributes(accounts[0]);
                accountInfo = new AccountInfo(account);

                // loop through all possible accounts
                for (int i = 1; i < accounts.length; i++) {
                    account = storeAccountAttributes(accounts[i]);
                    accountInfo.addAccount(account);
                }
            }
            return accountInfo;
        } else {
            Log.d(Config.LOGTAG, "No configuration found.");
        }
        return null;
    }

    private Account storeAccountAttributes(String accountStr) {
        Account account = new Account();
        String[] accountFields = accountStr.split(ACCOUNT_FIELD_DELIMITER);

        if (accountFields.length > 0) {

            for (int i = 0; i < accountFields.length; i++) {
                String[] accountValues = accountFields[i].split("=");

                if (accountValues.length > 1) {
                    account.addAttribute(accountValues[0], accountValues[1]);
                }

            }
        } else {
            return null;
        }
        return account;
    }

    /**
     * Structure that holds account information
     */
    public class AccountInfo {

        Account cognitoAccount = null;
        ArrayList<Account> accounts = new ArrayList<Account>();

        public AccountInfo() {
        }

        /**
         * Add Cognito account
         *
         * @param account
         */
        public AccountInfo(Account account) {
            cognitoAccount = account;
        }

        public Account getCognitoAccount() {
            return cognitoAccount;
        }

        /**
         * Add app account to cognito account
         *
         * @param account
         */
        public void addAccount(Account account) {
            accounts.add(account);
        }

        /**
         * Return list of accounts
         *
         * @return
         */
        public ArrayList<Account> getAccounts() {
            return accounts;
        }
    }

    public class Account {
        HashMap<String, String> accountAttr = new HashMap<String, String>();

        /**
         * Constructore
         */
        public Account() {

        }

        /**
         * Add key/value pair
         *
         * @param key
         * @param value
         */
        public void addAttribute(String key, String value) {
            accountAttr.put(key, value);
        }

        /**
         * Get value based on key
         *
         * @param key
         * @return
         */
        public String getAttribute(String key) {
            return accountAttr.get(key);
        }
    }
}