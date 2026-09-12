import pytest

from flight.factory import create_flight_client
from flight.ignav_client import IgnavClient
from flight.mock_client import MockFlightClient


def test_create_mock_flight_client(monkeypatch):
    monkeypatch.setenv("FLIGHT_CLIENT", "mock")

    assert isinstance(create_flight_client(), MockFlightClient)


def test_create_ignav_client(monkeypatch):
    monkeypatch.setenv("FLIGHT_CLIENT", "ignav")
    monkeypatch.setenv("IGNAV_API_KEY", "test-key")
    monkeypatch.setenv("ORIGIN", "KIX")
    monkeypatch.setenv("DESTINATION", "ICN")
    monkeypatch.setenv("MARKET", "JP")

    client = create_flight_client()

    assert isinstance(client, IgnavClient)
    assert client.config.origin == "KIX"
    assert client.config.destination == "ICN"
    assert client.config.market == "JP"


def test_ignav_client_requires_api_key(monkeypatch):
    monkeypatch.setenv("FLIGHT_CLIENT", "ignav")
    monkeypatch.delenv("IGNAV_API_KEY", raising=False)

    with pytest.raises(ValueError, match="IGNAV_API_KEY"):
        create_flight_client()


def test_unknown_flight_client_is_rejected(monkeypatch):
    monkeypatch.setenv("FLIGHT_CLIENT", "unknown")

    with pytest.raises(ValueError, match="FLIGHT_CLIENT"):
        create_flight_client()
