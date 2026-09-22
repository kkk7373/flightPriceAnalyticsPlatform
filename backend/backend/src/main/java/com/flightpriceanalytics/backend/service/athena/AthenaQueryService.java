package com.flightpriceanalytics.backend.service.athena;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.ColumnInfo;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.paginators.GetQueryResultsIterable;

@Service 
public class AthenaQueryService {

    private final AthenaClient athenaClient;
    private final AthenaProperties properties;
    
    public AthenaQueryService(AthenaClient athenaClient, AthenaProperties properties) {
        this.athenaClient = athenaClient;
        this.properties = properties;
    }

    public <T> List<T> athenaExecute(String sql, List<String> parameters, Function<Map<String, String>, T> mapper)throws InterruptedException{
        String queryExecutionId = submitQuery(sql,parameters);
        waitForQueryToComplete(queryExecutionId);
        return processResultRows(queryExecutionId).stream()
                .map(mapper)
                .toList();
    }
    private String submitQuery(String sql, List<String> parameters) {
    StartQueryExecutionRequest request = StartQueryExecutionRequest.builder()
            .queryString(sql)
            .executionParameters(parameters)
            .queryExecutionContext(QueryExecutionContext.builder()
                    .database(properties.database())
                    .build())
            .resultConfiguration(ResultConfiguration.builder()
                    .outputLocation(properties.outputLocation())
                    .build())
            .workGroup(properties.workgroup())
            .build();

    return athenaClient.startQueryExecution(request).queryExecutionId();
}
    

    // Wait for an Amazon Athena query to complete, fail or to be cancelled.
    private void waitForQueryToComplete(String queryExecutionId)
            throws InterruptedException {
        GetQueryExecutionRequest getQueryExecutionRequest = GetQueryExecutionRequest.builder()
                .queryExecutionId(queryExecutionId)
                .build();

        GetQueryExecutionResponse getQueryExecutionResponse;
        boolean isQueryStillRunning = true;
        long startedAt = System.nanoTime();

        while (isQueryStillRunning) {
            getQueryExecutionResponse = athenaClient.getQueryExecution(getQueryExecutionRequest);
            String queryState = getQueryExecutionResponse.queryExecution().status().state().toString();
            if (queryState.equals(QueryExecutionState.FAILED.toString())) {
                throw new RuntimeException(
                        "The Amazon Athena query failed to run with error message: " + getQueryExecutionResponse
                                .queryExecution().status().stateChangeReason());
            } else if (queryState.equals(QueryExecutionState.CANCELLED.toString())) {
                throw new RuntimeException("The Amazon Athena query was cancelled.");
            } else if (queryState.equals(QueryExecutionState.SUCCEEDED.toString())) {
                isQueryStillRunning = false;
            } else if(Duration.ofNanos(System.nanoTime() - startedAt).compareTo(properties.queryTimeout())>=0){
                throw new IllegalStateException("Athena query timed out");

            }else {
                // Sleep an amount of time before retrying again.
                Thread.sleep(properties.sleep());
            }
        }
    }

    // Collect all data rows; the first row of a SELECT result contains column names.
    private List<Map<String, String>> processResultRows(String queryExecutionId) {
        GetQueryResultsRequest request = GetQueryResultsRequest.builder()
                .queryExecutionId(queryExecutionId)
                .build();

        List<Map<String, String>> rows = new ArrayList<>();
        boolean skipHeader = true;
        GetQueryResultsIterable pages = athenaClient.getQueryResultsPaginator(request);
        for (GetQueryResultsResponse page : pages) {
            List<Row> pageRows = page.resultSet().rows();
            if (skipHeader && !pageRows.isEmpty()) {
                pageRows = pageRows.subList(1, pageRows.size());
                skipHeader = false;
            }
            List<ColumnInfo> columns = page.resultSet().resultSetMetadata().columnInfo();
            rows.addAll(processRow(pageRows, columns));
        }
        return rows;
    }

    private static List<Map<String,String>> processRow(List<Row> rows, List<ColumnInfo> columns) {
        List<Map<String,String>> results = new ArrayList<>();
        for (Row row : rows) {
            Map<String, String> result = new LinkedHashMap<>();
            List<Datum>  values = row.data();

            for (int i = 0; i < columns.size(); i++) {
                String columnName = columns.get(i).name();
                String value = i < values.size()
                ? values.get(i).varCharValue()
                : null;

                result.put(columnName, value);
            }
            results.add(result);
        }
        return results;
    }


}
