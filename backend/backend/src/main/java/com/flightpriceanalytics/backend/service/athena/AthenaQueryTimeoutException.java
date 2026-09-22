package com.flightpriceanalytics.backend.service.athena;

public class AthenaQueryTimeoutException extends AthenaQueryException {

    public AthenaQueryTimeoutException(String message) {
        super(message);
    }
}
