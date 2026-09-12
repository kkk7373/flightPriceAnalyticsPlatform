from datetime import datetime


def build_raw_record(flight_info: dict, searched_at: datetime) -> dict:
    return {
        "schema_version": 1,
        "searched_at": searched_at.isoformat(),
        "response": flight_info,
    }


def build_partition_key(flight_info: dict, searched_at: datetime) -> str:
    origin = _iata_code(flight_info.get("origin"), "origin")
    destination = _iata_code(flight_info.get("destination"), "destination")
    collected_date = searched_at.date().isoformat()
    timestamp = searched_at.strftime("%Y%m%dT%H%M%S%fZ")

    return (
        f"collected_date={collected_date}/"
        f"route={origin}-{destination}/"
        f"{timestamp}.json"
    )


def _iata_code(value, field_name: str) -> str:
    if not isinstance(value, str) or len(value) != 3 or not value.isalpha():
        raise ValueError(f"flight_info.{field_name} must be a 3-letter IATA code")
    return value.upper()
