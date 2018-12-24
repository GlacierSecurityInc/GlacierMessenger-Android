package com.glaciersecurity.glaciermessenger.xmpp;

import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.xmpp.stanzas.MessagePacket;

public interface OnMessagePacketReceived extends PacketReceived {
	public void onMessagePacketReceived(Account account, MessagePacket packet);
}
