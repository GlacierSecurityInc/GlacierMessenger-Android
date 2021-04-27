package com.glaciersecurity.glaciermessenger.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.ui.adapter.AccountAdapter;

public class ChooseAccountForProfilePictureActivity extends XmppActivity {

    protected final List<Account> accountList = new ArrayList<>();
    protected ListView accountListView;
    protected AccountAdapter mAccountAdapter;

    @Override
    protected void refreshUiReal() {
        loadEnabledAccounts();
        mAccountAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_accounts);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.action_glaciervpn);
        configureActionBar(getSupportActionBar(), false);
        accountListView = findViewById(R.id.account_list);
        this.mAccountAdapter = new AccountAdapter(this, accountList, false);
        accountListView.setAdapter(this.mAccountAdapter);
        accountListView.setOnItemClickListener((arg0, view, position, arg3) -> {
            final Account account = accountList.get(position);
            goToProfilePictureActivity(account);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        }
    }

    @Override
    void onBackendConnected() {
        loadEnabledAccounts();
        if (accountList.size() == 1) {
            goToProfilePictureActivity(accountList.get(0));
            return;
        }
        mAccountAdapter.notifyDataSetChanged();
    }

    private void loadEnabledAccounts() {
        accountList.clear();
        for(Account account : xmppConnectionService.getAccounts()) {
            if (account.isEnabled()) {
                accountList.add(account);
            }
        }
    }

    private void goToProfilePictureActivity(Account account) {
        final Intent startIntent = getIntent();
        final Uri uri = startIntent == null ? null : startIntent.getData();
        if (uri != null) {
            Intent intent = new Intent(this, PublishProfilePictureActivity.class);
            intent.putExtra(EXTRA_ACCOUNT, account.getJid().asBareJid().toString());
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
        finish();
    }
}