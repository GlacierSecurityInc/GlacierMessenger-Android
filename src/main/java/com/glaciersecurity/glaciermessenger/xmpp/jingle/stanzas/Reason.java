package com.glaciersecurity.glaciermessenger.xmpp.jingle.stanzas;

import com.glaciersecurity.glaciermessenger.xml.Element;

public class Reason extends Element {
	private Reason(String name) {
		super(name);
	}

	public Reason() {
		super("reason");
	}
}
