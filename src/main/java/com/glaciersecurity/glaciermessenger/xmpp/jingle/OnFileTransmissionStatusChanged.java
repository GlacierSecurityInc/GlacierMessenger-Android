package com.glaciersecurity.glaciermessenger.xmpp.jingle;

import com.glaciersecurity.glaciermessenger.entities.DownloadableFile;

public interface OnFileTransmissionStatusChanged {
	void onFileTransmitted(DownloadableFile file);

	void onFileTransferAborted();
}
