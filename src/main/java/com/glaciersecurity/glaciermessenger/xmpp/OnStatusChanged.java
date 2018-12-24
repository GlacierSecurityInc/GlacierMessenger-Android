package com.glaciersecurity.glaciermessenger.xmpp;

import com.glaciersecurity.glaciermessenger.entities.Account;

public interface OnStatusChanged {
	public void onStatusChanged(Account account);
}
