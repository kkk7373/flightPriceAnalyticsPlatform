import os

from flight.mock_client import MockFlightClient


def create_flight_client():
    client_type = os.getenv("FLIGHT_CLIENT")

    if client_type == "mock":
        return MockFlightClient()

    if client_type == "ignav":
        from config import Config
        from flight.ignav_client import IgnavClient

        api_key = os.getenv("IGNAV_API_KEY")

        if not api_key:
            raise ValueError("IGNAV_API_KEY is required")

        config = Config.from_env()
        return IgnavClient(api_key=api_key, config=config)

    raise ValueError("FLIGHT_CLIENT must be either 'mock' or 'ignav'")
