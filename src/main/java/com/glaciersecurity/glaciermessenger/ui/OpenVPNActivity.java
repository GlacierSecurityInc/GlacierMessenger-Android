package com.glaciersecurity.glaciermessenger.ui;


import android.os.Bundle;
import android.view.Menu;

import com.glaciersecurity.glaciermessenger.R;

public class OpenVPNActivity extends XmppActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.openvpn_activity);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new OpenVPNFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.vpn_connection, menu);
        return true;
    }

    @Override
    void onBackendConnected() {
        // nothing to do
    }

    public void refreshUiReal() {
        //nothing to do. This Activity doesn't implement any listeners
    }

}
