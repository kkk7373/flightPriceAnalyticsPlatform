import pytest

from storage.factory import create_storage
from storage.mock_storage import MockStorage
from storage.s3_storage import S3Storage


def test_create_mock_storage(monkeypatch, tmp_path):
    monkeypatch.setenv("STORAGE_TYPE", "mock")
    monkeypatch.setenv("MOCK_STORAGE_DIR", str(tmp_path))

    storage = create_storage()

    assert isinstance(storage, MockStorage)
    assert storage.directory == tmp_path


def test_unknown_storage_type_is_rejected(monkeypatch):
    monkeypatch.setenv("STORAGE_TYPE", "unknown")

    with pytest.raises(ValueError, match="STORAGE_TYPE"):
        create_storage()


def test_create_s3_storage(monkeypatch):
    s3_client = object()
    monkeypatch.setenv("STORAGE_TYPE", "s3")
    monkeypatch.setenv("RAW_BUCKET", "raw-bucket")
    monkeypatch.setenv("RAW_PREFIX", "raw/flight-offers")

    storage = create_storage(s3_client=s3_client)

    assert isinstance(storage, S3Storage)
    assert storage.bucket == "raw-bucket"
    assert storage.prefix == "raw/flight-offers"
    assert storage.s3_client is s3_client


def test_s3_storage_requires_bucket(monkeypatch):
    monkeypatch.setenv("STORAGE_TYPE", "s3")
    monkeypatch.delenv("RAW_BUCKET", raising=False)

    with pytest.raises(ValueError, match="RAW_BUCKET"):
        create_storage(s3_client=object())
