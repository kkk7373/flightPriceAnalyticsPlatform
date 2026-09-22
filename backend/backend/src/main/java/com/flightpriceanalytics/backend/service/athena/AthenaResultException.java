package com.flightpriceanalytics.backend.service.athena;

public class AthenaResultException extends AthenaQueryException {

    public AthenaResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public AthenaResultException(String message) {
        super(message);
    }
}
