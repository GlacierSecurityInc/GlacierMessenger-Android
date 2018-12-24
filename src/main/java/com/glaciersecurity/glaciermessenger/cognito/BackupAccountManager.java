package com.glaciersecurity.glaciermessenger.cognito;

import android.content.Context;
import android.os.Environment;

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
    private File getAccountFile(int location, int appType) {
        if (appType == APPTYPE_VOICE) {
            if (location == LOCATION_PUBLIC) {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                return new File(dir + File.separator + VOICE_ACCOUNT_FILENAME);
            } else if (location == LOCATION_PRIVATE) {
                return new File(context.getFilesDir(), VOICE_ACCOUNT_FILENAME);
            }
        } else if (appType == APPTYPE_MESSENGER) {
            if (location == LOCATION_PUBLIC) {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                return new File(dir + File.separator + MESSENGER_ACCOUNT_FILENAME);
            } else if (location == LOCATION_PRIVATE) {
                return new File(context.getFilesDir(), MESSENGER_ACCOUNT_FILENAME);
            }
        }
        return null;
    }

    /**
     * determine if public directory is writeable
     *
     * @return
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * determine if public directory is readable
     *
     * @return
     */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
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
     * Encrypt file
     *
     * @param key
     * @param fileData
     * @return
     * @throws Exception
     */
    private static byte[] encodeData(SecretKey key, byte[] fileData)
            throws Exception {
        byte[] encrypted = null;
        byte[] data = key.getEncoded();
        SecretKeySpec skeySpec = new SecretKeySpec(data, 0, data.length,
                ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(
                new byte[cipher.getBlockSize()]));
        encrypted = cipher.doFinal(fileData);
        return encrypted;
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
     * Add account to string.  Check to see if already exists and then
     * stick account to end of string.  This is only applicable to Voice since it can
     * retain multiple accounts at the same time.
     *
     * ** OBSOLETE SINCE WE ONLY USE ACCOUNT FROM COGNITO **
     *
     * @param accountStr
     * @param accountInfo
     * @return
     */
    private String addAccountToString(String accountStr, AccountInfo accountInfo) {
        // check if there are any existing accounts.  Primary accounts are at the
        // end so since we probably created a new account, we tack it at the end
        /* if ((accountStr != null) && (accountStr.length() > 0)) {
            boolean accountExists = false;
            String[] accountList = accountStr.split("\n");

            /// check if account exists already
            for (int i = 0; i < accountList.length; i++) {
                String[] account = accountList[i].split(ACCOUNT_FIELD_DELIMITER);
                if ((account != null) && (account.length > 0)) {
                    if (accountInfo.getExtension().compareTo(account[0]) == 0) {
                        accountExists = true;
                    }
                }
            }

            // if account doesn't already exist, add it to the list
            if (!accountExists) {
                accountStr = accountStr +
                        accountInfo.getCustomExtension("null") + ACCOUNT_FIELD_DELIMITER +
                        accountInfo.getCustomPassword("null") + ACCOUNT_FIELD_DELIMITER +
                        accountInfo.getCustomHa1("null") + ACCOUNT_FIELD_DELIMITER +
                        accountInfo.getCustomExternalNumber("null") + ACCOUNT_FIELD_DELIMITER +
                        accountInfo.getCustomRealm("null") + ACCOUNT_FIELD_DELIMITER +
                        accountInfo.getCustomConnection("null") + "\n";
            }
        } else {
            accountStr = accountInfo.getCustomExtension("null") + ACCOUNT_FIELD_DELIMITER +
                    accountInfo.getCustomPassword("null") + ACCOUNT_FIELD_DELIMITER +
                    accountInfo.getCustomHa1("null") + ACCOUNT_FIELD_DELIMITER +
                    accountInfo.getCustomExternalNumber("null") + ACCOUNT_FIELD_DELIMITER +
                    accountInfo.getCustomRealm("null") + ACCOUNT_FIELD_DELIMITER +
                    accountInfo.getCustomConnection("null") + "\n";
        }

        return accountStr;*/
        return null;
    }

    /**
     * Save account information to file.  Check to see if already exists and then
     * stick account to end of file.  This is only applicable to Voice since it can
     * retain multiple accounts at the same time.
     *
     * ** OBSOLETE SINCE WE ONLY USE ACCOUNT FROM COGNITO **
     *
     * @return
     */
    public boolean addAccountToFile(Account account, int location, int appType) {
        return true;
    }

    /**
     * Encode data and save to file
     *
     * @param data
     * @return
     */
    private boolean encryptAndSaveToFile(String data, File accountFile) {
        // check if destination is writeable
        if ((isExternalStorageWritable()) && (isExternalStorageReadable())) {
            // save data to file
            try {
                // create path if it doesn't already exist
                File dir = new File(accountFile.getAbsolutePath()).getParentFile();

                // directory doesn't exist, make it
                if (!dir.exists()) {
                    dir.mkdir();
                }

                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(accountFile));
                byte[] fileBytes = encodeData(key, data.getBytes());
                bos.write(fileBytes);
                bos.flush();
                bos.close();
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Create messenger config file in public directory from Cognito config file
     *
     * @param file
     * @return
     */
    public void createAppConfigFile(File file, String usr, String passwd, String orgID, int location, int appType) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            // save cognito information
            String buffer = COGNITO_USERNAME_KEY + "=" + usr + "\n" + COGNITO_PASSWORD_KEY + "=" + passwd + "\n" + COGNITO_ORGANIZATION_KEY + "=" + orgID + "\n";
            String line = br.readLine();

            // save app account information
            // we know there's only one account
            buffer = buffer + ACCOUNT_DELIMITER + "\n";
            while (line != null) {
                buffer = buffer + line + "\n";
                line = br.readLine();
            }

            br.close();

            encryptAndSaveToFile(buffer, getAccountFile(location, appType));
        } catch (FileNotFoundException e) {
            Log.d("GOOBER", "File does not exist: " + file.toString());
        } catch (IOException e) {
            Log.d("GOOBER", "Problem reading file: " + file.toString());
        }
    }

    /**
     * Copy file from public directory that Cognito created to app's private directory
     *
     * @return
     */
    public boolean copyAccountFileFromPublicToPrivate(int appType) {
        // make sure public file exists and accessible
        if (isExternalStorageReadable() && getAccountFile(LOCATION_PUBLIC, appType).exists()) {
            byte[] readBytes = readFile(getAccountFile(LOCATION_PUBLIC, appType).toString());

            try {
                FileOutputStream outputStream = new FileOutputStream(getAccountFile(LOCATION_PRIVATE, appType));
                outputStream.write(readBytes);
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
        return true;
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
     * Return whether account file in public/private directory exists
     *
     * @return
     */
    public boolean accountFileExists(int location, int app_type) {

        File file = getAccountFile(location, app_type);
        if ((file != null) && (file.exists())) {
            return true;
        }

        return false;
    }

    /**
     * Primary account switched so find account information and stick it to the
     * bottom of the list.
     *
     * @param username
     */
    public boolean switchPrimaryAccount(String username, int location, int appType) {
        String accounts[] = null;
        String primaryAccount = null;
        String newAccountStr = "";

        // retrieve entire file
        String accountStr = decodeFile(location, appType);

        // confirm there is data available
        if ((accountStr != null) && (accountStr.length() > 0)) {
            accounts = accountStr.split("\n");

            // look for primary account
            for (int i = 0; i < accounts.length; i++) {
                String[] account = accounts[i].split(ACCOUNT_FIELD_DELIMITER);
                if (account.length > 0) {

                    // check if primary account and save it for last
                    if (account[0].compareTo(username) == 0) {
                        primaryAccount = accounts[i] + "\n";
                    } else {
                        newAccountStr = newAccountStr + accounts[i] + "\n";
                    }
                }
            }

            // add primary account last
            newAccountStr = newAccountStr + primaryAccount;

            return encryptAndSaveToFile(newAccountStr, getAccountFile(location, appType));
        }

        return false;
    }


    /**
     * Delete public file
     *
     * @return
     */
    public boolean deleteAccountFile(int location, int app_type) {
        File file = getAccountFile(location, app_type);

        if ((file != null) && (file.exists())) {
            if (file.delete()) {
                return true;
            };
            return false;  // failed to delete
        }
        return true;  // file doesn't exist, nothing to delete
    }

    /**
     * Decode file in external or internal directory depending on location value
     *
     * @param location
     * @return
     */
    private String decodeFile(int location, int app_type) {

        File file = getAccountFile(location, app_type);

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
     * @param location
     * @param appType
     * @return
     */
    public AccountInfo getAccountInfo(int location, int appType) {
        AccountInfo accountInfo = new AccountInfo();

        String fileStr = decodeFile(location, appType);

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
            //
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

        public String getCustomAttribute(String key, String replacementValue) {
            String value = accountAttr.get(key);

            if (value != null) {
                return null;
            } else {
                return replacementValue;
            }
        }
    }
}