package com.glaciersecurity.glaciermessenger.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

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
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.glaciersecurity.glaciercore.api.APIVpnProfile;
import com.glaciersecurity.glaciercore.api.IOpenVPNAPIService;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;
import com.glaciersecurity.glaciermessenger.cognito.Constants;
import com.glaciersecurity.glaciermessenger.cognito.Util;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.CognitoAccount;
import com.glaciersecurity.glaciermessenger.persistance.DatabaseBackend;
import com.glaciersecurity.glaciermessenger.utils.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Added entire Dialog to add/import VPN into Core
 */
public class ImportVPNProfileDialogFragment extends DialogFragment {
    private FileNotFoundException mException;

    private final String REPLACEMENT_ORG_ID = "<org_id>";
    private final int VPN_STATE_UNKNOWN     = 0;
    private final int VPN_STATE_NOPROCESS   = 1;
    private final int VPN_STATE_MISC        = 2;
    private final int VPN_STATE_CONNECTED   = 3;

    private static final int MSG_UPDATE_STATE = 0;
    private static final int ICS_OPENVPN_PERMISSION = 7;

    List<String> list = new ArrayList<>();

    private AlertDialog alertDialog;

//    public Button cancelButton;
//    public Button okButton;
//    public TextView messageTextView;

    private Context context = null;
    //Spinner profileSpinner = null;

    String username = null;
    String password = null;
    String organization = null;

    List<String> origFileList = new ArrayList<String>();

    TextView textView = null;

    // Cognito variables
    protected IOpenVPNAPIService mService = null;
    private Handler mHandler;

    //private ProgressDialog waitDialog;

    static ImportVPNProfileDialogFragment newInstance(String profileNames) {
        ImportVPNProfileDialogFragment f = new ImportVPNProfileDialogFragment();
        Bundle args = new Bundle();
        args.putString("profile_names", profileNames);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Cognito
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getDialog().setTitle(getString(R.string.open_vpn_profile_dialog_title));
        View v = inflater.inflate(R.layout.import_vpn_profile_dialog, container, false);

        // retrieve Cognito credentials
        getCognitoInfo();

        // sign into Cognito
        signInUser();

        // prevent dialog from disappearing before button press finish
        this.setCancelable(false);

        // CORE processing/integration
        try {
            bindService();
        } catch (Exception e){
            Log.d("Exception", "at bindService");
            e.printStackTrace();
        }

        return v;
    }

