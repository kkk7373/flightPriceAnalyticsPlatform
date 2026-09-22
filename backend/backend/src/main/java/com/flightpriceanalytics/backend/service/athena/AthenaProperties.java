package com.flightpriceanalytics.backend.service.athena;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.athena")
public record AthenaProperties(
    String region,
    String database,
    String workgroup,
    String outputLocation,
    Duration queryTimeout,
    int sleep
) {}
