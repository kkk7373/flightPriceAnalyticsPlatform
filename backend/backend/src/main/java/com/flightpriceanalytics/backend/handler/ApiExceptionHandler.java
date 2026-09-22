package com.flightpriceanalytics.backend.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.flightpriceanalytics.backend.service.athena.AthenaQueryException;
import com.flightpriceanalytics.backend.service.athena.AthenaQueryTimeoutException;

import software.amazon.awssdk.core.exception.SdkException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AthenaQueryTimeoutException.class)
    public ProblemDetail handleTimeout(AthenaQueryTimeoutException exception) {
        logger.warn("Athena query timed out", exception);
        return problem(HttpStatus.GATEWAY_TIMEOUT, "ATHENA_TIMEOUT", "データ取得がタイムアウトしました");
    }

    @ExceptionHandler({AthenaQueryException.class, SdkException.class})
    public ProblemDetail handleAthenaFailure(Exception exception) {
        logger.error("Athena query failed", exception);
        return problem(HttpStatus.BAD_GATEWAY, "ATHENA_QUERY_FAILED", "集計データを取得できませんでした");
    }

    @ExceptionHandler(IOException.class)
    public ProblemDetail handleSqlLoadFailure(IOException exception) {
        logger.error("SQL resource could not be loaded", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "SQL_RESOURCE_ERROR", "集計処理を開始できませんでした");
    }

    @ExceptionHandler(InterruptedException.class)
    public ProblemDetail handleInterrupted(InterruptedException exception) {
        logger.warn("Dashboard request was interrupted", exception);
        Thread.currentThread().interrupt();
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "REQUEST_INTERRUPTED", "データ取得を完了できませんでした");
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleUnexpected(RuntimeException exception) {
        logger.error("Unexpected dashboard error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "処理中にエラーが発生しました");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("code", code);
        return detail;
    }
}
