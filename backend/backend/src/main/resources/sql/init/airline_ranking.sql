-- Parameters: 1 = period.from (DATE), 2 = period.to (DATE).
SELECT
    carrier_name,
    carrier_code,
    AVG(price) AS average_price_by_airline,
    COUNT(*) AS offer_count_by_airline
FROM flight_analytics.flight_prices
WHERE departure_date BETWEEN ? AND ?
  AND price > 0
  AND cabin_class = 'economy'
  AND price_status = 'verified'
  AND carrier_code IS NOT NULL
  AND carrier_name IS NOT NULL
GROUP BY carrier_name, carrier_code
ORDER BY average_price_by_airline ASC, carrier_code ASC;
