import json
from datetime import datetime, timezone
from pathlib import Path

from storage.raw_data import build_partition_key, build_raw_record


class MockStorage:
    def __init__(self, directory: str | Path, clock=None):
        self.directory = Path(directory)
        self.clock = clock or (lambda: datetime.now(timezone.utc))

    def save(self, flight_info: dict) -> str:
        searched_at = self.clock()
        file_path = self.directory / build_partition_key(flight_info, searched_at)
        raw_data = build_raw_record(flight_info, searched_at)

        file_path.parent.mkdir(parents=True, exist_ok=True)
        with file_path.open("x", encoding="utf-8") as file:
            json.dump(raw_data, file, ensure_ascii=False, indent=2)

        return file_path.resolve().as_uri()
