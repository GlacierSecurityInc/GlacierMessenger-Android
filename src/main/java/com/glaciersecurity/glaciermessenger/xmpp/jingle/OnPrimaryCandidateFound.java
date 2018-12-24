package com.glaciersecurity.glaciermessenger.xmpp.jingle;

public interface OnPrimaryCandidateFound {
	void onPrimaryCandidateFound(boolean success, JingleCandidate canditate);
}
