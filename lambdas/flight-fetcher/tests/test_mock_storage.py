import json
from datetime import datetime, timezone

from storage.mock_storage import MockStorage


def test_save_writes_flight_info_as_json(tmp_path):
    flight_info = {
        "origin": "KIX",
        "destination": "ICN",
        "departure_date": "2026-12-12",
    }
    searched_at = datetime(2026, 9, 12, 3, 0, tzinfo=timezone.utc)

    storage_uri = MockStorage(tmp_path, clock=lambda: searched_at).save(flight_info)

    saved_files = list(tmp_path.rglob("*.json"))
    assert len(saved_files) == 1
    assert saved_files[0].relative_to(tmp_path).as_posix() == (
        "collected_date=2026-09-12/route=KIX-ICN/"
        "20260912T030000000000Z.json"
    )
    saved_data = json.loads(saved_files[0].read_text(encoding="utf-8"))
    assert saved_data["schema_version"] == 1
    assert saved_data["searched_at"] == "2026-09-12T03:00:00+00:00"
    assert saved_data["response"] == flight_info
    assert storage_uri == saved_files[0].resolve().as_uri()
