-- Parameters: 1 = period.from (DATE), 2 = period.to (DATE).
-- departure_at is stored as a UTC timestamp without a timezone.
WITH local_departures AS (
    SELECT
        price,
        HOUR(AT_TIMEZONE(WITH_TIMEZONE(departure_at, 'UTC'), departure_timezone)) AS local_hour
    FROM flight_analytics.flight_prices
    WHERE departure_date BETWEEN ? AND ?
      AND price > 0
      AND cabin_class = 'economy'
      AND price_status = 'verified'
      AND departure_at IS NOT NULL
      AND departure_timezone IS NOT NULL
      AND TRIM(departure_timezone) <> ''
),
classified AS (
    SELECT
        price,
        CASE
            WHEN local_hour < 6 THEN 'earlyMorning'
            WHEN local_hour < 12 THEN 'morning'
            WHEN local_hour < 18 THEN 'afternoon'
            ELSE 'evening'
        END AS time_band
    FROM local_departures
),
band_summary AS (
    SELECT
        time_band,
        AVG(price) AS average_price,
        COUNT(*) AS offer_count
    FROM classified
    GROUP BY time_band
),
band_order (time_band, sort_order) AS (
    VALUES
        ('earlyMorning', 1),
        ('morning', 2),
        ('afternoon', 3),
        ('evening', 4)
)
SELECT
    band_order.time_band,
    band_summary.average_price,
    COALESCE(band_summary.offer_count, 0) AS offer_count
FROM band_order
LEFT JOIN band_summary ON band_order.time_band = band_summary.time_band
ORDER BY band_order.sort_order;