    /**
     *
     */
    private void doCoreErrorAction() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this.getContext());
        builder.setTitle(R.string.core_missing);
        builder.setMessage(R.string.glacier_core_install);
        builder.setPositiveButton(R.string.next, (dialog, which) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.glacier_core_https)));
                startActivity(intent);
                dialog.dismiss();
            }
            catch(Exception e2){
                e2.printStackTrace();
            }
        });
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();    }


    /**
     * setUpTitleText
     */
    private void setUpTitleText(int resourceId) {
        // int resourceId = R.string.open_vpn_profile_dialog_title;
        getDialog().setTitle(getString(resourceId));
    }

    /**
     * Retrieve Cognito account information from file
     */
    private void getCognitoInfo() {
        // get account from database if exists
        Account firstacct = null;
        DatabaseBackend databaseBackend = DatabaseBackend.getInstance(getActivity().getApplicationContext());
        for (Account account : databaseBackend.getAccounts()) {
            CognitoAccount cacct = databaseBackend.getCognitoAccount(account,getActivity().getApplicationContext());
            if (cacct != null) {
                username = cacct.getUserName();
                password = cacct.getPassword();
                return;
            }
            if (firstacct == null) {
                firstacct = account;
            }
        }

        //if not found, go to login screen. With jid or without? and how to come back here?
        //or setup open a different login screen?
        if (firstacct != null) {
            Intent intent = new Intent(getActivity().getApplicationContext(), EditAccountActivity.class);
            intent.putExtra("jid", firstacct.getJid().asBareJid().toString());
            startActivity(intent);
        }
    }

    private void downloadAllVPNs(String prof){
        String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
        TransferUtility transferUtility = Util.getTransferUtility(getActivity(), bucketName);

        // retrieve spinner value and add extension back on
        String selectedProfile = (String) prof + ".ovpn";

        // set where file is going on phone
        File destFile = new File(Environment.getExternalStorageDirectory() + "/" + selectedProfile);

        // start the transfer
        TransferObserver observer = transferUtility.download( Constants.KEY_PREFIX + "/" + selectedProfile, destFile, new DownloadListener(selectedProfile));
    }
    /**
     * Sign into Cognito
     */
    private void signInUser() {
        // Cognito - Initialize application
        AppHelper.init(getActivity().getApplicationContext());
        AppHelper.setUser(username);
        AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);
    }

    // App methods
    // Logout of Cognito and display logout screen
    // This is actually cuplicate of logOut(View) but call
    // comes from function call in program.
    public void logOut() {
        // logout of Cognito
        cognitoCurrentUserSignout();

        // clear s3bucket client
        Util.clearS3Client(getActivity());
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

//    @Override
//    public void onClick(View view) {
//
//        Bundle bundle = new Bundle();
//        Intent intent = null;
//
//        switch (view.getId()) {
//            case R.id.ok_button:
//
//                String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
//                TransferUtility transferUtility = Util.getTransferUtility(getActivity(), bucketName);
//
//                // retrieve spinner value and add extension back on
//                String selectedProfile = (String) profileSpinner.getSelectedItem() + ".ovpn";
//
//                // set where file is going on phone
//                File destFile = new File(Environment.getExternalStorageDirectory() + "/" + selectedProfile);
//
//                // start the transfer
//                TransferObserver observer = transferUtility.download( Constants.KEY_PREFIX + "/" + selectedProfile, destFile, new DownloadListener(selectedProfile));
//
//                // send data back to calling fragment
//                /* bundle.putString(OpenVPNFragment.PROFILE_SELECTED, (String) profileSpinner.getSelectedItem());
//                intent = new Intent().putExtras(bundle);
//                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);*/
//
//                break;
//            case R.id.cancel_button:
//                // do nothing
//                logOut();
//                dismiss();
//
//                // send data back to calling fragment
//                bundle.putString(OpenVPNFragment.PROFILE_SELECTED, null);
//                intent = new Intent().putExtras(bundle);
//                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
//
//                break;
//        }
//    }

    /**
     * Check if S3 bucket exists
     *
     * @return
     */
    private boolean doesBucketExist() {
        try {
            String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
            AmazonS3 sS3Client = Util.getS3Client(getActivity());

            return sS3Client.doesBucketExist(bucketName);
        } catch (Exception e) {
            String temp = e.getMessage();
            e.printStackTrace();
        }

        // bucket doesn't exist if there's a problem
        return false;
    }

    private List<String> downloadS3Files() {

        List<String> fileList = new ArrayList<String>();

        String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
        AmazonS3 sS3Client = Util.getS3Client(getActivity());
        TransferUtility transferUtility = Util.getTransferUtility(getActivity(), bucketName);

        try {
            // with correct login, I can get that bucket exists
            if (sS3Client.doesBucketExist(bucketName)) {
                List<S3ObjectSummary> objectListing = sS3Client.listObjects(bucketName, Constants.KEY_PREFIX).getObjectSummaries();
                for (S3ObjectSummary summary : objectListing) {
                    Log.d("Glacier", "Keys found in S3 Bucket (" + summary.getBucketName() + "): " + summary.getKey());

                    if (summary.getKey().contains("_" + username + ".ovpn")) {
                        Log.d("Glacier", "File we want to download: " + summary.getKey());
                        String destFilename = summary.getKey().substring(Constants.KEY_PREFIX.length() + 1, summary.getKey().length());

                        // remove directory and extension
                        String tmpString = stripProfileName(summary.getKey().toString());
                        fileList.add(tmpString);
                    }
                }

                return fileList;
            } else {
                showFailedDialog("Failed to retrieve profile list(1)!");
                // experienced some problem so logout
                //logOut();
            }
        } catch (AmazonS3Exception ase) {
            Log.d("Glacier","Caught an AmazonS3Exception, " +
                    "which means your request made it " +
                    "to Amazon S3, but was rejected with an error response " +
                    "for some reason.");
            Log.d("Glacier", "Error Message:    " + ase.getMessage());
            Log.d("Glacier","HTTP Status Code: " + ase.getStatusCode());
            Log.d("Glacier","AWS Error Code:   " + ase.getErrorCode());
            Log.d("Glacier","Error Type:       " + ase.getErrorType());
            Log.d("Glacier","Request ID:       " + ase.getRequestId());
            showFailedDialog("Failed to retrieve profile list(2)!");
        } catch (AmazonServiceException ase) {
            Log.d("Glacier","Caught an AmazonServiceException, " +
                    "which means your request made it " +
                    "to Amazon S3, but was rejected with an error response " +
                    "for some reason.");
            Log.d("Glacier", "Error Message:    " + ase.getMessage());
            Log.d("Glacier","HTTP Status Code: " + ase.getStatusCode());
            Log.d("Glacier","AWS Error Code:   " + ase.getErrorCode());
            Log.d("Glacier","Error Type:       " + ase.getErrorType());
            Log.d("Glacier","Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            Log.d("Glacier", "Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            Log.d("Glacier","Error Message: " + ace.getMessage());
            showFailedDialog("Failed to retrieve profile list(3)!");
        }
        return null;
    }

    /**
     * strip off directory and extension return filename
     *
     * @param value
     * @return
     */
    private String stripProfileName(String value) {
        String tmpStringArray[] = value.split("/");

        if (tmpStringArray.length > 1) {
            String tmpString = tmpStringArray[tmpStringArray.length -1];
            return tmpString.substring(0, (tmpString.length() - ".ovpn".length()));
        } else {
            return tmpStringArray[0];
        }
    }

    /**
     *
     */
    private void getBucketAndProfiles() {
        if (doesBucketExist()) {
            // try to list objects in directory
            // put together list
            List<String> listStr =  new ArrayList<String>();
            if (doesBucketExist()) {
                listStr = downloadS3Files();
            }

            if (listStr.isEmpty()){
                dismiss();
            }

            Collections.sort(listStr);
            for (String profileStr: listStr){
                downloadAllVPNs(profileStr);
            }
            logOut();
        } else {
            showFailedDialog("Failed to retrieve profile list");
        }
    }

    /**
     *
     */
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
            Log.d("Glacier", " -- Auth Success");
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

                            // username/password is correct.  Now check if bucket exists
                            if (organization != null) {
                                //TODO here
                                getBucketAndProfiles();
                            } else {
                                // log out of cognito
                                logOut();
                                dismiss();
                            }
                        } else {
                            showFailedDialog("Something is missing from account information");
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        showFailedDialog("Something is missing from account information");
                    }
                });
            } else {
                showFailedDialog("Could not access account information");
            }
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
            // closeWaitDialog();
            Locale.setDefault(Locale.US);
            getUserAuthentication(authenticationContinuation, username);
        }

        private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
            //closeWaitDialog();
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
            showFailedDialog("Failed to retrieve profile list(5)!");
            // showDialogMessage(AppHelper.formatException(e));
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            /**
             * For Custom authentication challenge, implement your logic to present challenge to the
             * user and pass the user's responses to the continuation.
             */
        }
    };

    /**
     * Download listener for profile
     */
    private class DownloadListener implements TransferListener {
        String key;

        public DownloadListener(String key) {
            super();

            this.key = key;
        }
        @Override
        public void onError(int id, Exception e) {
            Log.d("Glacier", "Error during download (" + key + "): " + id, e);
            showFailedDialog("Error encountered during download: " + key);
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d("Glacier", String.format("onProgressChanged (" + key + "): %d, total: %d, current: %d", id, bytesCurrent, bytesTotal));
        }

        @Override
        public void onStateChanged(int id, TransferState newState) {
            ImportVPNProfileListener activity = (ImportVPNProfileListener) context;

            Log.d("Glacier", "onStateChanged(" + key + "): " + id + "," + newState);
            if (newState == TransferState.COMPLETED) {
                // logout of


                File tmpFile = new File(Environment.getExternalStorageDirectory() + "/" + key);
                if (tmpFile.exists()) {
                    // track how many have completed download
                    // downloadCount--;
                    Log.d("Glacier", "File confirmed: " + Environment.getExternalStorageDirectory() + "/" + key);
                    moveFile(Environment.getExternalStorageDirectory().toString(), key, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
                    exportFile(key);
                    //showDialogMessage("VPN Download", "Successfully downloaded profile: " + key);
                    dismiss();

                    Bundle bundle = new Bundle();
                    bundle.putString(OpenVPNFragment.PROFILE_SELECTED, (String) key);
                    Intent intent = new Intent().putExtras(bundle);
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);

                } else {
                    Log.d("Glacier", "File unconfirmed: " + Environment.getExternalStorageDirectory() + "/" + key);
                }
            }
        }
    }

    /**
     * move file to different directory
     *
     * @param inputPath
     * @param inputFile
     * @param outputPath
     */
    private void moveFile(String inputPath, String inputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }

            in = new FileInputStream(inputPath + "/" + inputFile);
            out = new FileOutputStream(outputPath + "/" + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            if (new File(inputPath + "/" + inputFile).delete()) {
                Log.d("Glacier", "Successfully deleted profile: " + inputPath + "/" + inputFile);
            } else {
                Log.d("Glacier", "Failed to delete profile: " + inputPath + "/" + inputFile);
            }
        }
        catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
    }

    /**
     * Read file into buffer and then export it to Core
     *
     * @param inputFile
     */
    private void exportFile(String inputFile) {
        try {
            File location = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            location = new File(location.toString() + "/" + inputFile);
            if (location.exists()) {
                Log.d("Glacier", "File does exist!");
            } else {
                Log.d("Glacier", "File does not exist!");
            }
            //InputStream config2 = getActivity().getAssets().open(location.toString());

            FileInputStream config2 = new FileInputStream(location.toString());
            InputStreamReader isr = new InputStreamReader(config2);
            BufferedReader br = new BufferedReader(isr);
            String config="";
            String line;
            while(true) {
                line = br.readLine();
                if(line == null)
                    break;
                config += line + "\n";
            }
            br.readLine();
            br.close();
            isr.close();
            config2.close();

            // strip off extension from end of filename
            String profileName = inputFile.substring(0, inputFile.length()-".ovpn".length());

            // add profile to GlacierCore
            addVPNProfile(profileName, config);

            // delete profile after adding to Core
            if (location.delete()) {
                Log.d("Glacier", "Successfully deleted profile: " + location);
            } else {
                Log.d("Glacier", "Failed to delete profile: " + location);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add profile to Glacier Core
     *
     * @param profile
     * @param config
     * @return
     */
    private boolean addVPNProfile(String profile, String config) {
        try {

            if (mService != null) {
                List<APIVpnProfile> list = mService.getProfiles();

                // check if profile exists and delete it
                for (APIVpnProfile prof : list) {
                    if (prof.mName.compareTo(profile) == 0) {
                        mService.removeProfile(prof.mUUID);
                    }
                }

                // add vpn profile
                mService.addNewVPNProfile(profile, true, config);
                return true;
            }
        } catch (RemoteException e) {
            doCoreErrorAction();
        }
        return false;
    }

    /**
     * Bind to AWS service
     */
    private void bindService() {
        Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage("com.glaciersecurity.glaciercore");

        this.getActivity().bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.

            mService = IOpenVPNAPIService.Stub.asInterface(service);

            try {
                // Request permission to use the API
                Intent i = mService.prepare(getActivity().getPackageName());

                // NOT GOING TO WORK
                if (i!=null) {
                    getActivity().startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                }
            } catch (RemoteException e) {
                doCoreErrorAction();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };

    private void showFailedDialog(String body) {
        //closeWaitDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(body)
                .setTitle("Adding VPN Profile Error")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // dismiss this dialog
                        dismiss();

                        // since we don't know what went wrong, logout, get credentials and log back in
                        logOut();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}