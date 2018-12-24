package com.glaciersecurity.glaciermessenger.xmpp.stanzas.streammgmt;

import com.glaciersecurity.glaciermessenger.xmpp.stanzas.AbstractStanza;

public class AckPacket extends AbstractStanza {

	public AckPacket(int sequence, int smVersion) {
		super("a");
		this.setAttribute("xmlns", "urn:xmpp:sm:" + smVersion);
		this.setAttribute("h", Integer.toString(sequence));
	}

}
