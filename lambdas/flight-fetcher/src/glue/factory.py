import os

from glue.mock_client import MockGlueClient


def create_glue_client(aws_glue_client=None):
    client_type = os.getenv("GLUE_CLIENT")

    if client_type == "mock":
        return MockGlueClient()

    if client_type == "aws":
        from glue.client import GlueClient

        job_name = os.getenv("GLUE_JOB_NAME")
        if not job_name:
            raise ValueError("GLUE_JOB_NAME is required")

        return GlueClient(job_name=job_name, glue_client=aws_glue_client)

    raise ValueError("GLUE_CLIENT must be either 'mock' or 'aws'")
