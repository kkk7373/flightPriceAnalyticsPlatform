import json
from pathlib import Path


class MockFlightClient:
    def __init__(self, fixture_path: Path | None = None):
        self.fixture_path = fixture_path or (
            Path(__file__).parent / "fixtures" / "flight_data.json"
        )

    def search_flights(self, event: dict) -> dict:
        with self.fixture_path.open("r", encoding="utf-8") as file:
            return json.load(file)
