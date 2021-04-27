package com.glaciersecurity.glaciermessenger.utils;

import android.app.Activity;
import android.content.Intent;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.ConversationsActivity;
import com.glaciersecurity.glaciermessenger.ui.EditAccountActivity;
import com.glaciersecurity.glaciermessenger.ui.ManageAccountActivity;
import com.glaciersecurity.glaciermessenger.ui.StartConversationActivity;
import com.glaciersecurity.glaciermessenger.ui.StepperWizard;
import com.glaciersecurity.glaciermessenger.ui.WelcomeActivity;

public class SignupUtils {

    public static Intent getSignUpIntent(final Activity activity) {
        Intent intent = new Intent(activity, WelcomeActivity.class);
        StartConversationActivity.addInviteUri(intent, activity.getIntent());
        return intent;
    }

    public static Intent getRedirectionIntent(final ConversationsActivity activity) {
        final XmppConnectionService service = activity.xmppConnectionService;
        Account pendingAccount = AccountUtils.getPendingAccount(service);
        Intent intent;
        if (pendingAccount != null) {
            intent = new Intent(activity, EditAccountActivity.class);
            intent.putExtra("jid", pendingAccount.getJid().asBareJid().toString());
        } else {
            if (service.getAccounts().size() == 0) {
                if (Config.X509_VERIFICATION) {
                    intent = new Intent(activity, ManageAccountActivity.class);
                }
                else {
                    intent = new Intent(activity, StepperWizard.class);
                }
            } else {
                intent = new Intent(activity, ConversationsActivity.class);
            }
        }
        intent.putExtra("init", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}