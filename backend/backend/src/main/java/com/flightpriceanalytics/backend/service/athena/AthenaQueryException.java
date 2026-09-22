package com.flightpriceanalytics.backend.service.athena;

public class AthenaQueryException extends RuntimeException {

    public AthenaQueryException(String message) {
        super(message);
    }

    public AthenaQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
