import pytest

from config import Config


def test_config_uses_kix_to_icn_defaults(monkeypatch):
    for variable in (
        "ORIGIN",
        "DESTINATION",
        "MARKET",
        "TRIP_TYPE",
        "REQUEST_TIMEOUT_SECONDS",
    ):
        monkeypatch.delenv(variable, raising=False)

    config = Config.from_env()

    assert config.origin == "KIX"
    assert config.destination == "ICN"
    assert config.market == "JP"
    assert config.trip_type == "one-way"
    assert config.request_timeout_seconds == 30


def test_config_rejects_non_positive_timeout(monkeypatch):
    monkeypatch.setenv("REQUEST_TIMEOUT_SECONDS", "0")

    with pytest.raises(ValueError, match="greater than 0"):
        Config.from_env()
