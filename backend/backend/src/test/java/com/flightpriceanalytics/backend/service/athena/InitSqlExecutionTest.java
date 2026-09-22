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

import org.junit.jupiter.api.Test;

class InitSqlExecutionTest {

    @Test
    void todayQueryAggregatesOnlyEligibleOffersForRequestedDepartureDate() throws Exception {
        try (Connection connection = newDatabase();
                PreparedStatement query = connection.prepareStatement(readSql("today.sql"))) {
            query.setDate(1, Date.valueOf("2026-09-22"));

            try (ResultSet result = query.executeQuery()) {
                assertTrue(result.next());
                assertEquals(2, result.getLong("offer_count"));
                assertMoney("150.00", result.getBigDecimal("average_price"));
                assertMoney("100.00", result.getBigDecimal("min_price"));
                assertMoney("200.00", result.getBigDecimal("max_price"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void todayQueryReturnsZeroCountAndNullPricesWhenDateHasNoOffers() throws Exception {
        try (Connection connection = newDatabase();
                PreparedStatement query = connection.prepareStatement(readSql("today.sql"))) {
            query.setDate(1, Date.valueOf("2026-09-19"));

            try (ResultSet result = query.executeQuery()) {
                assertTrue(result.next());
                assertEquals(0, result.getLong("offer_count"));
                assertNull(result.getBigDecimal("average_price"));
                assertNull(result.getBigDecimal("min_price"));
                assertNull(result.getBigDecimal("max_price"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void dailyPricesQueryGroupsWithinInclusivePeriodInDateOrder() throws Exception {
        try (Connection connection = newDatabase();
                PreparedStatement query = connection.prepareStatement(readSql("daily_prices.sql"))) {
            query.setDate(1, Date.valueOf("2026-09-21"));
            query.setDate(2, Date.valueOf("2026-09-22"));

            try (ResultSet result = query.executeQuery()) {
                assertTrue(result.next());
                assertEquals(Date.valueOf("2026-09-21"), result.getDate("departure_date"));
                assertEquals(1, result.getLong("offer_count_by_date"));
                assertMoney("300.00", result.getBigDecimal("avg_price_by_date"));

                assertTrue(result.next());
                assertEquals(Date.valueOf("2026-09-22"), result.getDate("departure_date"));
                assertEquals(2, result.getLong("offer_count_by_date"));
                assertMoney("150.00", result.getBigDecimal("avg_price_by_date"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void airlineRankingQueryExcludesMissingCarrierAndBreaksAveragePriceTiesByCode() throws Exception {
        try (Connection connection = newDatabase();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO flight_analytics.flight_prices VALUES
                        (DATE '2026-09-22', 50.00, 'economy', 'verified', NULL, 'CC'),
                        (DATE '2026-09-22', 50.00, 'economy', 'verified', 'Unknown', NULL)
                    """);

            try (PreparedStatement query = connection.prepareStatement(readSql("airline_ranking.sql"))) {
                query.setDate(1, Date.valueOf("2026-09-21"));
                query.setDate(2, Date.valueOf("2026-09-22"));

                try (ResultSet result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("AA", result.getString("carrier_code"));
                    assertEquals("Alpha", result.getString("carrier_name"));
                    assertEquals(2, result.getLong("offer_count_by_airline"));
                    assertMoney("200.00", result.getBigDecimal("average_price_by_airline"));

                    assertTrue(result.next());
                    assertEquals("BB", result.getString("carrier_code"));
                    assertEquals("Beta", result.getString("carrier_name"));
                    assertEquals(1, result.getLong("offer_count_by_airline"));
                    assertMoney("200.00", result.getBigDecimal("average_price_by_airline"));
                    assertFalse(result.next());
                }
            }
        }
    }

    private static Connection newDatabase() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA flight_analytics");
            statement.execute("""
                    CREATE TABLE flight_analytics.flight_prices (
                        departure_date DATE,
                        price DECIMAL(12, 2),
                        cabin_class VARCHAR(30),
                        price_status VARCHAR(30),
                        carrier_name VARCHAR(100),
                        carrier_code VARCHAR(20)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO flight_analytics.flight_prices VALUES
                        (DATE '2026-09-21', 300.00, 'economy', 'verified', 'Alpha', 'AA'),
                        (DATE '2026-09-22', 100.00, 'economy', 'verified', 'Alpha', 'AA'),
                        (DATE '2026-09-22', 200.00, 'economy', 'verified', 'Beta', 'BB'),
                        (DATE '2026-09-22', 1.00, 'economy', 'unverified', 'Alpha', 'AA'),
                        (DATE '2026-09-22', 2.00, 'business', 'verified', 'Alpha', 'AA'),
                        (DATE '2026-09-22', 0.00, 'economy', 'verified', 'Alpha', 'AA'),
                        (DATE '2026-09-20', 999.00, 'economy', 'verified', 'Alpha', 'AA')
                    """);
        }
        return connection;
    }

    private static String readSql(String fileName) throws Exception {
        try (InputStream input = InitSqlExecutionTest.class.getResourceAsStream("/sql/init/" + fileName)) {
            assertNotNull(input, fileName + " must be on the classpath");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
