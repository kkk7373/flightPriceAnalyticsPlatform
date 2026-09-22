package com.flightpriceanalytics.backend.service.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.flightpriceanalytics.backend.dto.InitResponse;
import com.flightpriceanalytics.backend.service.athena.AthenaQueryService;
import com.flightpriceanalytics.backend.util.SqlResourceLoader;

class InitDashboardServiceTest {

    @Test
    void combinesFourQueriesAndFillsMissingDays() throws Exception {
        LocalDate to = LocalDate.of(2026, 9, 22);
        LocalDate from = to.minusDays(29);
        AthenaQueryService queryService = new AthenaQueryService(null, null) {
            @Override
            public <T> List<T> athenaExecute(
                    String sql, List<String> parameters, Function<Map<String, String>, T> mapper) {
                List<Map<String, String>> rows;

                if (sql.contains("AS min_price")) {
                    assertEquals(List.of(dateParameter(to)), parameters);
                    rows = List.of(row("offer_count", "2", "average_price", "150.00",
                            "min_price", "100.00", "max_price", "200.00"));
                } else {
                    assertEquals(List.of(dateParameter(from), dateParameter(to)), parameters);
                    if (sql.contains("AS avg_price_by_date")) {
                        rows = List.of(row("departure_date", to.toString(),
                                "avg_price_by_date", "150.00", "offer_count_by_date", "2"));
                    } else if (sql.contains("AS average_price_by_airline")) {
                        rows = List.of(row("carrier_code", "RS", "carrier_name", "Air Seoul",
                                "average_price_by_airline", "150.00", "offer_count_by_airline", "2"));
                    } else if (sql.contains("band_summary")) {
                        rows = List.of(row("time_band", "earlyMorning", "average_price", null,
                                "offer_count", "0"));
                    } else {
                        throw new AssertionError("Unexpected SQL: " + sql);
                    }
                }
                return rows.stream().map(mapper).toList();
            }
        };

        Instant before = Instant.now();
        InitResponse response = new InitDashboardService(queryService, new SqlResourceLoader())
                .initDashboard(to);
        Instant after = Instant.now();

        assertEquals("KIX", response.route().origin());
        assertEquals("ICN", response.route().destination());
        assertEquals(from, response.period().from());
        assertEquals(to, response.period().to());
        assertEquals("Asia/Tokyo", response.period().timezone());
        assertEquals("JPY", response.currency());
        assertFalse(response.lastUpdatedAt().isBefore(before));
        assertFalse(response.lastUpdatedAt().isAfter(after));
        assertEquals(to, response.today().departureDate());
        assertEquals(2, response.today().offerCount());
        assertEquals(new BigDecimal("150.00"), response.today().averagePrice());
        assertEquals(new BigDecimal("100.00"), response.today().lowestPrice());
        assertEquals(new BigDecimal("200.00"), response.today().highestPrice());
        assertEquals(30, response.dailyPrices().size());
        assertEquals(from, response.dailyPrices().getFirst().date());
        assertNull(response.dailyPrices().getFirst().averagePrice());
        assertEquals(0, response.dailyPrices().getFirst().offerCount());
        assertEquals(to, response.dailyPrices().getLast().date());
        assertEquals(new BigDecimal("150.00"), response.dailyPrices().getLast().averagePrice());
        assertEquals("RS", response.airlineRanking().getFirst().carrierCode());
        assertEquals("Air Seoul", response.airlineRanking().getFirst().carrierName());
        assertEquals(new BigDecimal("150.00"), response.airlineRanking().getFirst().averagePrice());
        assertEquals(2, response.airlineRanking().getFirst().offerCount());
        assertEquals("earlyMorning", response.departureTimeBands().getFirst().timeBand());
        assertNull(response.departureTimeBands().getFirst().averagePrice());
        assertEquals(0, response.departureTimeBands().getFirst().offerCount());
    }

    @Test
    void returnsEmptyDashboardWhenQueriesFindNoOffers() throws Exception {
        AthenaQueryService queryService = new AthenaQueryService(null, null) {
            @Override
            public <T> List<T> athenaExecute(
                    String sql, List<String> parameters, Function<Map<String, String>, T> mapper) {
                List<Map<String, String>> rows;
                if (sql.contains("AS min_price")) {
                    rows = List.of(row("offer_count", "0", "average_price", null,
                            "min_price", null, "max_price", null));
                } else if (sql.contains("band_summary")) {
                    rows = List.of(
                            row("time_band", "earlyMorning", "average_price", null, "offer_count", "0"),
                            row("time_band", "morning", "average_price", null, "offer_count", "0"),
                            row("time_band", "afternoon", "average_price", null, "offer_count", "0"),
                            row("time_band", "evening", "average_price", null, "offer_count", "0"));
                } else {
                    rows = List.of();
                }
                return rows.stream().map(mapper).toList();
            }
        };

        InitResponse response = new InitDashboardService(queryService, new SqlResourceLoader())
                .initDashboard(LocalDate.of(2026, 9, 22));

        assertNotNull(response.lastUpdatedAt());
        assertEquals(0, response.today().offerCount());
        assertNull(response.today().averagePrice());
        assertNull(response.today().lowestPrice());
        assertNull(response.today().highestPrice());
        assertEquals(30, response.dailyPrices().size());
        assertTrue(response.dailyPrices().stream()
                .allMatch(day -> day.averagePrice() == null && day.offerCount() == 0));
        assertTrue(response.airlineRanking().isEmpty());
        assertEquals(List.of("earlyMorning", "morning", "afternoon", "evening"),
                response.departureTimeBands().stream().map(InitResponse.DepartureTimeBand::timeBand).toList());
    }

    @Test
    void rejectsMissingTodayAggregateRow() {
        AthenaQueryService queryService = new AthenaQueryService(null, null) {
            @Override
            public <T> List<T> athenaExecute(
                    String sql, List<String> parameters, Function<Map<String, String>, T> mapper) {
                return List.of();
            }
        };

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new InitDashboardService(queryService, new SqlResourceLoader())
                        .initDashboard(LocalDate.of(2026, 9, 22)));

        assertEquals("Today query returned no aggregate row", error.getMessage());
    }

    private static String dateParameter(LocalDate date) {
        return "CAST('" + date + "' AS DATE)";
    }

    private static Map<String, String> row(String... values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            row.put(values[i], values[i + 1]);
        }
        return row;
    }
}
