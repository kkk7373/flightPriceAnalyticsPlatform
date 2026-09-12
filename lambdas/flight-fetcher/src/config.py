import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Config:
    origin: str
    destination: str
    market: str
    trip_type: str
    request_timeout_seconds: float

    @classmethod
    def from_env(cls):
        timeout = float(os.getenv("REQUEST_TIMEOUT_SECONDS", "30"))
        if timeout <= 0:
            raise ValueError("REQUEST_TIMEOUT_SECONDS must be greater than 0")

        return cls(
            origin=os.getenv("ORIGIN", "KIX"),
            destination=os.getenv("DESTINATION", "ICN"),
            market=os.getenv("MARKET", "JP"),
            trip_type=os.getenv("TRIP_TYPE", "one-way"),
            request_timeout_seconds=timeout,
        )
