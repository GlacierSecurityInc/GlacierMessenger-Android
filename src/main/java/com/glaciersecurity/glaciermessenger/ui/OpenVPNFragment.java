package com.glaciersecurity.glaciermessenger.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.glaciersecurity.glaciercore.api.APIVpnProfile;
import com.glaciersecurity.glaciercore.api.IOpenVPNAPIService;
import com.glaciersecurity.glaciercore.api.IOpenVPNStatusCallback;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.GlacierProfile;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.ui.adapter.ProfileSelectListAdapter;
import com.glaciersecurity.glaciermessenger.utils.Log;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static android.view.View.VISIBLE;

public class OpenVPNFragment extends Fragment implements View.OnClickListener, Handler.Callback{

    final static String EMERGENCY_PROFILE_TAG = "emerg";
    static final int PROFILE_DIALOG_REQUEST_CODE = 8;
    static final String PROFILE_SELECTED = "PROFILE_SELECTED";
    static final String USE_CORE_CONNECT = "use_core_connect";


    private ConnectivityReceiver connectivityReceiver;

    private TextView mMyIp;
    private TextView mStatus;
    private TextView mVpnConnectionStatus;
    private RelativeLayout mVpnStatusBar;
    private FloatingActionButton mImportVpn;
    private TextView mProfile;
    private CheckBox enableEmergConnectCheckBox;
    private GlacierProfile emergencyProfile;
    private Switch mUseVpnToggle;
    private Button mDisconnectVpn;
    private TextView mCoreLink;
    private LinearLayout mDisableVpnView;
    private LinearLayout mNoVpnProfilesView;
    private ListView profileSpinner;
    private ProfileSelectListAdapter<GlacierProfile> spinnerAdapter;

    // variables used for random profile retries upon failure
    private boolean randomProfileSelected = false;
    private List<String> excludeProfileList = new ArrayList<String>();
    private OpenVPNActivity activity;
    private boolean isCoreConnectUsed = false;

