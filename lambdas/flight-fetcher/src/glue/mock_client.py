class MockGlueClient:
    def start_job(self, source_uri: str) -> str:
        return "mock-glue-job-run"
