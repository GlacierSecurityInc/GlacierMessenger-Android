package com.glaciersecurity.glaciermessenger.entities;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection;

public class NetworkConnectivityStatus {
    private static NetworkConnectivityStatus networkConnetivity;
    private ConnectionState connectionState;

    public enum ConnectionState {
        WIFI_CONNECTED, CELLULAR_CONNECTED, NO_CONNECTIVITY
    }

    private NetworkConnectivityStatus(){
        this.connectionState = ConnectionState.NO_CONNECTIVITY;
    }

    public static NetworkConnectivityStatus getInstance(){
        if (networkConnetivity == null){
            networkConnetivity = new NetworkConnectivityStatus();
        }
        return networkConnetivity;
    }

    public boolean hasInternet(){
        if (connectionState == ConnectionState.WIFI_CONNECTED || connectionState == ConnectionState.CELLULAR_CONNECTED){
            return true;
        }
        return false;
    }

    public ConnectionState getConnectionState(){
        return connectionState;
    }

    public void setConnectionState(ConnectionState connectionState){
        this.connectionState = connectionState;
    }

    public void setConnectionState(NetworkInfo networkInfo){
        if (networkInfo != null){
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                connectionState = ConnectionState.WIFI_CONNECTED;
            } else
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
                connectionState = ConnectionState.CELLULAR_CONNECTED;
            } else {
                connectionState = ConnectionState.NO_CONNECTIVITY;
            }
        } else {
            connectionState = ConnectionState.NO_CONNECTIVITY;
        }

    }

    //TODO use more universal account connection status
    public enum VPN_ConnectionStatus {
        LEVEL_CONNECTED,
        LEVEL_VPNPAUSED,
        LEVEL_CONNECTING_SERVER_REPLIED,
        LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
        LEVEL_NONETWORK,
        LEVEL_NOTCONNECTED,
        LEVEL_START,
        LEVEL_AUTH_FAILED,
        LEVEL_WAITING_FOR_USER_INPUT,
        UNKNOWN_LEVEL
    }

    //TODO modify: idea of combining muliple reasons for a more generic state output
    private static VPN_ConnectionStatus getLevel(String state) {
        String[] noreplyet = {"CONNECTING", "WAIT", "RECONNECTING", "RESOLVE", "TCP_CONNECT"};
        String[] reply = {"AUTH", "GET_CONFIG", "ASSIGN_IP", "ADD_ROUTES"};
        String[] connected = {"CONNECTED"};
        String[] notconnected = {"DISCONNECTED", "EXITING"};

        for (String x : noreplyet)
            if (state.equals(x))
                return VPN_ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;

        for (String x : reply)
            if (state.equals(x))
                return VPN_ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;

        for (String x : connected)
            if (state.equals(x))
                return VPN_ConnectionStatus.LEVEL_CONNECTED;

        for (String x : notconnected)
            if (state.equals(x))
                return VPN_ConnectionStatus.LEVEL_NOTCONNECTED;

        return VPN_ConnectionStatus.UNKNOWN_LEVEL;
    }

}
