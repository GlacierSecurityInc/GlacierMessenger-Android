package com.glaciersecurity.glaciermessenger.xmpp.stanzas.csi;

import com.glaciersecurity.glaciermessenger.xmpp.stanzas.AbstractStanza;

public class ActivePacket extends AbstractStanza {
	public ActivePacket() {
		super("active");
		setAttribute("xmlns", "urn:xmpp:csi:0");
	}
}
