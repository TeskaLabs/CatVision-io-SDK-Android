package com.teskalabs.cvio.exceptions;

public class CatVisionNotSupportedException extends CatVisionException {
	public CatVisionNotSupportedException() {}

	public CatVisionNotSupportedException(Exception e) {
		super(e);
	}

	public CatVisionNotSupportedException(String message) {
		super(message);
	}
}
