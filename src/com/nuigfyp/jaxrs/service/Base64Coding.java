package com.nuigfyp.jaxrs.service;

import javax.xml.bind.DatatypeConverter;

public class Base64Coding {

	public String encode(String stringToBeEncoded) {

		String encoded = DatatypeConverter.printBase64Binary(stringToBeEncoded.getBytes()); 
		return encoded;
	}

	public String decode(String stringToBeDecoded) {

		String decoded = new String(DatatypeConverter.parseBase64Binary(stringToBeDecoded));
		return decoded;
	}

}