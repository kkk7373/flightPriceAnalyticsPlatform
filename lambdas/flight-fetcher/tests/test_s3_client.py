import json
from datetime import datetime, timezone

from storage.s3_storage import S3Storage


class FakeS3Client:
    def __init__(self):
        self.request = None

    def put_object(self, **kwargs):
        self.request = kwargs


def test_save_writes_partitioned_raw_json_and_returns_s3_uri():
    s3_client = FakeS3Client()
    searched_at = datetime(2026, 9, 12, 3, 0, tzinfo=timezone.utc)
    flight_info = {
        "origin": "KIX",
        "destination": "ICN",
        "departure_date": "2026-12-12",
    }
    storage = S3Storage(
        bucket="raw-bucket",
        prefix="/raw/flight-offers/",
        s3_client=s3_client,
        clock=lambda: searched_at,
    )

    storage_uri = storage.save(flight_info)

    expected_key = (
        "raw/flight-offers/collected_date=2026-09-12/route=KIX-ICN/"
        "20260912T030000000000Z.json"
    )
    assert storage_uri == f"s3://raw-bucket/{expected_key}"
    assert s3_client.request["Bucket"] == "raw-bucket"
    assert s3_client.request["Key"] == expected_key
    assert s3_client.request["ContentType"] == "application/json"
    assert json.loads(s3_client.request["Body"]) == {
        "schema_version": 1,
        "searched_at": "2026-09-12T03:00:00+00:00",
        "response": flight_info,
    }
