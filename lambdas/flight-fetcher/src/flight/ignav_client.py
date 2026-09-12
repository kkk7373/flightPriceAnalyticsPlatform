from datetime import date

import requests

from config import Config


class IgnavClient:
    ENDPOINTS = {
        "one-way": "https://ignav.com/api/fares/one-way",
    }

    def __init__(
        self,
        api_key: str,
        config: Config,
        session=None,
    ):
        if config.trip_type not in self.ENDPOINTS:
            raise ValueError(f"Unsupported TRIP_TYPE: {config.trip_type}")

        self.api_key = api_key
        self.config = config
        self.base_url = self.ENDPOINTS[config.trip_type]
        self.session = session or requests.Session()

    def search_flights(self, event: dict) -> dict:
        departure_date = event.get("departure_date")
        if not departure_date:
            raise ValueError("departure_date is required")

        try:
            date.fromisoformat(departure_date)
        except (TypeError, ValueError) as error:
            raise ValueError("departure_date must use YYYY-MM-DD format") from error

        payload = {
            "origin": self._validate_code(
                event.get("origin") or self.config.origin,
                "origin",
                3,
            ),
            "destination": self._validate_code(
                event.get("destination") or self.config.destination,
                "destination",
                3,
            ),
            "departure_date": departure_date,
            "market": self._validate_code(
                event.get("market") or self.config.market,
                "market",
                2,
            ),
        }

        departure_time_range = event.get("departure_time_range")
        if departure_time_range is not None:
            self._validate_departure_time_range(departure_time_range)
            payload["departure_time_range"] = departure_time_range

        response = self.session.post(
            self.base_url,
            headers={"X-Api-Key": self.api_key},
            json=payload,
            timeout=self.config.request_timeout_seconds,
        )
        response.raise_for_status()

        try:
            response_data = response.json()
        except requests.exceptions.JSONDecodeError as error:
            raise ValueError("Ignav API returned invalid JSON") from error

        if not isinstance(response_data, dict):
            raise ValueError("Ignav API response must be a JSON object")

        return response_data

    @staticmethod
    def _validate_departure_time_range(time_range):
        if not isinstance(time_range, dict):
            raise ValueError("departure_time_range must be an object")

        allowed_fields = {
            "earliest_hour",
            "latest_hour",
            "arrival_earliest_hour",
            "arrival_latest_hour",
        }
        unknown_fields = set(time_range) - allowed_fields
        if unknown_fields:
            raise ValueError("departure_time_range contains unsupported fields")

        for field, value in time_range.items():
            if isinstance(value, bool) or not isinstance(value, int) or not 0 <= value <= 23:
                raise ValueError(f"{field} must be an integer between 0 and 23")

        hour_pairs = (
            ("earliest_hour", "latest_hour"),
            ("arrival_earliest_hour", "arrival_latest_hour"),
        )
        for earliest, latest in hour_pairs:
            if (
                earliest in time_range
                and latest in time_range
                and time_range[earliest] > time_range[latest]
            ):
                raise ValueError(f"{earliest} must not be later than {latest}")

    @staticmethod
    def _validate_code(value, field_name: str, length: int) -> str:
        if not isinstance(value, str) or len(value) != length or not value.isalpha():
            raise ValueError(f"{field_name} must be a {length}-letter code")
        return value.upper()
