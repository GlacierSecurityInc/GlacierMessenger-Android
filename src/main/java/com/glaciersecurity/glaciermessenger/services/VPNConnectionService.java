package com.glaciersecurity.glaciermessenger.services;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;

import com.glaciersecurity.glaciercore.api.IOpenVPNAPIService;
import com.glaciersecurity.glaciercore.api.IOpenVPNStatusCallback;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.utils.Log;

/*
 * Not sure whether to have this as a separate class or incorporate it into
 * XmppConnectionService. Probably better to have it separate eventually, but might be easier to start
 * with it in XmppConnectionService to get the functionality working. Christina's NetworkConnectivityStatus
 * code might be a better fit, or at least a place to handle the same things.
 */

public class VPNConnectionService extends Service implements ServiceConnection, Handler.Callback{
    private final int VPN_STATE_UNKNOWN     = 0;
    private final int VPN_STATE_NOPROCESS   = 1;
    private final int VPN_STATE_MISC        = 2;
    private final int VPN_STATE_CONNECTED   = 3;
    private static final int MSG_UPDATE_STATE = 0;
    private int lastConnectionState = VPN_STATE_UNKNOWN;

    protected IOpenVPNAPIService mService=null;
    private Handler mHandler;


    @Override
    public void onCreate() {
        try {
            super.onCreate();
            mHandler = new Handler(this);

            Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
            icsopenvpnService.setPackage("com.glaciersecurity.glaciercore");

            bindService(icsopenvpnService, this, Context.BIND_AUTO_CREATE);

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IOpenVPNAPIService.Stub.asInterface(service);

        try {
            mService.registerStatusCallback(mCallback);
        } catch (RemoteException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        mService = null;
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
        }
    };

    @Override
    public boolean handleMessage(Message msg) {

        if(msg.what == MSG_UPDATE_STATE) {
            if (msg.obj.toString().startsWith("CONNECTED")) {
                lastConnectionState = VPN_STATE_CONNECTED;
            } else if (msg.obj.toString().startsWith("NOPROCESS")) {
                // no vpn running
                lastConnectionState = VPN_STATE_NOPROCESS;
            } else {
                // in the process of coming up
                lastConnectionState = VPN_STATE_MISC;
            }
        }
        return true;
    }

    public boolean hasVpnConnection() {
        if (lastConnectionState == VPN_STATE_CONNECTED) {
            return true;
        }

        return false;
    }

    public int getLastConnectionState() {
        return lastConnectionState;
    }
}
