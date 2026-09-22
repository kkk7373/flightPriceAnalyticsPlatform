-- Parameters: 1 = period.from (DATE), 2 = period.to (DATE).
SELECT
    AVG(price) AS avg_price_by_date,
    COUNT(*) AS offer_count_by_date,
    departure_date
FROM flight_analytics.flight_prices
WHERE departure_date BETWEEN ? AND ?
  AND price > 0
  AND cabin_class = 'economy'
  AND price_status = 'verified'
GROUP BY departure_date
ORDER BY departure_date;
