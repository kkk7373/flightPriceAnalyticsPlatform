import os
from pathlib import Path

from storage.mock_storage import MockStorage


def create_storage(s3_client=None):
    storage_type = os.getenv("STORAGE_TYPE")

    if storage_type == "mock":
        default_directory = Path(__file__).resolve().parents[2] / ".local" / "raw"
        directory = os.getenv("MOCK_STORAGE_DIR") or default_directory
        return MockStorage(directory)

    if storage_type == "s3":
        from storage.s3_storage import S3Storage

        bucket = os.getenv("RAW_BUCKET")
        if not bucket:
            raise ValueError("RAW_BUCKET is required")

        return S3Storage(
            bucket=bucket,
            prefix=os.getenv("RAW_PREFIX", "raw/flight-offers"),
            s3_client=s3_client,
        )

    raise ValueError("STORAGE_TYPE must be either 'mock' or 's3'")
