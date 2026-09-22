package com.flightpriceanalytics.backend.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.flightpriceanalytics.backend.dto.InitResponse;
import com.flightpriceanalytics.backend.service.init.InitDashboardService;

class ApiControllerTest {

    @Test
    void getInitReturnsOkAndDashboardJson() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 22);
        InitResponse response = new InitResponse(
                new InitResponse.Route("KIX", "ICN"),
                new InitResponse.Period(date.minusDays(29), date, "Asia/Tokyo"),
                "JPY",
                Instant.parse("2026-09-22T07:05:04Z"),
                new InitResponse.Today(date, 2, new BigDecimal("150.00"),
                        new BigDecimal("100.00"), new BigDecimal("200.00")),
                List.of(new InitResponse.DailyPrice(date, new BigDecimal("150.00"), 2)),
                List.of(new InitResponse.AirlineRanking("RS", "Air Seoul", new BigDecimal("150.00"), 2)),
                List.of(new InitResponse.DepartureTimeBand("morning", null, 0)));
        InitDashboardService service = new InitDashboardService(null, null) {
            @Override
            public InitResponse initDashboard(LocalDate referenceDate) {
                assertEquals(LocalDate.now(ZoneId.of("Asia/Tokyo")), referenceDate);
                return response;
            }
        };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ApiController(service)).build();

        mvc.perform(get("/api/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.origin").value("KIX"))
                .andExpect(jsonPath("$.period.to").value("2026-09-22"))
                .andExpect(jsonPath("$.currency").value("JPY"))
                .andExpect(jsonPath("$.today.departureDate").value("2026-09-22"))
                .andExpect(jsonPath("$.today.offerCount").value(2))
                .andExpect(jsonPath("$.dailyPrices[0].averagePrice").value(150.00))
                .andExpect(jsonPath("$.airlineRanking[0].carrierCode").value("RS"))
                .andExpect(jsonPath("$.departureTimeBands[0].averagePrice").value(nullValue()));
    }
}
