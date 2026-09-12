from typing import Protocol


class FlightClient(Protocol):
    def search_flights(self, event: dict) -> dict: ...
