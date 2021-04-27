package com.glaciersecurity.glaciermessenger.xmpp.stanzas.streammgmt;

import com.glaciersecurity.glaciermessenger.xmpp.stanzas.AbstractStanza;

public class RequestPacket extends AbstractStanza {

	public RequestPacket(int smVersion) {
		super("r");
		this.setAttribute("xmlns", "urn:xmpp:sm:" + smVersion);
	}

}
