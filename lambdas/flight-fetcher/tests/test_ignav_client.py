import pytest
import requests

from config import Config
from flight.ignav_client import IgnavClient


class FakeResponse:
    def __init__(self, data=None, error=None):
        self.data = data
        self.error = error

    def raise_for_status(self):
        if self.error:
            raise self.error

    def json(self):
        return self.data


class InvalidJsonResponse(FakeResponse):
    def json(self):
        raise requests.exceptions.JSONDecodeError("invalid", "{", 0)


class FakeSession:
    def __init__(self, response):
        self.response = response
        self.request = None

    def post(self, url, **kwargs):
        self.request = {"url": url, **kwargs}
        return self.response


class RaisingSession:
    def post(self, url, **kwargs):
        raise requests.Timeout("request timed out")


def make_config():
    return Config(
        origin="KIX",
        destination="ICN",
        market="JP",
        trip_type="one-way",
        request_timeout_seconds=10,
    )


def test_search_flights_sends_expected_request():
    response_data = {
        "origin": "KIX",
        "destination": "ICN",
        "departure_date": "2026-12-12",
        "itineraries": [],
    }
    session = FakeSession(FakeResponse(response_data))
    client = IgnavClient("secret", make_config(), session=session)

    result = client.search_flights(
        {
            "departure_date": "2026-12-12",
            "departure_time_range": {"earliest_hour": 6, "latest_hour": 12},
        }
    )

    assert result == response_data
    assert session.request == {
        "url": "https://ignav.com/api/fares/one-way",
        "headers": {"X-Api-Key": "secret"},
        "json": {
            "origin": "KIX",
            "destination": "ICN",
            "departure_date": "2026-12-12",
            "market": "JP",
            "departure_time_range": {"earliest_hour": 6, "latest_hour": 12},
        },
        "timeout": 10,
    }


@pytest.mark.parametrize("departure_date", [None, "", "2026/12/12", 20261212])
def test_search_flights_rejects_invalid_departure_date(departure_date):
    session = FakeSession(FakeResponse({}))
    client = IgnavClient("secret", make_config(), session=session)

    with pytest.raises(ValueError, match="departure_date"):
        client.search_flights({"departure_date": departure_date})

    assert session.request is None


def test_search_flights_propagates_http_error():
    http_error = requests.HTTPError("500 Server Error")
    session = FakeSession(FakeResponse(error=http_error))
    client = IgnavClient("secret", make_config(), session=session)

    with pytest.raises(requests.HTTPError):
        client.search_flights({"departure_date": "2026-12-12"})


def test_search_flights_propagates_timeout():
    client = IgnavClient("secret", make_config(), session=RaisingSession())

    with pytest.raises(requests.Timeout):
        client.search_flights({"departure_date": "2026-12-12"})


def test_search_flights_rejects_invalid_json():
    client = IgnavClient(
        "secret",
        make_config(),
        session=FakeSession(InvalidJsonResponse()),
    )

    with pytest.raises(ValueError, match="invalid JSON"):
        client.search_flights({"departure_date": "2026-12-12"})


@pytest.mark.parametrize(
    ("field", "value"),
    [("origin", "KIX1"), ("destination", "12"), ("market", "JPY")],
)
def test_search_flights_rejects_invalid_codes(field, value):
    session = FakeSession(FakeResponse({}))
    client = IgnavClient("secret", make_config(), session=session)

    with pytest.raises(ValueError, match=field):
        client.search_flights(
            {
                "departure_date": "2026-12-12",
                field: value,
            }
        )

    assert session.request is None


@pytest.mark.parametrize(
    "time_range",
    [
        "06-12",
        {"earliest_hour": -1},
        {"latest_hour": 24},
        {"earliest_hour": 12, "latest_hour": 6},
        {"unsupported": 10},
    ],
)
def test_search_flights_rejects_invalid_time_range(time_range):
    client = IgnavClient(
        "secret",
        make_config(),
        session=FakeSession(FakeResponse({})),
    )

    with pytest.raises(ValueError, match="departure_time_range|hour"):
        client.search_flights(
            {
                "departure_date": "2026-12-12",
                "departure_time_range": time_range,
            }
        )
