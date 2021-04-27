package com.glaciersecurity.glaciermessenger.xmpp.stanzas.csi;

import com.glaciersecurity.glaciermessenger.xmpp.stanzas.AbstractStanza;

public class InactivePacket extends AbstractStanza {
	public InactivePacket() {
		super("inactive");
		setAttribute("xmlns", "urn:xmpp:csi:0");
	}
}
