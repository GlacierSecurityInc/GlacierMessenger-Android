package com.glaciersecurity.glaciermessenger.ui;


import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;

public class OpenVPNActivity extends XmppActivity {

    private ConnectivityReceiver connectivityReceiver;
    private OpenVPNFragment openVPNFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.openvpn_activity);
        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        configureActionBar(getSupportActionBar());
        if (savedInstanceState == null) {
            openVPNFragment = new OpenVPNFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.container, openVPNFragment).commit();

        } else {
            openVPNFragment = (OpenVPNFragment) getSupportFragmentManager()
                    .findFragmentByTag("openVpnFragment");
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, "openVpnFragment", openVPNFragment);
    }
@Override
protected void onStart() {
    super.onStart();
}

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    void onBackendConnected() {
        // nothing to do
    }

    public void refreshUiReal() {
    }

}
