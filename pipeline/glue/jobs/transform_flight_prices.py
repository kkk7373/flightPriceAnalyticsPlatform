import json
import sys
from datetime import date, datetime
from decimal import Decimal, InvalidOperation
from urllib.parse import unquote


def flatten_raw_record(raw_record: dict) -> list[dict]:
    searched_at = _parse_datetime(raw_record.get("searched_at"), "searched_at")
    response = raw_record.get("response")
    if not isinstance(response, dict):
        raise ValueError("response must be an object")

    departure_date = _parse_date(response.get("departure_date"), "departure_date")
    rows = []

    for itinerary in response.get("itineraries", []):
        outbound = itinerary.get("outbound") or {}
        segments = outbound.get("segments") or []
        if not segments:
            raise ValueError("each itinerary must contain at least one outbound segment")

        first_segment = segments[0]
        last_segment = segments[-1]
        price = itinerary.get("price") or {}
        bags = itinerary.get("bags") or {}

        rows.append(
            {
                "search_date": searched_at.date(),
                "searched_at": searched_at,
                "departure_date": departure_date,
                "origin": first_segment.get("departure_airport"),
                "destination": last_segment.get("arrival_airport"),
                "carrier_name": outbound.get("carrier"),
                "carrier_code": first_segment.get("marketing_carrier_code"),
                "flight_number": first_segment.get("flight_number"),
                "departure_at": _parse_optional_datetime(
                    first_segment.get("departure_time_utc")
                ),
                "arrival_at": _parse_optional_datetime(
                    last_segment.get("arrival_time_utc")
                ),
                "departure_timezone": first_segment.get("departure_timezone"),
                "arrival_timezone": last_segment.get("arrival_timezone"),
                "duration_minutes": outbound.get("duration_minutes"),
                "stops": len(segments) - 1,
                "aircraft": first_segment.get("aircraft"),
                "cabin_class": itinerary.get("cabin_class"),
                "price": _parse_decimal(price.get("amount")),
                "currency": price.get("currency"),
                "price_status": price.get("status"),
                "carry_on_bags": bags.get("carry_on"),
                "checked_bags": bags.get("checked"),
                "requires_self_transfer": itinerary.get("requires_self_transfer"),
                "external_offer_id": itinerary.get("ignav_id"),
                "search_year": f"{searched_at.year:04d}",
                "search_month": f"{searched_at.month:02d}",
            }
        )

    return rows


def run_job(source_s3_uri: str, processed_s3_uri: str, spark) -> None:
    from pyspark.sql.types import (
        BooleanType,
        DateType,
        DecimalType,
        IntegerType,
        StringType,
        StructField,
        StructType,
        TimestampType,
    )

    spark.sparkContext.addPyFile(__file__)

    schema = StructType(
        [
            StructField("search_date", DateType(), False),
            StructField("searched_at", TimestampType(), False),
            StructField("departure_date", DateType(), False),
            StructField("origin", StringType(), True),
            StructField("destination", StringType(), True),
            StructField("carrier_name", StringType(), True),
            StructField("carrier_code", StringType(), True),
            StructField("flight_number", StringType(), True),
            StructField("departure_at", TimestampType(), True),
            StructField("arrival_at", TimestampType(), True),
            StructField("departure_timezone", StringType(), True),
            StructField("arrival_timezone", StringType(), True),
            StructField("duration_minutes", IntegerType(), True),
            StructField("stops", IntegerType(), False),
            StructField("aircraft", StringType(), True),
            StructField("cabin_class", StringType(), True),
            StructField("price", DecimalType(12, 2), True),
            StructField("currency", StringType(), True),
            StructField("price_status", StringType(), True),
            StructField("carry_on_bags", IntegerType(), True),
            StructField("checked_bags", IntegerType(), True),
            StructField("requires_self_transfer", BooleanType(), True),
            StructField("external_offer_id", StringType(), True),
            StructField("search_year", StringType(), False),
            StructField("search_month", StringType(), False),
        ]
    )

    source_uri = _normalize_source_uri(source_s3_uri)
    raw_records = spark.sparkContext.wholeTextFiles(source_uri).values().map(json.loads)
    rows = raw_records.flatMap(flatten_raw_record)
    dataframe = spark.createDataFrame(rows, schema=schema).dropDuplicates()

    (
        dataframe.write.mode("append")
        .partitionBy("search_year", "search_month")
        .parquet(processed_s3_uri.rstrip("/"))
    )


def main() -> None:
    from awsglue.context import GlueContext
    from awsglue.job import Job
    from awsglue.utils import getResolvedOptions
    from pyspark.context import SparkContext

    args = getResolvedOptions(
        sys.argv,
        ["JOB_NAME", "SOURCE_S3_URI", "PROCESSED_S3_URI"],
    )

    glue_context = GlueContext(SparkContext.getOrCreate())
    job = Job(glue_context)
    job.init(args["JOB_NAME"], args)

    run_job(
        source_s3_uri=args["SOURCE_S3_URI"],
        processed_s3_uri=args["PROCESSED_S3_URI"],
        spark=glue_context.spark_session,
    )
    job.commit()


def _parse_datetime(value, field_name: str) -> datetime:
    if not isinstance(value, str):
        raise ValueError(f"{field_name} must be an ISO-8601 timestamp")
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as error:
        raise ValueError(f"{field_name} must be an ISO-8601 timestamp") from error


def _parse_optional_datetime(value):
    if value is None:
        return None
    return _parse_datetime(value, "flight timestamp")


def _parse_date(value, field_name: str) -> date:
    if not isinstance(value, str):
        raise ValueError(f"{field_name} must use YYYY-MM-DD format")
    try:
        return date.fromisoformat(value)
    except ValueError as error:
        raise ValueError(f"{field_name} must use YYYY-MM-DD format") from error


def _parse_decimal(value):
    if value is None:
        return None
    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError, TypeError) as error:
        raise ValueError("price.amount must be numeric") from error


def _normalize_source_uri(source_uri: str) -> str:
    if source_uri.startswith("file://"):
        return unquote(source_uri)
    return source_uri


if __name__ == "__main__":
    main()
