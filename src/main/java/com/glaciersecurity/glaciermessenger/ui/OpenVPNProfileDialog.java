package com.glaciersecurity.glaciermessenger.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.glaciersecurity.glaciercore.api.APIVpnProfile;
import com.glaciersecurity.glaciermessenger.R;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

// import com.appstoremarketresearch.android_customalertdialogs.R;
// import com.appstoremarketresearch.android_customalertdialogs.notification.AssetFileNameReceiver;

/**
 * Created bon 5/6/2016.
 */
public class OpenVPNProfileDialog extends Dialog implements View.OnClickListener {
    private FileNotFoundException mException;
    List<APIVpnProfile> list = null;

    public Button cancelButton;
    public Button okButton;

    private Context context = null;
    Spinner profileSpinner = null;

    /**
     * @param context
     */
    public OpenVPNProfileDialog(Context context, List<APIVpnProfile> list) {
        super(context);

        this.context = context;
        this.list = list;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.openvpn_profile_dialog);

        View v = this.findViewById(android.R.id.content);

        // TextView titleTextView = (TextView) findViewById(R.id.title);
        cancelButton = (Button) findViewById(R.id.cancel_button);
        okButton = (Button) findViewById(R.id.ok_button);
        cancelButton.setOnClickListener(this);
        okButton.setOnClickListener(this);

        profileSpinner = (Spinner) v.findViewById(R.id.file_spinner);
        List<String> listStr = new ArrayList<String>();
        for (APIVpnProfile profile : list) {
            listStr.add(profile.mName);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_spinner_item, listStr);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();

        // prevent dialog from disappearing before button press finish
        this.setCancelable(false);
        this.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onClick(View view) {
        OpenVPNProfileListener activity = (OpenVPNProfileListener) context;
        switch (view.getId()) {
            case R.id.ok_button:
                dismiss();
                activity.onReturnValue((String) profileSpinner.getSelectedItem());
                break;
            case R.id.cancel_button:
                dismiss();
                activity.onReturnValue(null);
                break;
        }
    }
}
