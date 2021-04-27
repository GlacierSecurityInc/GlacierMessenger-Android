package com.glaciersecurity.glaciermessenger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;

public class EventReceiver extends BroadcastReceiver {

	public static final String SETTING_ENABLED_ACCOUNTS = "enabled_accounts";
	public static final String EXTRA_NEEDS_FOREGROUND_SERVICE = "needs_foreground_service";

	@Override
	public void onReceive(final Context context, final Intent originalIntent) {
		final Intent intentForService = new Intent(context, XmppConnectionService.class);
		if (originalIntent.getAction() != null) {
			intentForService.setAction(originalIntent.getAction());
			final Bundle extras = originalIntent.getExtras();
			if (extras != null) {
				intentForService.putExtras(extras);
			}
		} else {
			intentForService.setAction("other");
		}
		final String action = originalIntent.getAction();
		if (action.equals("ui") || hasEnabledAccounts(context)) {
			Compatibility.startService(context, intentForService);
		} else {
			Log.d(Config.LOGTAG, "EventReceiver ignored action " + intentForService.getAction());
		}
	}

	public static boolean hasEnabledAccounts(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SETTING_ENABLED_ACCOUNTS,true);
	}

}
