package com.glaciersecurity.glaciermessenger.ui;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.utils.ThemeHelper;

import static com.glaciersecurity.glaciermessenger.ui.XmppActivity.configureActionBar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(ThemeHelper.find(this));

        setContentView(R.layout.activity_about);
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());

    }
}
