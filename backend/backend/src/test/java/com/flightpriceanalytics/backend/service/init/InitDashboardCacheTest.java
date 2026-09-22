package com.flightpriceanalytics.backend.service.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.flightpriceanalytics.backend.config.CacheConfiguration;
import com.flightpriceanalytics.backend.dto.InitResponse;
import com.flightpriceanalytics.backend.service.athena.AthenaQueryService;
import com.flightpriceanalytics.backend.util.SqlResourceLoader;

class InitDashboardCacheTest {

    @Test
    void cachesAllFourQueriesByReferenceDate() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(CacheConfiguration.class, TestBeans.class)) {
            InitDashboardService service = context.getBean(InitDashboardService.class);
            FakeAthenaQueryService athena = context.getBean(FakeAthenaQueryService.class);
            LocalDate firstDate = LocalDate.of(2026, 9, 22);

            InitResponse first = service.initDashboard(firstDate);
            InitResponse cached = service.initDashboard(firstDate);

            assertSame(first, cached);
            assertEquals(4, athena.queryCount.get());

            InitResponse nextDate = service.initDashboard(firstDate.plusDays(1));
            assertNotSame(first, nextDate);
            assertEquals(firstDate.plusDays(1), nextDate.period().to());
            assertEquals(8, athena.queryCount.get());
            assertSame(first, service.initDashboard(firstDate));
            assertEquals(8, athena.queryCount.get());
        }
    }

    @Test
    void failedQueryIsNotCached() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(CacheConfiguration.class, TestBeans.class)) {
            InitDashboardService service = context.getBean(InitDashboardService.class);
            FakeAthenaQueryService athena = context.getBean(FakeAthenaQueryService.class);
            LocalDate date = LocalDate.of(2026, 9, 22);
            athena.failNextQuery.set(true);

            assertThrows(IllegalStateException.class, () -> service.initDashboard(date));
            service.initDashboard(date);
            assertEquals(5, athena.queryCount.get());
        }
    }

    @Configuration
    static class TestBeans {
        @Bean
        CacheManager cacheManager() {
            CaffeineCacheManager manager = new CaffeineCacheManager("initDashboard");
            manager.setCacheSpecification("maximumSize=32,expireAfterWrite=24h");
            return manager;
        }

        @Bean
        FakeAthenaQueryService athenaQueryService() {
            return new FakeAthenaQueryService();
        }

        @Bean
        InitDashboardService initDashboardService(FakeAthenaQueryService athena) {
            return new InitDashboardService(athena, new SqlResourceLoader());
        }
    }

    static class FakeAthenaQueryService extends AthenaQueryService {
        final AtomicInteger queryCount = new AtomicInteger();
        final AtomicBoolean failNextQuery = new AtomicBoolean();

        FakeAthenaQueryService() {
            super(null, null);
        }

        @Override
        public <T> List<T> athenaExecute(
                String sql, List<String> parameters, Function<Map<String, String>, T> mapper) {
            queryCount.incrementAndGet();
            if (failNextQuery.getAndSet(false)) {
                throw new IllegalStateException("Athena unavailable");
            }
            if (sql.contains("AS min_price")) {
                return List.of(mapper.apply(Map.of(
                        "offer_count", "1",
                        "average_price", "150.00",
                        "min_price", "150.00",
                        "max_price", "150.00")));
            }
            return List.of();
        }
    }
}
