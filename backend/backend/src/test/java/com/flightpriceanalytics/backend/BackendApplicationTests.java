package com.flightpriceanalytics.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.benmanes.caffeine.cache.Cache;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private CacheManager cacheManager;

	@Test
	void contextLoads() {
	}

	@Test
	void initDashboardCacheExpiresAfterOneDay() {
		assertInstanceOf(CaffeineCacheManager.class, cacheManager);
		Cache<?, ?> nativeCache = (Cache<?, ?>) cacheManager.getCache("initDashboard").getNativeCache();
		assertEquals(24, nativeCache.policy().expireAfterWrite().orElseThrow()
				.getExpiresAfter(TimeUnit.HOURS));
	}

}
