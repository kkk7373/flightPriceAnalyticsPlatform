-- Parameters: 1 = period.to (DATE; today in Asia/Tokyo).
SELECT
    COUNT(*) AS offer_count,
    AVG(price) AS average_price,
    MIN(price) AS min_price,
    MAX(price) AS max_price
FROM flight_analytics.flight_prices
WHERE cabin_class = 'economy'
  AND price > 0
  AND price_status = 'verified'
  AND departure_date = ?;
