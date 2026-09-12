FROM public.ecr.aws/sam/build-python3.13:latest AS development

WORKDIR /workspace/lambdas/flight-fetcher

ENV FLIGHT_CLIENT=mock
ENV STORAGE_TYPE=mock
ENV MOCK_STORAGE_DIR=/workspace/lambdas/flight-fetcher/.local/raw/flight-offers
ENV GLUE_CLIENT=mock
ENV PYTHONPATH=/workspace/lambdas/flight-fetcher:/workspace/lambdas/flight-fetcher/src

COPY requirements.txt requirements-dev.txt ./

RUN python3 -m pip install --no-cache-dir -r requirements-dev.txt

ENTRYPOINT []
CMD ["/bin/bash"]

FROM development AS test

COPY src ./src
COPY scripts ./scripts
COPY tests ./tests

CMD ["pytest", "-q", "-p", "no:cacheprovider"]
