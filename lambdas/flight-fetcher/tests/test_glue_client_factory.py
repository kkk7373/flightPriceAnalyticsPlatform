import pytest

from glue.client import GlueClient
from glue.factory import create_glue_client
from glue.mock_client import MockGlueClient


def test_create_mock_glue_client(monkeypatch):
    monkeypatch.setenv("GLUE_CLIENT", "mock")

    client = create_glue_client()

    assert isinstance(client, MockGlueClient)


def test_create_aws_glue_client(monkeypatch):
    boto_client = object()
    monkeypatch.setenv("GLUE_CLIENT", "aws")
    monkeypatch.setenv("GLUE_JOB_NAME", "transform-flight-prices")

    client = create_glue_client(aws_glue_client=boto_client)

    assert isinstance(client, GlueClient)
    assert client.job_name == "transform-flight-prices"
    assert client.glue_client is boto_client


def test_aws_glue_client_requires_job_name(monkeypatch):
    monkeypatch.setenv("GLUE_CLIENT", "aws")
    monkeypatch.delenv("GLUE_JOB_NAME", raising=False)

    with pytest.raises(ValueError, match="GLUE_JOB_NAME"):
        create_glue_client(aws_glue_client=object())


def test_unknown_glue_client_is_rejected(monkeypatch):
    monkeypatch.setenv("GLUE_CLIENT", "unknown")

    with pytest.raises(ValueError, match="GLUE_CLIENT"):
        create_glue_client()
