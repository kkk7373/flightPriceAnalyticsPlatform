package com.flightpriceanalytics.backend.service.init;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.flightpriceanalytics.backend.dto.InitResponse;
import com.flightpriceanalytics.backend.dto.InitResponse.AirlineRanking;
import com.flightpriceanalytics.backend.dto.InitResponse.DailyPrice;
import com.flightpriceanalytics.backend.dto.InitResponse.DepartureTimeBand;
import com.flightpriceanalytics.backend.dto.InitResponse.Today;
import com.flightpriceanalytics.backend.service.athena.AthenaQueryService;
import com.flightpriceanalytics.backend.service.athena.AthenaResultException;
import com.flightpriceanalytics.backend.util.SqlResourceLoader;

@Service
public class InitDashboardService {

    private final AthenaQueryService athenaQueryService;
    private final SqlResourceLoader sqlResourceLoader;

    public InitDashboardService(
            AthenaQueryService athenaQueryService,
            SqlResourceLoader sqlResourceLoader) {
        this.athenaQueryService = athenaQueryService;
        this.sqlResourceLoader = sqlResourceLoader;
    }

    @Cacheable(cacheNames = "initDashboard", key = "#p0", sync = true)
    public InitResponse initDashboard(LocalDate to) throws IOException, InterruptedException {
        LocalDate from = to.minusDays(29);

        Today today = fetchToday(to);
        List<DailyPrice> dailyPrices = fillMissingDays(from, to, fetchDailyPrices(from, to));
        List<AirlineRanking> airlineRanking = fetchAirlineRanking(from, to);
        List<DepartureTimeBand> departureTimeBands = fetchDepartureTimeBands(from, to);

        return new InitResponse(
                new InitResponse.Route("KIX", "ICN"),
                new InitResponse.Period(from, to, "Asia/Tokyo"),
                "JPY",
                Instant.now(),
                today,
                dailyPrices,
                airlineRanking,
                departureTimeBands);
    }

    private Today fetchToday(LocalDate currentDate) throws IOException, InterruptedException {
        Function<Map<String, String>, Today> mapper = row -> new Today(
                currentDate,
                Long.parseLong(row.get("offer_count")),
                decimalOrNull(row.get("average_price")),
                decimalOrNull(row.get("min_price")),
                decimalOrNull(row.get("max_price")));

        String sql = sqlResourceLoader.loadSql("init/today.sql");
        List<Today> results = athenaQueryService.athenaExecute(
                sql, List.of(dateParameter(currentDate)), mapper);
        if (results.isEmpty()) {
            throw new AthenaResultException("Today query returned no aggregate row");
        }
        return results.getFirst();
    }

    private List<DailyPrice> fetchDailyPrices(LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        Function<Map<String, String>, DailyPrice> mapper = row -> new DailyPrice(
                LocalDate.parse(row.get("departure_date")),
                decimalOrNull(row.get("avg_price_by_date")),
                Long.parseLong(row.get("offer_count_by_date")));

        String sql = sqlResourceLoader.loadSql("init/daily_prices.sql");
        return athenaQueryService.athenaExecute(sql, periodParameters(from, to), mapper);
    }

    private List<AirlineRanking> fetchAirlineRanking(LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        Function<Map<String, String>, AirlineRanking> mapper = row -> new AirlineRanking(
                row.get("carrier_code"),
                row.get("carrier_name"),
                decimalOrNull(row.get("average_price_by_airline")),
                Long.parseLong(row.get("offer_count_by_airline")));

        String sql = sqlResourceLoader.loadSql("init/airline_ranking.sql");
        return athenaQueryService.athenaExecute(sql, periodParameters(from, to), mapper);
    }

    private List<DepartureTimeBand> fetchDepartureTimeBands(LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        Function<Map<String, String>, DepartureTimeBand> mapper = row -> new DepartureTimeBand(
                row.get("time_band"),
                decimalOrNull(row.get("average_price")),
                Long.parseLong(row.get("offer_count")));

        String sql = sqlResourceLoader.loadSql("init/departure_time_bands.sql");
        return athenaQueryService.athenaExecute(sql, periodParameters(from, to), mapper);
    }

    private static List<DailyPrice> fillMissingDays(
            LocalDate from, LocalDate to, List<DailyPrice> fetched) {
        Map<LocalDate, DailyPrice> byDate = new HashMap<>();
        for (DailyPrice price : fetched) {
            byDate.put(price.date(), price);
        }

        List<DailyPrice> dailyPrices = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            dailyPrices.add(byDate.getOrDefault(date, new DailyPrice(date, null, 0)));
        }
        return dailyPrices;
    }

    private static List<String> periodParameters(LocalDate from, LocalDate to) {
        return List.of(dateParameter(from), dateParameter(to));
    }

    private static String dateParameter(LocalDate date) {
        return "CAST('" + date + "' AS DATE)";
    }

    private static BigDecimal decimalOrNull(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
