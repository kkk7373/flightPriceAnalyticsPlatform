package com.flightpriceanalytics.backend.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class SqlResourceLoaderTest {

    private final SqlResourceLoader loader = new SqlResourceLoader();

    @ParameterizedTest
    @ValueSource(strings = {
            "init/today.sql",
            "init/daily_prices.sql",
            "init/airline_ranking.sql",
            "init/departure_time_bands.sql"
    })
    void loadsInitSqlFromClasspath(String path) throws IOException {
        String sql = loader.loadSql(path);

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("flight_analytics.flight_prices"));
    }

    @Test
    void failsClearlyForMissingSql() {
        assertThrows(IOException.class, () -> loader.loadSql("init/missing.sql"));
    }
}
