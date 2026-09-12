class GlueClient:
    SOURCE_S3_URI_ARGUMENT = "--SOURCE_S3_URI"

    def __init__(self, job_name: str, glue_client=None):
        if not job_name:
            raise ValueError("job_name is required")

        if glue_client is None:
            import boto3

            glue_client = boto3.client("glue")

        self.job_name = job_name
        self.glue_client = glue_client

    def start_job(self, source_s3_uri: str) -> str:
        if not source_s3_uri.startswith("s3://"):
            raise ValueError("source_s3_uri must be an S3 URI")

        response = self.glue_client.start_job_run(
            JobName=self.job_name,
            Arguments={self.SOURCE_S3_URI_ARGUMENT: source_s3_uri},
        )

        job_run_id = response.get("JobRunId")
        if not job_run_id:
            raise RuntimeError("Glue start_job_run response did not contain JobRunId")

        return job_run_id
