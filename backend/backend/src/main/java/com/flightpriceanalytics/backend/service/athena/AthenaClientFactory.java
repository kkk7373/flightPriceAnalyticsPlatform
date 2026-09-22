package com.flightpriceanalytics.backend.service.athena;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;

@Configuration
public class AthenaClientFactory {
    @Bean
    public AthenaClient athenaClient(AthenaProperties properties) {
        return AthenaClient.builder()
                .region(Region.of(properties.region()))
                .build();
    }

}
