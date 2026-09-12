from datetime import date, datetime, timezone
from decimal import Decimal

import pytest

from transform_flight_prices import _normalize_source_uri, flatten_raw_record


def test_flatten_raw_record_maps_itinerary_to_processed_row():
    raw_record = {
        "schema_version": 1,
        "searched_at": "2026-09-12T03:00:00+00:00",
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
                        "duration_minutes": 240,
                        "segments": [
                            {
                                "marketing_carrier_code": "TA",
                                "flight_number": "100",
                                "departure_airport": "KIX",
                                "departure_time_utc": "2026-12-12T01:00:00Z",
                                "departure_timezone": "Asia/Tokyo",
                                "arrival_airport": "FUK",
                                "arrival_time_utc": "2026-12-12T02:00:00Z",
                                "arrival_timezone": "Asia/Tokyo",
                                "aircraft": "Airbus A320",
                            },
                            {
                                "marketing_carrier_code": "TA",
                                "flight_number": "200",
                                "departure_airport": "FUK",
                                "departure_time_utc": "2026-12-12T03:00:00Z",
                                "departure_timezone": "Asia/Tokyo",
                                "arrival_airport": "ICN",
                                "arrival_time_utc": "2026-12-12T05:00:00Z",
                                "arrival_timezone": "Asia/Seoul",
                                "aircraft": "Airbus A321",
                            },
                        ],
                    },
                    "cabin_class": "economy",
                    "bags": {"carry_on": 1, "checked": 1},
                    "requires_self_transfer": False,
                    "ignav_id": "offer-1",
                }
            ],
        },
    }

    rows = flatten_raw_record(raw_record)

    assert rows == [
        {
            "search_date": date(2026, 9, 12),
            "searched_at": datetime(2026, 9, 12, 3, 0, tzinfo=timezone.utc),
            "departure_date": date(2026, 12, 12),
            "origin": "KIX",
            "destination": "ICN",
            "carrier_name": "Test Airline",
            "carrier_code": "TA",
            "flight_number": "100",
            "departure_at": datetime(2026, 12, 12, 1, 0, tzinfo=timezone.utc),
            "arrival_at": datetime(2026, 12, 12, 5, 0, tzinfo=timezone.utc),
            "departure_timezone": "Asia/Tokyo",
            "arrival_timezone": "Asia/Seoul",
            "duration_minutes": 240,
            "stops": 1,
            "aircraft": "Airbus A320",
            "cabin_class": "economy",
            "price": Decimal("28830"),
            "currency": "JPY",
            "price_status": "verified",
            "carry_on_bags": 1,
            "checked_bags": 1,
            "requires_self_transfer": False,
            "external_offer_id": "offer-1",
            "search_year": "2026",
            "search_month": "09",
        }
    ]


def test_flatten_raw_record_returns_empty_list_for_no_itineraries():
    raw_record = {
        "searched_at": "2026-09-12T03:00:00Z",
        "response": {
            "departure_date": "2026-12-12",
            "itineraries": [],
        },
    }

    assert flatten_raw_record(raw_record) == []


def test_flatten_raw_record_rejects_itinerary_without_segments():
    raw_record = {
        "searched_at": "2026-09-12T03:00:00Z",
        "response": {
            "departure_date": "2026-12-12",
            "itineraries": [{"outbound": {"segments": []}}],
        },
    }

    with pytest.raises(ValueError, match="outbound segment"):
        flatten_raw_record(raw_record)


def test_normalize_source_uri_decodes_local_file_uri_only():
    assert _normalize_source_uri("file:///tmp/route%3DKIX-ICN/data.json") == (
        "file:///tmp/route=KIX-ICN/data.json"
    )
    assert _normalize_source_uri("s3://bucket/route=KIX-ICN/data.json") == (
        "s3://bucket/route=KIX-ICN/data.json"
    )
