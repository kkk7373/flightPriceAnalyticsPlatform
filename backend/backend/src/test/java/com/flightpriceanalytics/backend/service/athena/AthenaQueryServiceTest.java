package com.flightpriceanalytics.backend.service.athena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.AthenaException;
import software.amazon.awssdk.services.athena.model.ColumnInfo;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecution;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus;
import software.amazon.awssdk.services.athena.model.ResultSet;
import software.amazon.awssdk.services.athena.model.ResultSetMetadata;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;
import software.amazon.awssdk.services.athena.paginators.GetQueryResultsIterable;

class AthenaQueryServiceTest {

    private record Price(String date, String amount) {
    }

    @Test
    void mapsDataRowsAcrossPagesWithoutIncludingHeader() throws InterruptedException {
        AthenaClient client = mock(AthenaClient.class);
        AthenaProperties properties = new AthenaProperties(
                "ap-northeast-1", "flight_analytics", "primary",
                "s3://example-results/", Duration.ofSeconds(30), 1000);
        AthenaQueryService service = new AthenaQueryService(client, properties);

        when(client.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(StartQueryExecutionResponse.builder().queryExecutionId("query-1").build());
        when(client.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(GetQueryExecutionResponse.builder()
                        .queryExecution(QueryExecution.builder()
                                .status(QueryExecutionStatus.builder()
                                        .state(QueryExecutionState.SUCCEEDED).build())
                                .build())
                        .build());

        List<ColumnInfo> columns = List.of(
                ColumnInfo.builder().name("date").type("varchar").build(),
                ColumnInfo.builder().name("amount").type("varchar").build());
        GetQueryResultsResponse firstPage = page(columns,
                row("date", "amount"), row("2026-09-21", "12000"))
                .toBuilder().nextToken("page-2").build();
        GetQueryResultsResponse secondPage = page(columns,
                row("2026-09-22", null));
        when(client.getQueryResultsPaginator(any(GetQueryResultsRequest.class)))
                .thenAnswer(invocation -> new GetQueryResultsIterable(client, invocation.getArgument(0)));
        when(client.getQueryResults(any(GetQueryResultsRequest.class)))
                .thenReturn(firstPage, secondPage);

        String sql = "SELECT date, amount FROM prices WHERE departure_date = ?";
        List<String> parameters = List.of("DATE '2026-09-22'");
        List<Price> result = service.athenaExecute(sql, parameters,
                values -> new Price(values.get("date"), values.get("amount")));

        assertEquals(2, result.size());
        assertEquals(new Price("2026-09-21", "12000"), result.get(0));
        assertEquals("2026-09-22", result.get(1).date());
        assertNull(result.get(1).amount());

        ArgumentCaptor<StartQueryExecutionRequest> request =
                ArgumentCaptor.forClass(StartQueryExecutionRequest.class);
        verify(client).startQueryExecution(request.capture());
        assertEquals(sql, request.getValue().queryString());
        assertEquals(parameters, request.getValue().executionParameters());
        assertEquals("flight_analytics", request.getValue().queryExecutionContext().database());
        assertEquals("primary", request.getValue().workGroup());
        assertEquals("s3://example-results/", request.getValue().resultConfiguration().outputLocation());
    }

    @ParameterizedTest
    @EnumSource(value = QueryExecutionState.class, names = { "FAILED", "CANCELLED" })
    void doesNotFetchResultsWhenQueryFailsOrIsCancelled(QueryExecutionState state) {
        AthenaClient client = mock(AthenaClient.class);
        AthenaQueryService service = service(client, Duration.ofSeconds(30));
        when(client.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(StartQueryExecutionResponse.builder().queryExecutionId("query-2").build());
        when(client.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(execution(state));

        assertThrows(RuntimeException.class,
                () -> service.athenaExecute("SELECT 1", List.of(), values -> values));
        verify(client, never()).getQueryResultsPaginator(any(GetQueryResultsRequest.class));
    }

    @Test
    void timesOutWhileQueryIsStillRunning() {
        AthenaClient client = mock(AthenaClient.class);
        AthenaQueryService service = service(client, Duration.ZERO);
        when(client.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(StartQueryExecutionResponse.builder().queryExecutionId("query-3").build());
        when(client.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(execution(QueryExecutionState.RUNNING));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.athenaExecute("SELECT 1", List.of(), values -> values));
        assertEquals("Athena query timed out", error.getMessage());
        verify(client, never()).getQueryResultsPaginator(any(GetQueryResultsRequest.class));
    }

    @Test
    void pollsAgainUntilQuerySucceeds() throws InterruptedException {
        AthenaClient client = mock(AthenaClient.class);
        AthenaQueryService service = new AthenaQueryService(client, new AthenaProperties(
                "ap-northeast-1", "flight_analytics", "primary",
                "s3://example-results/", Duration.ofSeconds(30), 0));
        when(client.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(StartQueryExecutionResponse.builder().queryExecutionId("query-5").build());
        when(client.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(execution(QueryExecutionState.RUNNING),
                        execution(QueryExecutionState.SUCCEEDED));
        List<ColumnInfo> columns = List.of(ColumnInfo.builder().name("date").type("varchar").build());
        when(client.getQueryResultsPaginator(any(GetQueryResultsRequest.class)))
                .thenAnswer(invocation -> new GetQueryResultsIterable(client, invocation.getArgument(0)));
        when(client.getQueryResults(any(GetQueryResultsRequest.class)))
                .thenReturn(page(columns, row("date"), row("2026-09-22")));

        List<String> result = service.athenaExecute("SELECT date FROM prices", List.of(),
                values -> values.get("date"));

        assertEquals(List.of("2026-09-22"), result);
        verify(client, times(2)).getQueryExecution(any(GetQueryExecutionRequest.class));
    }

    @Test
    void returnsEmptyListForHeaderOnlyResult() throws InterruptedException {
        AthenaClient client = mock(AthenaClient.class);
        AthenaQueryService service = service(client, Duration.ofSeconds(30));
        when(client.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(StartQueryExecutionResponse.builder().queryExecutionId("query-4").build());
        when(client.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(execution(QueryExecutionState.SUCCEEDED));
        List<ColumnInfo> columns = List.of(ColumnInfo.builder().name("date").type("varchar").build());
        when(client.getQueryResultsPaginator(any(GetQueryResultsRequest.class)))
                .thenAnswer(invocation -> new GetQueryResultsIterable(client, invocation.getArgument(0)));
        when(client.getQueryResults(any(GetQueryResultsRequest.class)))
                .thenReturn(page(columns, row("date")));

        List<String> result = service.athenaExecute("SELECT date FROM prices", List.of(),
                values -> values.get("date"));

        assertTrue(result.isEmpty());
    }

    @Test
    void propagatesAthenaStartFailure() {
        AthenaClient client = mock(AthenaClient.class);
        AthenaQueryService service = service(client, Duration.ofSeconds(30));
        when(client.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenThrow(AthenaException.builder().message("Start failed").build());

        assertThrows(AthenaException.class,
                () -> service.athenaExecute("SELECT 1", List.of(), values -> values));
    }

    private static AthenaQueryService service(AthenaClient client, Duration timeout) {
        return new AthenaQueryService(client, new AthenaProperties(
                "ap-northeast-1", "flight_analytics", "primary",
                "s3://example-results/", timeout, 1));
    }

    private static GetQueryExecutionResponse execution(QueryExecutionState state) {
        return GetQueryExecutionResponse.builder()
                .queryExecution(QueryExecution.builder()
                        .status(QueryExecutionStatus.builder()
                                .state(state)
                                .stateChangeReason("Query state: " + state)
                                .build())
                        .build())
                .build();
    }

    private static GetQueryResultsResponse page(List<ColumnInfo> columns, Row... rows) {
        return GetQueryResultsResponse.builder()
                .resultSet(ResultSet.builder()
                        .resultSetMetadata(ResultSetMetadata.builder().columnInfo(columns).build())
                        .rows(rows)
                        .build())
                .build();
    }

    private static Row row(String... values) {
        return Row.builder().data(java.util.Arrays.stream(values)
                .map(value -> Datum.builder().varCharValue(value).build())
                .toList()).build();
    }
}
