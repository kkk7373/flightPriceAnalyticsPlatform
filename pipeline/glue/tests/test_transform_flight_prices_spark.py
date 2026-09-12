import json

import pytest


pytest.importorskip("pyspark")

from pyspark.sql import SparkSession

from transform_flight_prices import run_job


def test_run_job_writes_partitioned_parquet(tmp_path):
    raw_path = tmp_path / "raw.json"
    output_path = tmp_path / "processed"
    raw_path.write_text(
        json.dumps(
            {
                "schema_version": 1,
                "searched_at": "2026-09-12T03:00:00Z",
                "response": {
                    "origin": "KIX",
                    "destination": "ICN",
                    "departure_date": "2026-12-12",
                    "itineraries": [
                        {
                            "price": {
                                "amount": 28830,
                                "currency": "JPY",
                                "status": "verified",
                            },
                            "outbound": {
                                "carrier": "Test Airline",
                                "duration_minutes": 130,
                                "segments": [
                                    {
                                        "marketing_carrier_code": "TA",
                                        "flight_number": "100",
                                        "departure_airport": "KIX",
                                        "departure_time_utc": "2026-12-12T01:00:00Z",
                                        "departure_timezone": "Asia/Tokyo",
                                        "arrival_airport": "ICN",
                                        "arrival_time_utc": "2026-12-12T03:10:00Z",
                                        "arrival_timezone": "Asia/Seoul",
                                        "aircraft": "Airbus A321",
                                    }
                                ],
                            },
                            "cabin_class": "economy",
                            "requires_self_transfer": False,
                            "ignav_id": "offer-1",
                        }
                    ],
                },
            }
        ),
        encoding="utf-8",
    )

    spark = (
        SparkSession.builder.master("local[1]")
        .appName("flight-price-transform-test")
        .getOrCreate()
    )
    try:
        run_job(raw_path.as_uri(), output_path.as_uri(), spark)

        dataframe = spark.read.parquet(str(output_path))
        row = dataframe.collect()[0]
        assert dataframe.count() == 1
        assert row.origin == "KIX"
        assert row.destination == "ICN"
        assert row.price == 28830
        assert row.price_status == "verified"
        assert row.stops == 0
        assert list(output_path.glob("search_year=2026/search_month=09/*.parquet"))
    finally:
        spark.stop()
