package com.glaciersecurity.glaciermessenger.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import androidx.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.util.IOUtils;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;
import com.glaciersecurity.glaciermessenger.cognito.Constants;
import com.glaciersecurity.glaciermessenger.cognito.PropertyLoader;
import com.glaciersecurity.glaciermessenger.cognito.Util;
import com.glaciersecurity.glaciermessenger.databinding.DialogPresenceBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.CognitoAccount;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.persistance.FileBackend;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.ui.util.ActivityResult;
import com.glaciersecurity.glaciermessenger.ui.util.Attachment;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.PresenceSelector;
import com.glaciersecurity.glaciermessenger.utils.FileUtils;
import com.glaciersecurity.glaciermessenger.utils.MimeUtils;
import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.meetingIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.sickIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.travelIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.StatusMessage.vacationIcon;
import static com.glaciersecurity.glaciermessenger.entities.Presence.getEmojiByUnicode;

public class FileSafeActivity extends XmppActivity implements ConnectivityReceiver.ConnectivityReceiverListener{

    private ImageView filesafeImage;
    private TextView hintOrWarning;
    private TextView uploadFilesList;
    private Button cancelButton;
    private Button uploadButton;
    private boolean uploading = false;
    private ConnectivityReceiver connectivityReceiver;

    private LinearLayout offlineLayout;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filesafe);
        setSupportActionBar(findViewById(R.id.toolbar));

        this.filesafeImage = findViewById(R.id.filesafe_image);
        this.offlineLayout = findViewById(R.id.offline_layout);
        this.cancelButton = findViewById(R.id.cancel_button);
        this.uploadButton = findViewById(R.id.upload_button);
        this.hintOrWarning = findViewById(R.id.hint_or_warning);
        this.offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
        this.uploadFilesList = findViewById(R.id.upload_filesafe_files);
        this.uploadButton.setOnClickListener(v -> {
            tryFileSafeUpload();
        });
        this.cancelButton.setOnClickListener(v -> {
            finish();
        });
        this.filesafeImage.setOnClickListener(v -> fileSafeChooserDialog());
        loadPropertiesFile();

        connectivityReceiver = new ConnectivityReceiver(this);
        updateOfflineStatusBar();
        checkNetworkStatus();
    }

    private void loadPropertiesFile(){
        PropertyLoader propertyLoader = new PropertyLoader(getApplicationContext());
        Properties properties = propertyLoader.getProperties(Constants.CONFIG_PROPERTIES_FILE);

        Constants.setCognitoIdentityPoolId(properties.getProperty("COGNITO_IDENTITY_POOL_ID"));
        Constants.setCognitoUserPoolId(properties.getProperty("COGNITO_USER_POOL_ID"));
        Constants.setCognitoIdentityPoolId(properties.getProperty("COGNITO_IDENTITY_POOL_ID"));
        Constants.setBucketName(properties.getProperty("BUCKET_NAME"));
        Constants.setKeyPrefix(properties.getProperty("KEY_PREFIX"));
        Constants.setCognitoClientSecret(properties.getProperty("COGNITO_CLIENT_SECRET"));
        Constants.setCognitoClientId(properties.getProperty("COGNITO_CLIENT_ID"));
        Constants.setFilesafePrefix(properties.getProperty("FILESAFE_PREFIX"));
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityResult activityResult = ActivityResult.of(requestCode, resultCode, data);
        if (xmppConnectionService != null) {
            if (activityResult.resultCode == Activity.RESULT_OK && requestCode == ConversationFragment.ATTACHMENT_CHOICE_CHOOSE_FILE) {
                final List<Attachment> fileUris = Attachment.extractFileSafeAttachments(this, activityResult.data, Attachment.Type.FILE);
                setSelectedFiles(fileUris);
            }
        }
    }

    /**
     *
     */
    @SuppressLint("InflateParams")
    protected void fileSafeChooserDialog() {
        uploadFilesList.setText("");
        final Account account = xmppConnectionService.getAccounts().get(0);
        if (account == null || account.getStatus() != Account.State.ONLINE) {
            this.hintOrWarning.setVisibility(View.VISIBLE);
            this.hintOrWarning.setTextColor(getWarningTextColor());
            this.hintOrWarning.setText(R.string.upload_filesafe_error_offline);
            return;
        }

        final PresenceSelector.OnPresenceSelected callback = () -> {

            //Intent intent = new Intent();
            Intent intent = new Intent(Intent.ACTION_PICK);

            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setAction(Intent.ACTION_GET_CONTENT);

            if (intent.resolveActivity(getPackageManager()) != null) {
                Intent chooserIntent = Intent.createChooser(intent, getString(R.string.perform_action_with));
                startActivityForResult(chooserIntent, ConversationFragment.ATTACHMENT_CHOICE_CHOOSE_FILE);
            }
        };

        //if (cognitoAccount)
        callback.onPresenceSelected();
    }

    @Override
    protected void onBackendConnected() {
        updateOfflineStatusBar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        configureActionBar(getSupportActionBar(), true);
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onStop () {
        unregisterReceiver(connectivityReceiver);
        logOut();
        super.onStop();
    }



    protected void toggleUploadButton(boolean enabled, @StringRes int res) {
        final boolean status = enabled && !uploading;
        this.uploadButton.setText(uploading ? R.string.uploading_filesafe_button_message : res);
        this.uploadButton.setEnabled(status);
    }

    public void refreshUiReal() {
    }

    /*private void handleUploadFailed(int res) {
        handleUploadFailed(this.getString(res));
    }

    private void handleUploadFailed(String text) {
        runOnUiThread(() -> {
            hintOrWarning.setText(text);
            hintOrWarning.setTextColor(getWarningTextColor());
            hintOrWarning.setVisibility(View.VISIBLE);
            uploading = false;
            toggleUploadButton(true, R.string.upload);
        });
    }*/

    private void resetUpload() {
        runOnUiThread(() -> {
            uploading = false;
            toggleUploadButton(false, R.string.upload);
            this.cancelButton.setText("CLOSE");
        });
    }

    private void setSelectedFiles(List<Attachment> fileUris) {
        if (fileUris == null || fileUris.size() == 0) {
            return;
        }

        fileSafeUris = fileUris;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.getString(R.string.upload_filesafe_files_selected_message) + '\n');
        for (Attachment attachment : fileSafeUris) {
            String filepath = FileUtils.getPath(xmppConnectionService, attachment.getUri());
            if (filepath == null) {
                filepath = FileUtils.getDisplayName(xmppConnectionService, attachment.getUri());
            }

            if (filepath == null) {
                Uri uri = attachment.getUri();
                stringBuilder.append(uri.getLastPathSegment() + '\n');
            } else {
                try {
                    File f = new File(filepath);
                    if (f != null) {
                        stringBuilder.append(f.getName() + '\n');
                    }
                } catch (Exception e) {
                    Uri uri = attachment.getUri();
                    stringBuilder.append(uri.getLastPathSegment() + '\n');
                }
            }
        }
        runOnUiThread(() -> {
            this.uploadFilesList.setVisibility(View.VISIBLE);
            this.uploadFilesList.setText(stringBuilder.toString());
            toggleUploadButton(true, R.string.upload);
        });
    }

    private void tryFileSafeUpload() {
        uploading = true;
        //this.cancelButton.setEnabled(true);
        this.cancelButton.setText("CANCEL");
        toggleUploadButton(false, R.string.uploading_filesafe_button_message);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // retrieve Cognito credentials
        getCognitoInfo();

        // sign into Cognito
        signInUser();
        // check Cognito status
        // if good, start upload (have popup with progress bar)

    }

    private final String REPLACEMENT_ORG_ID = "<org_id>";
    String username = null;
    String password = null;
    String organization = null;
    List<Attachment> fileSafeUris = null;
    /**
     * Retrieve Cognito account information from file
     */
    private void getCognitoInfo() {
        // get account from database if exists
        if (xmppConnectionService != null) {
            for (Account account : xmppConnectionService.getAccounts()) {

                CognitoAccount cacct = xmppConnectionService.databaseBackend.getCognitoAccount(account,getApplicationContext());
                if (cacct != null) {
                    username = cacct.getUserName();
                    password = cacct.getPassword();
                    return;
                }
            }
        }

        showFailedDialog("Account info cannot be found to initiate login.");
    }

    /**
     * Sign into Cognito
     */
    private void signInUser() {
        AppHelper.init(getApplicationContext());
        AppHelper.setUser(username);
        AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);
    }

    /**
     * Check if S3 bucket exists
     *
     * @return
     */
    private boolean doesBucketExist() {
        try {
            String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
            AmazonS3 sS3Client = Util.getS3Client(this);

            return sS3Client.doesBucketExist(bucketName);
        } catch (Exception e) {
            String temp = e.getMessage();
            e.printStackTrace();
        }

        // bucket doesn't exist if there's a problem
        return false;
    }

    private void uploadFileSafe() {

        String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
        TransferUtility transferUtility = Util.getTransferUtility(this, bucketName);
        int totalForCompletion = fileSafeUris.size() * 100;
        int[] transferIds = new int[fileSafeUris.size()];
        int[] completion = new int[fileSafeUris.size()];
        File[] tempFiles = new File[fileSafeUris.size()];
        boolean[] processed = new boolean[fileSafeUris.size()];

        for (int i=0; i<transferIds.length; i++) {
            transferIds[i] = -1;
            completion[i] = 0;
            processed[i] = false;
        }

        showUploadDialog(this.getString(R.string.upload_filesafe_dialog_message));

        StringBuilder failedsb = new StringBuilder();
        StringBuilder successb = new StringBuilder();
        boolean invalidfile = false;

        int ctr = -1;
        for (Attachment attachment : fileSafeUris) {
            ctr++;
            String filename = null;
            try {
                final String filepath = FileUtils.getPath(xmppConnectionService, attachment.getUri());
                File uploadfile = null;

                if (filepath != null && !FileBackend.isPathBlacklisted(filepath)) {
                    uploadfile = xmppConnectionService.getFileBackend().getFileForPath(filepath);
                    filename = uploadfile.getName();
                } else {
                    try {
                        Uri uri = attachment.getUri();
                        filename = FileUtils.getDisplayName(xmppConnectionService, uri);

                        if (filename == null) {
                            filename = uri.getLastPathSegment();
                        }
                        if (filename.startsWith("enc")) {
                            filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        }

                        String mime = MimeUtils.guessMimeTypeFromUriAndMime(xmppConnectionService, attachment.getUri(), null);
                        String extension = MimeUtils.guessExtensionFromMimeType(mime);
                        if (extension == null) {
                            extension = xmppConnectionService.getFileBackend().getExtensionFromUri(attachment.getUri());
                        }
                        if (extension != null && !(filename.endsWith(extension))) {
                            filename = filename + "." + extension;
                        }

                        uploadfile = new File(getFilesDir().getAbsolutePath(), filename);

                        try(OutputStream outputStream = new FileOutputStream(uploadfile)){
                            IOUtils.copy(getContentResolver().openInputStream(attachment.getUri()), outputStream);
                        } catch (FileNotFoundException e) {
                            Log.e("FileSafe", "FileNotFound Error in local file save");
                        } catch (IOException e) {
                            Log.e("FileSafe", "IO Error in local file save");
                        }

                        tempFiles[ctr] = uploadfile;
                    } catch (Exception ex) {
                        Log.e("FileSafe", "Error in FileSafe without path");
                    }
                }

                if (uploadfile == null) {
                    showFailedDialog("Couldn't find file to upload: " + filepath);
                    continue;
                }

                // FILESAFE_PREFIX / cognito user
                TransferObserver uploadObserver = transferUtility.upload(Constants.getFilesafePrefix() + "/glacierUpload-" + username + "/" + uploadfile.getName(), uploadfile);

                transferIds[ctr] = uploadObserver.getId();
                completion[ctr] = 0;
                //ctr++;

                final String finalfilename = filename;

                // Attach a listener to the observer to get state update and progress notifications
                uploadObserver.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (TransferState.COMPLETED == state) {

                            for (int i=0; i<transferIds.length; i++) {
                                if (id == transferIds[i]) {
                                    processed[i] = true;
                                }
                                if (id == transferIds[i] && tempFiles[i] != null) {
                                    /*String loadedName = Constants.getFilesafePrefix() + "/" + username + "/" + tempFiles[i].getName();
									if (sS3Client.doesObjectExist(bucketName, loadedName)) {
										Log.i("FileSafe", "Object successfully uploaded.");
									} else {
										Log.i("FileSafe", "Can't find object");
									}*/

                                    if (tempFiles[i] != null) {
                                        try {
                                            tempFiles[i].delete();
                                        } catch (Exception e) {
                                            Log.e("FileSafe", "Error Deleting file: " + tempFiles[i].getAbsolutePath());
                                        }
                                    }
                                    tempFiles[i] = null;
                                    break;
                                }
                            }

                            if (successb.length() == 0) {
                                successb.append("Successfully Uploaded:" + '\n');
                            }
                            successb.append(finalfilename + '\n');
                            uploadFilesList.setText(successb.toString());

                            boolean complete = true;
                            for (int p=0; p<processed.length; p++) {
                                if (!processed[p]) {
                                    complete = false;
                                }
                            }
                            if (complete) {
                                resetUpload();
                            }
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                        int percentDone = (int)percentDonef;

                        Log.d("FileSafeActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                                + " bytesTotal: " + bytesTotal + " " + percentDone + "%");

                        int totalDone = 0;
                        for (int i=0; i<completion.length; i++) {
                            if (id == transferIds[i]) {
                                completion[i] = percentDone;
                                processed[i] = true;
                            }
                            totalDone = totalDone + completion[i];
                        }

                        int percentTotal = (totalDone*100) / totalForCompletion;
                        if (uploadDialog != null) {
                            uploadDialog.setProgress(percentTotal);
                        }

                        if (percentTotal == 100) {
                            int toastmsg = R.string.upload_filesafe_success_message;
                            if (transferIds.length > 1) {
                                toastmsg = R.string.upload_filesafe_success_message_plural;
                            }
                            Toast.makeText(FileSafeActivity.this,
                                    toastmsg,
                                    Toast.LENGTH_SHORT).show();
                            resetUpload();
                        }
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        int toastmsg = R.string.upload_filesafe_error_message;
                        Toast.makeText(FileSafeActivity.this, toastmsg, Toast.LENGTH_SHORT).show();

                        Log.e("FileSafeActivity", "Error in upload of id" + id);
                        for (int i=0; i<transferIds.length; i++) {
                            if (id == transferIds[i]) {
                                processed[i] = true;
                            }
                            if (id == transferIds[i] && tempFiles[i] != null) {
                                try {
                                    tempFiles[i].delete();
                                } catch (Exception e) {
                                    Log.e("FileSafe", "Error deleting temp file: " + tempFiles[i].getAbsolutePath());
                                }
                                tempFiles[i] = null;
                                break;
                            }
                        }

                        boolean complete = true;
                        for (int p=0; p<processed.length; p++) {
                            if (!processed[p]) {
                                complete = false;
                            }
                        }
                        if (complete) {
                            resetUpload();
                        }
                    }

                });

                //uploadObserver.getAbsoluteFilePath();

            } catch (AmazonS3Exception ase) {
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","Caught an AmazonS3Exception, " +
                        "which means your request made it " +
                        "to Amazon S3, but was rejected with an error response " +
                        "for some reason.");
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "Error Message:    " + ase.getMessage());
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","HTTP Status Code: " + ase.getStatusCode());
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","AWS Error Code:   " + ase.getErrorCode());
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","Error Type:       " + ase.getErrorType());
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","Request ID:       " + ase.getRequestId());
                if (failedsb.length() > 0) {
                    failedsb.append(", ");
                }
                failedsb.append(filename);
                processed[ctr] = true;
            } catch (AmazonServiceException ase) {
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","Caught an AmazonServiceException, " +
                        "which means your request made it " +
                        "to Amazon S3, but was rejected with an error response " +
                        "for some reason.");
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "Error Message:    " + ase.getMessage());
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","HTTP Status Code: " + ase.getStatusCode());
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","AWS Error Code:   " + ase.getErrorCode());
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","Error Type:       " + ase.getErrorType());
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier","Request ID:       " + ase.getRequestId());
                if (failedsb.length() > 0) {
                    failedsb.append(", ");
                }
                failedsb.append(filename);
                processed[ctr] = true;
            } catch (AmazonClientException ace) {
                com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", "Caught an AmazonClientException, " +
                        "which means the client encountered " +
                        "an internal error while trying to communicate" +
                        " with S3, " +
                        "such as not being able to access the network.");
                if (failedsb.length() > 0) {
                    failedsb.append(", ");
                }
                failedsb.append(filename);
                processed[ctr] = true;
            } catch (Exception e) {
                e.printStackTrace();
                String test = "Something went wrong with the upload";
                if (e.toString().contains("Invalid file")) {
                    invalidfile = true;
                }

                if (failedsb.length() > 0) {
                    failedsb.append(", ");
                }
                failedsb.append(filename);
                processed[ctr] = true;
            }
        }

        if (failedsb.length() > 0) {
            String errmsg = "Problems uploading " + failedsb.toString();
            if (invalidfile) {
                errmsg = "Problems finding data for " + failedsb.toString() + ". The file data has likely been moved or removed.";
            }
            showFailedDialog(errmsg);
        }

        boolean complete = true;
        for (int p=0; p<processed.length; p++) {
            if (!processed[p]) {
                complete = false;
            }
        }
        if (complete) {
            resetUpload();
        }
    }

    /**
     *
     */
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
            com.glaciersecurity.glaciermessenger.utils.Log.d("Glacier", " -- Auth Success");
            AppHelper.setCurrSession(cognitoUserSession);
            AppHelper.newDevice(device);

            // get organization from user attributes
            CognitoUserPool userPool = AppHelper.getPool();
            if (userPool != null) {
                CognitoUser user = userPool.getCurrentUser();
                user.getDetails(new GetDetailsHandler() {
                    @Override
                    public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                        CognitoUserAttributes cognitoUserAttributes = cognitoUserDetails.getAttributes();
                        String org = null;
                        if (cognitoUserAttributes.getAttributes().containsKey("custom:organization")) {
                            org = cognitoUserAttributes.getAttributes().get("custom:organization");
                            organization = org;

                            if (organization != null) {
                                if (doesBucketExist()) {
                                    uploadFileSafe();
                                } else {
                                    showFailedDialog("Could not find storage location");
                                    // log out of cognito
                                    logOut();
                                }
                            } else {
                                showFailedDialog("Could not find storage location");
                                logOut();
                            }
                        } else {
                            showFailedDialog("Could not find storage location");
                            logOut();
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        showFailedDialog("Could not find storage location");
                        logOut();
                    }
                });
            } else {
                showFailedDialog("Could not find storage location");
                logOut();
            }
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
            Locale.setDefault(Locale.US);
            getUserAuthentication(authenticationContinuation, username);
        }

        private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
            if(username != null) {
                username = username;
                AppHelper.setUser(username);
            }
            AuthenticationDetails authenticationDetails = new AuthenticationDetails(username, password, null);
            continuation.setAuthenticationDetails(authenticationDetails);
            continuation.continueTask();
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
        }

        @Override
        public void onFailure(Exception e) {
            showFailedDialog("Authentication failed");
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            /**
             * For Custom authentication challenge, implement your logic to present challenge to the
             * user and pass the user's responses to the continuation.
             */
        }
    };

    // App methods
    // Logout of Cognito and display logout screen
    // This is actually cuplicate of logOut(View) but call
    // comes from function call in program.
    public void logOut() {
        // logout of Cognito
        cognitoCurrentUserSignout();

        // clear s3bucket client
        Util.clearS3Client(this);
    }

    private void cognitoCurrentUserSignout() {
        // logout of Cognito
        // sometimes if it's been too long, I believe pool doesn't
        // exists and user is no longer logged in
        CognitoUserPool userPool = AppHelper.getPool();
        if (userPool != null) {
            CognitoUser user = userPool.getCurrentUser();
            if (user != null) {
                user.signOut();
            }
        }
    }

    private void showFailedDialog(String body) {
        closeUploadDialog();
        uploading = false;
        toggleUploadButton(true, R.string.upload);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(body)
                .setTitle("FileSafe Error")
                .setCancelable(false)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // dismiss this dialog
                        dialog.dismiss();

                        // show wait dialog
                        showUploadDialog(getString(R.string.upload_filesafe_dialog_message));

                        // since we don't know what went wrong, logout, get credentials and log back in
                        logOut();
                        getCognitoInfo();
                        signInUser();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Close progress dialog
     */
    private void closeUploadDialog() {
        if (uploadDialog != null && uploadDialog.isShowing()){
            uploadDialog.dismiss();
        }
    }

    static private ProgressDialog uploadDialog;
    /**
     * Display progress dialog
     *
     * @param message
     */
    public void showUploadDialog(String message) {
        if (uploadDialog != null) {
            uploadDialog.dismiss();
        }
        uploadDialog = new ProgressDialog(this);
        uploadDialog.setMessage(message); // Setting Message
        uploadDialog.setTitle("Glacier FileSafe"); // Setting Title
        uploadDialog.setMax(100);
        uploadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        uploadDialog.show(); // Display Progress Dialog

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (uploadDialog != null && (uploadDialog.getProgress() <= uploadDialog
                            .getMax())) {
                        Thread.sleep(200);

                        if (uploadDialog != null && uploadDialog.isShowing() &&
                                (uploadDialog.getProgress() == uploadDialog.getMax())) {
                            uploadDialog.dismiss();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    ///// OFFLINE STATUS BAR  TODO move to own class, rather than duplicated over code
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        updateOfflineStatusBar();

    }

    private void checkNetworkStatus() {
        updateOfflineStatusBar();
    }

    private View.OnClickListener mRefreshNetworkClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView networkStatus = findViewById(R.id.network_status);
            networkStatus.setCompoundDrawables(null, null, null, null);
            String previousNetworkState = networkStatus.getText().toString();
            final Account account = xmppConnectionService.getAccounts().get(0);
            if (account != null) {
                // previousNetworkState: ie what string is displayed currently in the offline status bar
                if (previousNetworkState != null) {

				    /*
				     Case 1a. PRESENCE -> OFFLINE ) "_____: tap to Reconnect"
				     -> refresh to "Attempting to Connect"
				     -> presence is offline, need to reenable account
				     -> change presence to online
				      */
                    if (previousNetworkState.contains(getResources().getString(R.string.status_tap_to_enable))) {
                        networkStatus.setText(getResources().getString(R.string.refreshing_connection));
                        if (account.getPresenceStatus().equals(Presence.Status.OFFLINE)){
                            enableAccount(account);
                        }
                        PresenceTemplate template = new PresenceTemplate(Presence.Status.ONLINE, account.getPresenceStatusMessage());
                        xmppConnectionService.changeStatus(account, template, null);

                    }
					/*
				     Case 1b. PRESENCE) "_____: tap to set to Available"
				     -> refresh to "Changing status to Available"
				     -> if was offline need to reenable account
				     -> change presence to online
				      */
                    else if (previousNetworkState.contains(getResources().getString(R.string.status_tap_to_available))) {
                        networkStatus.setText(getResources().getString(R.string.refreshing_status));
                        changePresence(account);

                     /*
				     Case 2. ACCOUNT) "Disconnected: tap to connect"
				     -> refresh to "Attempting to Connect"
				     -> toggle account connection(ie what used to be manage accounts toggle)
				      */
                    } else if (previousNetworkState.contains(getResources().getString(R.string.disconnect_tap_to_connect))) {
                        networkStatus.setText(getResources().getString(R.string.refreshing_connection));
                        if (!(account.getStatus().equals(Account.State.CONNECTING) || account.getStatus().equals(Account.State.ONLINE))){
                            enableAccount(account);
                        }
                     /*
				     Case 2. NETWORK) "No internet connection"
				     -> refresh to "Checking for signal"
				     -> ???
				      */
                    } else if (previousNetworkState.contains(getResources().getString(R.string.status_no_network))) {
                        networkStatus.setText(getResources().getString(R.string.refreshing_network));
                        enableAccount(account);
                    }
                } else {
                    // should not reach here... Offline status message state should be defined in one of the above cases
                    networkStatus.setText(getResources().getString(R.string.refreshing_connection));
                }

                updateOfflineStatusBar();
            }

        }
    };

    protected void updateOfflineStatusBar(){
        if (ConnectivityReceiver.isConnected(this)) {
            if (xmppConnectionService != null  && !xmppConnectionService.getAccounts().isEmpty()){
                final Account account = xmppConnectionService.getAccounts().get(0);
                Account.State accountStatus = account.getStatus();
                Presence.Status presenceStatus = account.getPresenceStatus();
                if (presenceStatus.equals(Presence.Status.OFFLINE)){
                    runStatus( getResources().getString(R.string.status_tap_to_enable) ,true, true);
                    Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_enable));
                } else if (!presenceStatus.equals(Presence.Status.ONLINE)){
                    runStatus( presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_available) ,true, true);
                    Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_available));
                } else {
                    if (accountStatus == Account.State.ONLINE ) {
                        runStatus("", false, false);
                    } else if (accountStatus == Account.State.CONNECTING) {
                        runStatus(getResources().getString(R.string.connecting),true, false);
                        Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + getResources().getString(accountStatus.getReadableId()));
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateOfflineStatusBar();
                            }
                        },1000);
                    } else {
                        runStatus(getResources().getString(R.string.disconnect_tap_to_connect),true, true);
                        Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + getResources().getString(accountStatus.getReadableId()));
                    }
                }
            }
        } else {
            runStatus(getResources().getString(R.string.status_no_network), true, true);
            Log.w(Config.LOGTAG ,"updateOfflineStatusBar disconnected from network");

        }
    }

    private void disableAccount(Account account) {
        account.setOption(Account.OPTION_DISABLED, true);
        if (!xmppConnectionService.updateAccount(account)) {
            Toast.makeText(this, R.string.unable_to_update_account, Toast.LENGTH_SHORT).show();
        }
    }

    private void enableAccount(Account account) {
        account.setOption(Account.OPTION_DISABLED, false);
        final XmppConnection connection = account.getXmppConnection();
        if (connection != null) {
            connection.resetEverything();
        }
        if (!xmppConnectionService.updateAccount(account)) {
            Toast.makeText(this, R.string.unable_to_update_account, Toast.LENGTH_SHORT).show();
        }
    }

    private void runStatus(String str, boolean isVisible, boolean withRefresh){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                reconfigureOfflineText(str, withRefresh);
                if(isVisible){
                    offlineLayout.setVisibility(View.VISIBLE);
                } else {
                    offlineLayout.setVisibility(View.GONE);
                }
            }
        }, 1000);
    }
    private void reconfigureOfflineText(String str, boolean withRefresh) {
        if (offlineLayout.isShown()) {
            TextView networkStatus = findViewById(R.id.network_status);
            if (networkStatus != null) {
                networkStatus.setText(str);
                if (withRefresh) {
                    Drawable refreshIcon =
                            ContextCompat.getDrawable(this, R.drawable.ic_refresh_black_24dp);
                    networkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(refreshIcon, null, null, null);
                } else {
                    networkStatus.setCompoundDrawables(null, null, null, null);
                }
            }
        }
    }
    protected void changePresence(Account fragAccount) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        final DialogPresenceBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_presence, null, false);
        String current = fragAccount.getPresenceStatusMessage();
        if (current != null && !current.trim().isEmpty()) {
            binding.statusMessage.append(current);
        }
        setAvailabilityRadioButton(fragAccount.getPresenceStatus(), binding);
        setStatusMessageRadioButton(fragAccount.getPresenceStatusMessage(), binding);
        List<PresenceTemplate> templates = xmppConnectionService.getPresenceTemplates(fragAccount);

        binding.clearPrefs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.statuses.clearCheck();
                binding.statusMessage.setText("");
            }
        });
        binding.statuses.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch(checkedId){
                    case R.id.in_meeting:
                        binding.statusMessage.setText(Presence.StatusMessage.IN_MEETING.toShowString());
                        binding.statusMessage.setEnabled(false);
                        break;
                    case R.id.on_travel:
                        binding.statusMessage.setText(Presence.StatusMessage.ON_TRAVEL.toShowString());
                        binding.statusMessage.setEnabled(false);
                        break;
                    case R.id.out_sick:
                        binding.statusMessage.setText(Presence.StatusMessage.OUT_SICK.toShowString());
                        binding.statusMessage.setEnabled(false);
                        break;
                    case R.id.vacation:
                        binding.statusMessage.setText(Presence.StatusMessage.VACATION.toShowString());
                        binding.statusMessage.setEnabled(false);
                        break;
                    case R.id.custom:
                        binding.statusMessage.setEnabled(true);
                        break;
                    default:
                        binding.statusMessage.setEnabled(false);
                        break;
                }
            }
        });

        builder.setTitle(R.string.edit_status_message_title);
        builder.setView(binding.getRoot());
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
            PresenceTemplate template = new PresenceTemplate(getAvailabilityRadioButton(binding), binding.statusMessage.getText().toString().trim());
            xmppConnectionService.changeStatus(fragAccount, template, null);
            if (template.getStatus().equals(Presence.Status.OFFLINE)){
                disableAccount(fragAccount);
            } else {
                if (!template.getStatus().equals(Presence.Status.OFFLINE) && fragAccount.getStatus().equals(Account.State.DISABLED)){
                    enableAccount(fragAccount);
                }
            }
            updateOfflineStatusBar();

        });
        builder.create().show();
    }


    /*private void generateSignature(Intent intent, PresenceTemplate template, Account fragAccount) {
        xmppConnectionService.getPgpEngine().generateSignature(intent, fragAccount, template.getStatusMessage(), new UiCallback<String>() {
            @Override
            public void success(String signature) {
                xmppConnectionService.changeStatus(fragAccount, template, signature);
            }

            @Override
            public void error(int errorCode, String object) {

            }

            @Override
            public void userInputRequried(PendingIntent pi, String object) {
                mPendingPresenceTemplate.push(template);
                try {
                    startIntentSenderForResult(pi.getIntentSender(), REQUEST_CHANGE_STATUS, null, 0, 0, 0);
                } catch (final IntentSender.SendIntentException ignored) {
                }
            }
        });
    }*/

    private static final int REQUEST_CHANGE_STATUS = 0xee11;
    private final PendingItem<PresenceTemplate> mPendingPresenceTemplate = new PendingItem<>();


    private static void setAvailabilityRadioButton(Presence.Status status, DialogPresenceBinding binding) {
        if (status == null) {
            binding.online.setChecked(true);
            return;
        }
        switch (status) {
            case DND:
                binding.dnd.setChecked(true);
                break;
            case OFFLINE:
                binding.xa.setChecked(true);
                break;
            case XA:
                binding.xa.setChecked(true);
                break;
            case AWAY:
                binding.away.setChecked(true);
                break;
            default:
                binding.online.setChecked(true);
        }
    }

    private static void setStatusMessageRadioButton(String statusMessage, DialogPresenceBinding binding) {
        if (statusMessage == null) {
            binding.statuses.clearCheck();
            binding.statusMessage.setEnabled(false);
            return;
        }
        binding.statuses.clearCheck();
        binding.statusMessage.setEnabled(false);
        if (statusMessage.equals(getEmojiByUnicode(meetingIcon)+"\tIn a meeting")) {
            binding.inMeeting.setChecked(true);
            return;
        } else if (statusMessage.equals(getEmojiByUnicode(travelIcon)+"\tOn travel")) {
            binding.onTravel.setChecked(true);
            return;
        } else if (statusMessage.equals(getEmojiByUnicode(sickIcon)+"\tOut sick")) {
            binding.outSick.setChecked(true);
            return;
        } else if (statusMessage.equals(getEmojiByUnicode(vacationIcon)+"\tVacation")) {
            binding.vacation.setChecked(true);
            return;
        } else if (!statusMessage.isEmpty()) {
            binding.custom.setChecked(true);
            binding.statusMessage.setEnabled(true);
            return;
        } else {
            binding.statuses.clearCheck();
            binding.statusMessage.setEnabled(false);
            return;
        }

    }

    private static Presence.Status getAvailabilityRadioButton(DialogPresenceBinding binding) {
        if (binding.dnd.isChecked()) {
            return Presence.Status.DND;
        } else if (binding.xa.isChecked()) {
            return Presence.Status.OFFLINE;
        } else if (binding.away.isChecked()) {
            return Presence.Status.AWAY;
        } else {
            return Presence.Status.ONLINE;
        }
    }

}