    //TODO getConnectedProf

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OpenVPNActivity) {
            this.activity = (OpenVPNActivity) activity;
            boolean is_using_core = this.activity.getPreferences().getBoolean(USE_CORE_CONNECT, false);
            Log.d(Config.LOGTAG, is_using_core + " ");

        } else {
            throw new IllegalStateException("Trying to attach fragment to activity that is not the ConversationsActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.openvpn_fragment, container, false);

        v.findViewById(R.id.fab_import).setOnClickListener(this);
        mImportVpn = (FloatingActionButton) v.findViewById(R.id.fab_import);
        mStatus = (TextView) v.findViewById(R.id.status);
        mProfile = (TextView) v.findViewById(R.id.currentProfile);
        mVpnConnectionStatus = (TextView) v.findViewById(R.id.vpn_connection_status);
        mVpnStatusBar = (RelativeLayout) v.findViewById(R.id.vpn_status);
        mDisableVpnView = (LinearLayout) v.findViewById(R.id.disabled_vpn_layout);
        mCoreLink = (TextView) v.findViewById(R.id.glacier_chat_core_link);
        mNoVpnProfilesView = (LinearLayout) v.findViewById(R.id.no_vpn_profiles_layout);
        mDisconnectVpn= (Button) v.findViewById(R.id.disconnet_button);
        v.findViewById(R.id.disconnet_button).setOnClickListener(mOnDisconnectListener);
        v.findViewById(R.id.glacier_chat_core_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.core_link)));
                startActivity(intent);
            }
        });

        mProfile = (TextView) v.findViewById(R.id.currentProfile);
        mUseVpnToggle = (Switch) v.findViewById(R.id.use_vpn_status_toggle);
        v.findViewById(R.id.use_vpn_status_toggle).setOnClickListener(mOnToggleSwitchListener);
        addItemsOnProfileSpinner(v);

        isCoreConnectUsed = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(USE_CORE_CONNECT, false);
            if(isCoreConnectUsed){
                profileSpinner.setVisibility(View.VISIBLE);
                mVpnStatusBar.setVisibility(View.VISIBLE);
                mDisableVpnView.setVisibility(View.GONE);
                mImportVpn.show();
                mVpnStatusBar.setVisibility(VISIBLE);
                mUseVpnToggle.setChecked(true);
           } else {
                mUseVpnToggle.setChecked(false);
                mDisableVpnView.setVisibility(View.VISIBLE);
                mNoVpnProfilesView.setVisibility(View.GONE);
                profileSpinner.setVisibility(View.GONE);
                mVpnStatusBar.setVisibility(View.GONE);
                mImportVpn.hide();
            }

        return v;

    }



    private void setUseCoreConnect (boolean bool) {
        activity.getPreferences().edit().putBoolean(USE_CORE_CONNECT, bool).apply();
    }

    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_MYIP = 1;
    private static final int START_PROFILE_EMBEDDED = 2;
    private static final int START_PROFILE_BYUUID = 3;
    private static final int ICS_OPENVPN_PERMISSION = 7;
    private static final int PROFILE_ADD_NEW = 8;

    private View.OnClickListener mOnToggleSwitchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(mUseVpnToggle.isChecked()){
                setUseCoreConnect(true);
                mVpnStatusBar.setVisibility(View.VISIBLE);
                mImportVpn.show();
                mVpnStatusBar.setVisibility(VISIBLE);
                mDisableVpnView.setVisibility(View.GONE);
                mVpnStatusBar.setVisibility(VISIBLE);
                listVPNs();

            } else {
                setUseCoreConnect(false);
                disconnectVpn();
                mDisableVpnView.setVisibility(View.VISIBLE);
                profileSpinner.setVisibility(View.GONE);
                mNoVpnProfilesView.setVisibility(View.GONE);
                mVpnStatusBar.setVisibility(View.GONE);
                mDisableVpnView.setVisibility(VISIBLE);
                mImportVpn.hide();

            }
        }
    };

    private View.OnClickListener mOnDisconnectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mDisconnectVpn.setVisibility(View.GONE);
            disconnectVpn();

        }
    };


    private void disconnectVpn(){
        try {
            mService.disconnect();
            spinnerAdapter.clearSelections(profileSpinner);
            SharedPreferences sp = this.getActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
            sp.edit().putString("last_spinner_profile", "").commit();
        } catch (RemoteException e) {
            Log.d(Config.LOGTAG, "at mService.disconnect");
            e.printStackTrace();
        }
    }


    protected IOpenVPNAPIService mService=null;
    private Handler mHandler;

    private void startEmbeddedProfile(boolean addNew)
    {
        try {
            InputStream conf = getActivity().getAssets().open("dave-vpn.ovpn");
            InputStreamReader isr = new InputStreamReader(conf);
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

            if (addNew)
                mService.addNewVPNProfile("newDaveProfile", true, config);
            else
                mService.startVPN(config);
        } catch (IOException | RemoteException e) {
            Log.d("RemoteException", "at mService.startVpn");
            e.printStackTrace();
        }
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
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        });
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }



    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler(this);
        bindService();
    }


    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */

        @Override
        public void newStatus(String uuid, String state, String message, String level)
                throws RemoteException {
            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
            msg.sendToTarget();

            // Retrieve name of uuid and set the profile text
            String profileName = getProfileName(uuid);
            if (profileName != null) {
                mProfile.setText(profileName);
            }
        }
    };

    /**
     * Retrieve profile name based on uuid.  We first check
     * the spinner and then check the emergency node
     * @param uuid
     * @return
     */
    private String getProfileName(String uuid) {
        int index = getSpinnerIndex(uuid);

        if (index >= 0) {
            GlacierProfile gp = (GlacierProfile) profileSpinner.getItemAtPosition(index);
            return gp.getName();
 //           return gp.getName();
        } else if (uuid.compareTo(emergencyProfile.getUuid()) == 0) {
            return emergencyProfile.getName();
        }
        return null;
    }

    /**
     * Retrieve index in spinner for matching uuid.
     * Return -1 if nothing found
     */
    private int getSpinnerIndex(String uuid) {
        GlacierProfile tmpProfile = null;
        String tmpUuid = null;
        int i = 0;
        for (i = 0; i < profileSpinner.getAdapter().getCount(); i++) {
            tmpProfile = (GlacierProfile) profileSpinner.getItemAtPosition(i);
            tmpUuid = tmpProfile.getUuid();

            if (tmpUuid != null){
                // compare lower cases
                if (uuid.toLowerCase().compareTo(tmpUuid.toLowerCase()) == 0) {

                    break;
                }
            }
        }

        // cold not find uuid being used
        if (i == profileSpinner.getAdapter().getCount()) {
            return -1;
        }
        return i;
    }


    /**
     * Add items to spinner
     *
     * @param v
     */
    private void addItemsOnProfileSpinner(View v) {
        profileSpinner = (ListView) v.findViewById(R.id.profileSpinner);
        List<GlacierProfile> gp = new ArrayList<GlacierProfile>();

        spinnerAdapter = new ProfileSelectListAdapter<GlacierProfile>(this.getActivity(),R.layout.radio, gp);
        profileSpinner.setAdapter(spinnerAdapter);
        profileSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,final int position, long id) {
                //TODO CHECK
                spinnerAdapter.select(position, view, parent);
                GlacierProfile glacierProfile = (GlacierProfile) parent.getItemAtPosition(position);
                confirmProfileSelection(position, glacierProfile);
            }

        });
        spinnerAdapter.notifyDataSetChanged();
    }


    /**
     * Class for interacting with the main interface of the service.
     */
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
                if (i!=null) {
                    startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK,null);
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
    private String mStartUUID=null;

    private void bindService() {

        Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage("com.glaciersecurity.glaciercore");

        getActivity().bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void listVPNs() {

        List<GlacierProfile> nameList = new ArrayList<GlacierProfile>();

        try {
            List<APIVpnProfile> list = mService.getProfiles();
            String all="Profile List:\n";
            for(APIVpnProfile vp:list.subList(0, Math.min(5, list.size()))) {
                all = all + vp.mName + ":" + vp.mUUID + "\n";
            }

            if (list.size() > 5)
                all +="\n And some profiles....";

            // add rest of vpn profiles to list
            for (int j = 0; j < list.size();j++) {
                // do not add emergency profile yet
                if (!isEmergencyProfile(list.get(j).mName.toLowerCase())) {
                    nameList.add(new GlacierProfile(parseVpnName(list.get(j).mName), list.get(j).mUUID));
                } else {
                    emergencyProfile = new GlacierProfile(parseVpnName(list.get(j).mName), list.get(j).mUUID);
                }
            }
            Collections.sort(nameList, new Comparator<GlacierProfile>() {
                @Override
                public int compare(GlacierProfile glacierProfile, GlacierProfile t1) {
                    return glacierProfile.getName().compareTo(t1.getName());
                }
            });

            spinnerAdapter = new ProfileSelectListAdapter<GlacierProfile>(this.getActivity(),R.layout.radio, nameList);
            //spinnerAdapter.setDropDownViewResource(R.layout.radio);
            profileSpinner.setAdapter(spinnerAdapter);
            spinnerAdapter.notifyDataSetChanged();
            spinnerAdapter.setOnItemClickListener(new ProfileSelectListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, GlacierProfile glacierProfile, int position) {
                    spinnerAdapter.select(position,view,profileSpinner);
                    confirmProfileSelection(position, glacierProfile);
                }

            });
            spinnerAdapter.notifyDataSetChanged();

            if(list.size()> 0) {
                mNoVpnProfilesView.setVisibility(View.GONE);
                if (mUseVpnToggle.isChecked()){
                    profileSpinner.setVisibility(View.VISIBLE);
                    mVpnStatusBar.setVisibility(View.VISIBLE);
                } else {
                    profileSpinner.setVisibility(View.GONE);
                    mVpnStatusBar.setVisibility(View.GONE);

                }


            } else {
                profileSpinner.setVisibility(View.GONE);
                mVpnStatusBar.setVisibility(View.GONE);
                if (mUseVpnToggle.isChecked()){
                    mNoVpnProfilesView.setVisibility(View.VISIBLE);
                } else {
                    mNoVpnProfilesView.setVisibility(View.GONE);
                }
            }

        } catch (RemoteException e) {
            Log.d("RemoteException", "at listVpns");
            e.printStackTrace();
        }
    }

    private void confirmProfileSelection(int position, GlacierProfile glacierProfile){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.core_profile));
        builder.setMessage(getText(R.string.change_vpn_profile)+" "+ glacierProfile.getName() +"?");
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setPositiveButton(getString(R.string.connect_button_label),
                (dialog, which) -> {
                    selectProfile(position, glacierProfile);
                });
        builder.create().show();
    }
    private void selectProfile(int position, GlacierProfile glacierProfile){
        //clearSelectedVpn();
        //selectedVpn(position);
        mDisconnectVpn.setVisibility(View.VISIBLE);
        startVpn(glacierProfile);
    }
    private void unbindService() {
        getActivity().unbindService(mConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

    private boolean isEmergencyProfile(String name) {
        if (name != null) {
            if (name.toLowerCase().contains(EMERGENCY_PROFILE_TAG))
                return true;
        }
        return false;
    }

    private void startVpn(GlacierProfile glacierProfile){
        mStartUUID = glacierProfile.getUuid();

        // retrieve previous profile selected
        SharedPreferences sp = this.getActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
        sp.edit().putString("last_spinner_profile", mStartUUID).commit();

        // see if random profile selected
        if (mStartUUID.compareTo("random") == 0) {
            excludeProfileList.clear();
            randomProfileSelected = true;
            mStartUUID = getRandomUuid();
        }

        if (isEmergencyProfile(glacierProfile.getName())) {
            AlertDialog.Builder d = new AlertDialog.Builder(this.getActivity());

            d.setIconAttribute(android.R.attr.alertDialogIcon);
            d.setTitle("Emergency Profile");
            d.setMessage("This is an emergency profile.  Are you sure you want to continue?");
            d.setNegativeButton(getString(R.string.cancel), null);
            d.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        prepareStartProfile(START_PROFILE_BYUUID);
                    } catch (RemoteException e) {
                        Log.d("RemoteException", "at prepareStartProfile");
                        e.printStackTrace();
                    }
                }
            });
            d.show();
        } else {
            try {
                prepareStartProfile(START_PROFILE_BYUUID);
            } catch (RemoteException e) {
                Log.d("RemoteException", "at prepareStartProfile");
                e.printStackTrace();
            }
        }
    }

    public String parseVpnName(String name){
        try {
            String result = "";
            String [] splitname = name.split("_");
            return splitname[1];
        } catch (Exception e){
        }
        return name;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_import:
                try {
                    showImportProfileVPNDialogFragment();
                } catch (Exception e){
                    Log.d("Exception", "at showImportProfileVPNDialogFragment");
                    e.printStackTrace();
                }
            default:
                break;
        }

    }

    /**
     *
     */
    public void launchPlayStoreCore(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getString(R.string.glacier_core_https)));
        startActivity(intent);
    }

    /**
     * Retreive random profile
     *
     * @return
     */
    private String getRandomUuid() {
        Random randomGenerator = new Random();

        // retrieve number of profiles
        int count = spinnerAdapter.getCount();
        int randomInt = -1;
        GlacierProfile glacierProfile = null;

        while (true) {
            if (enableEmergConnectCheckBox.isChecked() == true) {
                // do not include random profile in the beginning and the
                // emergency profile at the end.
                randomInt = randomGenerator.nextInt(count - 2);

                if (excludeProfileList.size() == (count - 2)) {
                    return null;
                }
            } else {
                // do not include the random profile in the beginning
                randomInt = randomGenerator.nextInt(count - 1);
                if (excludeProfileList.size() == (count-1)) {
                    return null;
                }
            }

            // get random profile, don't forget to skip the first one ("random")
            glacierProfile = (GlacierProfile) spinnerAdapter.getItem(randomInt + 1);

            // check if we're excluding
            if (!excludeProfileList.contains(glacierProfile.getUuid())) {
                excludeProfileList.add(glacierProfile.getUuid());
                return glacierProfile.getUuid();
            }
        }
    }

    private void prepareStartProfile(int requestCode) throws RemoteException {
        Intent requestpermission = mService.prepareVPNService();
        if(requestpermission == null) {
            onActivityResult(requestCode, Activity.RESULT_OK, null);
        } else {
            // Have to call an external Activity since services cannot used onActivityResult
            startActivityForResult(requestpermission, requestCode);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode==START_PROFILE_EMBEDDED)
                startEmbeddedProfile(false);
            if(requestCode==START_PROFILE_BYUUID)
                try {
                    if (mStartUUID!= null ||mStartUUID.isEmpty()) {
                        mService.startProfile(mStartUUID);
                    }
                } catch (RemoteException e) {
                    Log.d("RemoteException", "at start profile byuuid");
                    e.printStackTrace();
                }
            if (requestCode == ICS_OPENVPN_PERMISSION) {




                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException | SecurityException e) {
                    Log.d(Config.LOGTAG, "RemoteException or Security Exception register status callback");
                    e.printStackTrace();
                }

                listVPNs();
            }
            if (requestCode == PROFILE_ADD_NEW) {
                listVPNs();
                startEmbeddedProfile(true);
            }
        }
    };

    String getMyOwnIP() throws UnknownHostException, IOException, RemoteException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        String resp="";
        Socket client = new Socket();
        // Setting Keep Alive forces creation of the underlying socket, otherwise getFD returns -1
        client.setKeepAlive(true);


        client.connect(new InetSocketAddress("v4address.com", 23),20000);
        client.shutdownOutput();
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        while (true) {
            String line = in.readLine();
            if( line == null)
                return resp;
            resp+=line;
        }

    }


    public void handleStatusMessage(String status){
        if (status.startsWith("NOPROCESS")) {
            mVpnConnectionStatus.setText("Not Connected");
            mDisconnectVpn.setVisibility(View.GONE);
            return;
        } else if (status.startsWith("CONNECTED")) {
            mVpnConnectionStatus.setText("Connected");
            mDisconnectVpn.setVisibility(VISIBLE);
            return;
        } else if ((status.startsWith("NONETWORK")) || (status.startsWith("AUTH_FAILED")) || (status.startsWith("EXITING"))) {
            mVpnConnectionStatus.setText("Not Connected");
            mDisconnectVpn.setVisibility(View.GONE);
            return;
        }
        else {
            mVpnConnectionStatus.setText("Connecting");
            mDisconnectVpn.setVisibility(VISIBLE);
            return;
        }
    }
    @Override
    public boolean handleMessage(Message msg) {
        Log.d(Config.LOGTAG, "OpenVPNFragment::handleMessage(): " + msg.obj.toString() + "::What = " + msg.what);
        //TODO use message to update connection status
        if(msg.what == MSG_UPDATE_STATE) {
            // check for NOPROCESS string and change it to NOT CONNECTED
            if (msg.obj.toString().startsWith("NOPROCESS")) {
                mStatus.setText("NOT CONNECTED");
                mVpnConnectionStatus.setText("Not Connected");
                mDisconnectVpn.setVisibility(View.GONE);
                // check if this is a start of trying random profiles and it failed

                if ((randomProfileSelected)) {
                    mStartUUID = getRandomUuid();
                    if (mStartUUID != null) {
                        try {
                            prepareStartProfile(START_PROFILE_BYUUID);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // reset flags b/c done trying all profiles
                        randomProfileSelected = false;
                        excludeProfileList.clear();
                    }
                }

            } else {
                // found profile that works, so reset variables
                if (msg.obj.toString().startsWith("CONNECTED")) {
                    mStatus.setText("CONNECTED");
                    mVpnConnectionStatus.setText("Connected");
                    mDisconnectVpn.setVisibility(VISIBLE);
                } else if ((msg.obj.toString().startsWith("NONETWORK")) || (msg.obj.toString().startsWith("AUTH_FAILED")) || (msg.obj.toString().startsWith("EXITING"))) {
                    randomProfileSelected = false;
                    excludeProfileList.clear();
                    mStatus.setText(((CharSequence) msg.obj).subSequence(0, ((CharSequence) msg.obj).length() - 1));
                    mVpnConnectionStatus.setText("Connection failed");
                    mDisconnectVpn.setVisibility(View.GONE);

                } else {
                    mStatus.setText(((CharSequence) msg.obj).subSequence(0, ((CharSequence) msg.obj).length() - 1));
                    mVpnConnectionStatus.setText("Configuring Connection");
                    mDisconnectVpn.setVisibility(VISIBLE);

                }
            }
        } else if (msg.what == MSG_UPDATE_MYIP) {

            mMyIp.setText((CharSequence) msg.obj);
        }
        return true;
    }

    /**
     * Import VPN from AWS
     */
    private void showImportProfileVPNDialogFragment() {
        //TODO
        DialogFragment dialogFragment = ImportVPNProfileDialogFragment.newInstance(getString(R.string.load_vpn_profile_dialog_message));
        dialogFragment.setTargetFragment(this, PROFILE_DIALOG_REQUEST_CODE);
        dialogFragment.show(getFragmentManager(), "dialog");

    }
}