package com.glaciersecurity.glaciermessenger.crypto.axolotl;

public class CryptoFailedException extends Exception {

	public CryptoFailedException(String msg) {
		super(msg);
	}

	public CryptoFailedException(Exception e){
		super(e);
	}

	public CryptoFailedException(String msg, Exception e) {
		super(msg, e);
	}
}
