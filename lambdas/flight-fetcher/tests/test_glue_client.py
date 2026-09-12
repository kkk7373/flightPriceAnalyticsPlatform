import pytest

from glue.client import GlueClient


class FakeBotoGlueClient:
    def __init__(self, response=None):
        self.response = response or {"JobRunId": "jr_123"}
        self.request = None

    def start_job_run(self, **kwargs):
        self.request = kwargs
        return self.response


def test_start_job_passes_source_s3_uri_and_returns_job_run_id():
    boto_client = FakeBotoGlueClient()
    client = GlueClient("transform-flight-prices", boto_client)

    job_run_id = client.start_job("s3://raw-bucket/raw/data.json")

    assert job_run_id == "jr_123"
    assert boto_client.request == {
        "JobName": "transform-flight-prices",
        "Arguments": {
            "--SOURCE_S3_URI": "s3://raw-bucket/raw/data.json",
        },
    }


def test_start_job_rejects_non_s3_uri():
    client = GlueClient("transform-flight-prices", FakeBotoGlueClient())

    with pytest.raises(ValueError, match="S3 URI"):
        client.start_job("file:///tmp/data.json")


def test_start_job_requires_job_run_id_in_response():
    client = GlueClient(
        "transform-flight-prices",
        FakeBotoGlueClient(response={"ResponseMetadata": {}}),
    )

    with pytest.raises(RuntimeError, match="JobRunId"):
        client.start_job("s3://raw-bucket/raw/data.json")
