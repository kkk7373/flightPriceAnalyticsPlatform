import json
from datetime import datetime, timezone

import boto3

from storage.raw_data import build_partition_key, build_raw_record


class S3Storage:
    def __init__(self, bucket: str, prefix: str, s3_client=None, clock=None):
        self.bucket = bucket
        self.prefix = prefix.strip("/")
        if not self.prefix:
            raise ValueError("prefix is required")
        self.s3_client = s3_client or boto3.client("s3")
        self.clock = clock or (lambda: datetime.now(timezone.utc))

    def save(self, flight_info: dict) -> str:
        searched_at = self.clock()
        partition_key = build_partition_key(flight_info, searched_at)
        key = f"{self.prefix}/{partition_key}"
        raw_data = build_raw_record(flight_info, searched_at)

        self.s3_client.put_object(
            Bucket=self.bucket,
            Key=key,
            Body=json.dumps(
                raw_data,
                ensure_ascii=False,
            ).encode("utf-8"),
            ContentType="application/json",
        )

        return f"s3://{self.bucket}/{key}"
