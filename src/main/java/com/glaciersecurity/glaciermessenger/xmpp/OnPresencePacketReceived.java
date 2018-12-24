package com.glaciersecurity.glaciermessenger.xmpp;

import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.xmpp.stanzas.PresencePacket;

public interface OnPresencePacketReceived extends PacketReceived {
	public void onPresencePacketReceived(Account account, PresencePacket packet);
}
