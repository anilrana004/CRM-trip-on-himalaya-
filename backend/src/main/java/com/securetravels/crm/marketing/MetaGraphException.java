package com.securetravels.crm.marketing;

/**
 * Normalized Meta Graph failure. Tokens are never included in the message.
 */
public class MetaGraphException extends RuntimeException {

    private final int httpStatus;
    private final String errorCode;
    private final boolean retryable;

    public MetaGraphException(int httpStatus, String errorCode, String message, boolean retryable) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public int httpStatus() { return httpStatus; }
    public String errorCode() { return errorCode; }
    public boolean retryable() { return retryable; }
}
