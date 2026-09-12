## Lambdaのハンドラー関数

import logging
from datetime import date


logger = logging.getLogger(__name__)


def lambda_handler(event, context):
    """
    Lambdaのエントリーポイントとなるハンドラー関数です。

    Args:
        event (dict): Lambda関数に渡されるイベントデータ。
        context (object): Lambda関数の実行コンテキスト情報。
    Returns:
        dict: 処理結果を含む辞書。
    """
    try:
        return process_event(event)
    except Exception:
        logger.exception("Flight price collection failed")
        raise


def process_event(
    event: dict,
    flight_client=None,
    storage=None,
    glue_client=None,
) -> dict:
    """
    イベントデータを処理するための関数です。

    Args:
        event (dict): Lambda関数に渡されるイベントデータ。
    Returns:
        dict: 処理結果を含む辞書。
    """
    validate_event(event)
    flight_info = get_flight_info(event, flight_client)
    storage_uri = save_flight_info(flight_info, storage)
    glue_job_run_id = start_glue_job(storage_uri, glue_client)

    return {
        "flight_info": flight_info,
        "storage_uri": storage_uri,
        "glue_job_run_id": glue_job_run_id,
    }


def validate_event(event: dict) -> None:
    if not isinstance(event, dict):
        raise ValueError("event must be an object")

    departure_date = event.get("departure_date")
    if not departure_date:
        raise ValueError("departure_date is required")

    try:
        date.fromisoformat(departure_date)
    except (TypeError, ValueError) as error:
        raise ValueError("departure_date must use YYYY-MM-DD format") from error


def get_flight_info(event: dict, flight_client=None) -> dict:
    """
    フライト情報を取得するための関数です。

    Args:
        event (dict): フライト情報を取得するためのイベントデータ。
    Returns:
        dict: フライト情報を含む辞書。
    """
    if flight_client is None:
        from flight.factory import create_flight_client

        flight_client = create_flight_client()

    return flight_client.search_flights(event)


def save_flight_info(flight_info: dict, storage=None) -> str:
    """
    フライト情報を保存するための関数です。

    Args:
        flight_info (dict): 保存するフライト情報。
    Returns:
        str: 保存先のURI。
    """
    if storage is None:
        from storage.factory import create_storage

        storage = create_storage()

    return storage.save(flight_info)


def start_glue_job(storage_uri: str, glue_client=None) -> str:
    """保存済みのRawデータを入力としてGlue Jobを起動する。"""
    if glue_client is None:
        from glue.factory import create_glue_client

        glue_client = create_glue_client()

    return glue_client.start_job(storage_uri)
