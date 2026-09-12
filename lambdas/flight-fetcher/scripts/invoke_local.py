import argparse
import json

from handler import lambda_handler


def main(argv=None) -> None:
    parser = argparse.ArgumentParser(description="Invoke the Lambda handler locally")
    parser.add_argument("departure_date", help="Departure date in YYYY-MM-DD format")
    args = parser.parse_args(argv)

    result = lambda_handler({"departure_date": args.departure_date}, None)
    summary = {
        "flight_count": len(result["flight_info"].get("itineraries", [])),
        "storage_uri": result["storage_uri"],
        "glue_job_run_id": result["glue_job_run_id"],
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
