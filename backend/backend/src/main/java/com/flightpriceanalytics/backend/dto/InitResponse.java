package com.flightpriceanalytics.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InitResponse(
        Route route,
        Period period,
        String currency,
        Instant lastUpdatedAt,
        Today today,
        List<DailyPrice> dailyPrices,
        List<AirlineRanking> airlineRanking,
        List<DepartureTimeBand> departureTimeBands
) {
    public record Route(String origin, String destination) {
    }

    public record Period(LocalDate from, LocalDate to, String timezone) {
    }

    public record Today(
            LocalDate departureDate,
            long offerCount,
            BigDecimal averagePrice,
            BigDecimal lowestPrice,
            BigDecimal highestPrice
    ) {
    }

    public record DailyPrice(LocalDate date, BigDecimal averagePrice, long offerCount) {
    }

    public record AirlineRanking(
            String carrierCode,
            String carrierName,
            BigDecimal averagePrice,
            long offerCount
    ) {
    }

    public record DepartureTimeBand(String timeBand, BigDecimal averagePrice, long offerCount) {
    }
}
