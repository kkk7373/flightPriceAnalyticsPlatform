package com.flightpriceanalytics.backend.service.athena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

public class DepartureTimeBandsSqlTest {

    @Test
    void queryConvertsUtcToDepartureTimezoneAndIncludesEmptyBands() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:");
                Statement setup = connection.createStatement()) {
            setup.execute("CREATE SCHEMA flight_analytics");
            setup.execute("""
                    CREATE TABLE flight_analytics.flight_prices (
                        departure_date DATE,
                        price DECIMAL(12, 2),
                        cabin_class VARCHAR(30),
                        price_status VARCHAR(30),
                        departure_at TIMESTAMP,
                        departure_timezone VARCHAR(100)
                    )
                    """);
            setup.execute("""
                    CREATE ALIAS WITH_TIMEZONE FOR
                    "com.flightpriceanalytics.backend.service.athena.DepartureTimeBandsSqlTest.withTimezone"
                    """);
            setup.execute("""
                    CREATE ALIAS AT_TIMEZONE FOR
                    "com.flightpriceanalytics.backend.service.athena.DepartureTimeBandsSqlTest.atTimezone"
                    """);
            setup.executeUpdate("""
                    INSERT INTO flight_analytics.flight_prices VALUES
                        (DATE '2026-09-22', 100.00, 'economy', 'verified', TIMESTAMP '2026-09-21 17:00:00', 'Asia/Tokyo'),
                        (DATE '2026-09-22', 200.00, 'economy', 'verified', TIMESTAMP '2026-09-22 00:00:00', 'Asia/Tokyo'),
                        (DATE '2026-09-22', 300.00, 'economy', 'verified', TIMESTAMP '2026-09-22 06:00:00', 'Asia/Tokyo'),
                        (DATE '2026-09-22', 400.00, 'economy', 'verified', TIMESTAMP '2026-09-22 09:00:00', NULL),
                        (DATE '2026-09-22', 500.00, 'economy', 'verified', NULL, 'Asia/Tokyo'),
                        (DATE '2026-09-22', 0.00, 'economy', 'verified', TIMESTAMP '2026-09-22 09:00:00', 'Asia/Tokyo'),
                        (DATE '2026-09-22', 600.00, 'economy', 'unverified', TIMESTAMP '2026-09-22 09:00:00', 'Asia/Tokyo'),
                        (DATE '2026-09-21', 700.00, 'economy', 'verified', TIMESTAMP '2026-09-21 09:00:00', 'Asia/Tokyo')
                    """);

            try (PreparedStatement query = connection.prepareStatement(readSql())) {
                query.setDate(1, Date.valueOf("2026-09-22"));
                query.setDate(2, Date.valueOf("2026-09-22"));

                try (ResultSet result = query.executeQuery()) {
                    assertBand(result, "earlyMorning", "100.00", 1);
                    assertBand(result, "morning", "200.00", 1);
                    assertBand(result, "afternoon", "300.00", 1);
                    assertBand(result, "evening", null, 0);
                    assertFalse(result.next());
                }
            }
        }
    }

    @Test
    void queryKeepsPeriodTimezoneAndFourBandContract() throws Exception {
        String sql = readSql();
        assertEquals(2, sql.chars().filter(character -> character == '?').count());
        assertTrue(sql.contains("departure_date BETWEEN ? AND ?"));
        assertTrue(sql.contains("price > 0"));
        assertTrue(sql.contains("cabin_class = 'economy'"));
        assertTrue(sql.contains("price_status = 'verified'"));
        assertTrue(sql.contains("AT_TIMEZONE(WITH_TIMEZONE(departure_at, 'UTC'), departure_timezone)"));
        assertTrue(sql.contains("departure_at IS NOT NULL"));
        assertTrue(sql.contains("departure_timezone IS NOT NULL"));
        assertTrue(sql.contains("WHEN local_hour < 6 THEN 'earlyMorning'"));
        assertTrue(sql.contains("WHEN local_hour < 12 THEN 'morning'"));
        assertTrue(sql.contains("WHEN local_hour < 18 THEN 'afternoon'"));
        assertTrue(sql.contains("LEFT JOIN band_summary"));
        assertTrue(sql.contains("AVG(price) AS average_price"));
        assertTrue(sql.contains("COALESCE(band_summary.offer_count, 0) AS offer_count"));
        assertTrue(sql.contains("ORDER BY band_order.sort_order"));

        String[] bands = {"'earlyMorning'", "'morning'", "'afternoon'", "'evening'"};
        String bandOrder = sql.substring(sql.indexOf("band_order (time_band, sort_order) AS"));
        int previousIndex = -1;
        for (String band : bands) {
            int index = bandOrder.indexOf(band);
            assertTrue(index > previousIndex, band + " must appear in time order");
            previousIndex = index;
        }
    }

    public static OffsetDateTime withTimezone(Timestamp timestamp, String zone) {
        return timestamp.toLocalDateTime().atZone(ZoneId.of(zone)).toOffsetDateTime();
    }

    public static OffsetDateTime atTimezone(OffsetDateTime timestamp, String zone) {
        return timestamp.atZoneSameInstant(ZoneId.of(zone)).toOffsetDateTime();
    }

    private static String readSql() throws Exception {
        try (InputStream input = DepartureTimeBandsSqlTest.class.getResourceAsStream(
                "/sql/init/departure_time_bands.sql")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertBand(ResultSet result, String expectedBand, String expectedPrice, long expectedCount)
            throws Exception {
        assertTrue(result.next());
        assertEquals(expectedBand, result.getString("time_band"));
        assertEquals(expectedCount, result.getLong("offer_count"));
        if (expectedPrice == null) {
            assertNull(result.getBigDecimal("average_price"));
        } else {
            assertEquals(0, result.getBigDecimal("average_price").compareTo(new BigDecimal(expectedPrice)));
        }
    }
}
