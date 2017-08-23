package com.teskalabs.cvio.exceptions;

public class MissingAPIKeyException extends CatVisionException {
    public MissingAPIKeyException() {}
    public MissingAPIKeyException(Exception e) {
        super(e);
    }
    public MissingAPIKeyException(String message) {
        super(message);
    }
}
