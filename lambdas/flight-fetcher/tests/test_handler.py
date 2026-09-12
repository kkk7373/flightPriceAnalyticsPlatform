import pytest

import handler
from handler import lambda_handler, process_event


class FakeFlightClient:
    def search_flights(self, event):
        return {"origin": event["origin"], "destination": event["destination"]}


class FakeStorage:
    def __init__(self):
        self.saved_data = None

    def save(self, flight_info):
        self.saved_data = flight_info
        return "file:///tmp/flight-data.json"


class FakeGlueClient:
    def __init__(self):
        self.source_uri = None

    def start_job(self, source_uri):
        self.source_uri = source_uri
        return "mock-job-run-id"


class FailingFlightClient:
    def search_flights(self, event):
        raise RuntimeError("flight fetch failed")


class FailingGlueClient:
    def start_job(self, source_uri):
        raise RuntimeError("glue failed")


def test_process_event_saves_fetched_flight_info():
    storage = FakeStorage()
    glue_client = FakeGlueClient()

    result = process_event(
        {
            "origin": "KIX",
            "destination": "ICN",
            "departure_date": "2026-12-12",
        },
        flight_client=FakeFlightClient(),
        storage=storage,
        glue_client=glue_client,
    )

    expected_flight_info = {"origin": "KIX", "destination": "ICN"}
    assert storage.saved_data == expected_flight_info
    assert glue_client.source_uri == "file:///tmp/flight-data.json"
    assert result == {
        "flight_info": expected_flight_info,
        "storage_uri": "file:///tmp/flight-data.json",
        "glue_job_run_id": "mock-job-run-id",
    }


def test_process_event_does_not_start_glue_when_storage_fails():
    class FailingStorage:
        def save(self, flight_info):
            raise RuntimeError("storage failed")

    glue_client = FakeGlueClient()

    with pytest.raises(RuntimeError, match="storage failed"):
        process_event(
            {
                "origin": "KIX",
                "destination": "ICN",
                "departure_date": "2026-12-12",
            },
            flight_client=FakeFlightClient(),
            storage=FailingStorage(),
            glue_client=glue_client,
        )

    assert glue_client.source_uri is None


def test_process_event_does_not_save_or_start_glue_when_fetch_fails():
    storage = FakeStorage()
    glue_client = FakeGlueClient()

    with pytest.raises(RuntimeError, match="flight fetch failed"):
        process_event(
            {
                "origin": "KIX",
                "destination": "ICN",
                "departure_date": "2026-12-12",
            },
            flight_client=FailingFlightClient(),
            storage=storage,
            glue_client=glue_client,
        )

    assert storage.saved_data is None
    assert glue_client.source_uri is None


def test_process_event_propagates_glue_failure_after_save():
    storage = FakeStorage()

    with pytest.raises(RuntimeError, match="glue failed"):
        process_event(
            {
                "origin": "KIX",
                "destination": "ICN",
                "departure_date": "2026-12-12",
            },
            flight_client=FakeFlightClient(),
            storage=storage,
            glue_client=FailingGlueClient(),
        )

    assert storage.saved_data is not None


@pytest.mark.parametrize("event", [{}, {"departure_date": "2026/12/12"}, None])
def test_process_event_rejects_invalid_event_before_dependencies(event):
    storage = FakeStorage()
    glue_client = FakeGlueClient()

    with pytest.raises(ValueError, match="event|departure_date"):
        process_event(
            event,
            flight_client=FakeFlightClient(),
            storage=storage,
            glue_client=glue_client,
        )

    assert storage.saved_data is None
    assert glue_client.source_uri is None


def test_lambda_handler_propagates_errors(monkeypatch):
    def fail(_event):
        raise RuntimeError("flight fetch failed")

    monkeypatch.setattr(handler, "process_event", fail)

    with pytest.raises(RuntimeError, match="flight fetch failed"):
        lambda_handler({}, None)
