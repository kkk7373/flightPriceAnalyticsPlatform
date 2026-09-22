package com.flightpriceanalytics.backend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class InitResponseTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void serializesContractFieldNamesAndNullPrices() throws Exception {
        InitResponse response = new InitResponse(
                new InitResponse.Route("KIX", "ICN"),
                new InitResponse.Period(
                        LocalDate.parse("2026-08-23"),
                        LocalDate.parse("2026-09-21"),
                        "Asia/Tokyo"),
                "JPY",
                Instant.parse("2026-09-21T03:00:00Z"),
                new InitResponse.Today(LocalDate.parse("2026-09-21"), 0, null, null, null),
                List.of(new InitResponse.DailyPrice(LocalDate.parse("2026-09-21"), null, 0)),
                List.of(new InitResponse.AirlineRanking(
                        "RS", "エアソウル", new BigDecimal("28000.00"), 15)),
                List.of(new InitResponse.DepartureTimeBand("morning", null, 0)));

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(response));

        assertEquals("KIX", json.path("route").path("origin").asText());
        assertEquals("2026-08-23", json.path("period").path("from").asText());
        assertEquals("2026-09-21T03:00:00Z", json.path("lastUpdatedAt").asText());
        assertEquals("2026-09-21", json.path("today").path("departureDate").asText());
        assertTrue(json.path("today").has("averagePrice"));
        assertTrue(json.path("today").path("averagePrice").isNull());
        assertTrue(json.path("dailyPrices").get(0).path("averagePrice").isNull());
        assertEquals("RS", json.path("airlineRanking").get(0).path("carrierCode").asText());
        assertEquals("morning", json.path("departureTimeBands").get(0).path("timeBand").asText());
    }
}
