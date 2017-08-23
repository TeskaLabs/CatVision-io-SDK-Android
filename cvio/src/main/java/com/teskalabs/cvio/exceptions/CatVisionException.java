package com.teskalabs.cvio.exceptions;

public class CatVisionException extends Exception {
    public CatVisionException() {}
    public CatVisionException(Exception e) {
        super(e);
    }
    public CatVisionException(String message) {
        super(message);
    }
}
