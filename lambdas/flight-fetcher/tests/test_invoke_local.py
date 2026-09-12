import json

from scripts import invoke_local


def test_main_invokes_lambda_and_prints_summary(monkeypatch, capsys):
    received = {}

    def fake_lambda_handler(event, context):
        received["event"] = event
        received["context"] = context
        return {
            "flight_info": {"itineraries": [{}, {}]},
            "storage_uri": "file:///tmp/flight.json",
            "glue_job_run_id": "mock-job-run",
        }

    monkeypatch.setattr(invoke_local, "lambda_handler", fake_lambda_handler)

    invoke_local.main(["2026-12-12"])

    assert received == {
        "event": {"departure_date": "2026-12-12"},
        "context": None,
    }
    assert json.loads(capsys.readouterr().out) == {
        "flight_count": 2,
        "storage_uri": "file:///tmp/flight.json",
        "glue_job_run_id": "mock-job-run",
    }
