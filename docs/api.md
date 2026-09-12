# Backend API

BackendはJava / Spring Bootで実装し、ECS Fargate上で実行します。AWS SDK for Javaを使用してAthenaへQueryを発行し、結果をFrontend向けのJSONへ変換します。

```text
Spring Boot
    ↓ StartQueryExecution
Athena QueryExecutionId
    ↓ GetQueryExecution
SUCCEEDED
    ↓ GetQueryResults
JSON Response
```

BackendはRequest Validation、Athena Queryの生成・実行、結果の整形、Error Handlingを担当します。Frontendから任意SQLを送信する機能は提供しません。

## Endpoints

### Routes

```http
GET /api/routes
```

利用可能な路線を返します。

### Latest Price

```http
GET /api/prices/latest?origin=KIX&destination=ICN&departureDate=2026-10-11
```

### Price History

```http
GET /api/prices/history?origin=KIX&destination=ICN&departureDate=2026-10-11
```

### Price Summary

```http
GET /api/analytics/summary?origin=KIX&destination=ICN&departureDate=2026-10-11
```

```json
{
  "latestPrice": 252,
  "lowestPrice": 252,
  "averagePrice": 314.8,
  "currency": "USD"
}
```

### Airline Analytics

```http
GET /api/analytics/airlines?origin=KIX&destination=ICN
```

```json
[
  {
    "carrier": "Air Seoul",
    "averagePrice": 281.5
  },
  {
    "carrier": "Jeju Air",
    "averagePrice": 316.0
  }
]
```

### Purchase Timing Analytics

```http
GET /api/analytics/purchase-timing?origin=KIX&destination=ICN
```

出発までの日数と平均価格の関係を返します。

## Frontend

Frontendは路線と出発日を指定し、各APIの結果から次の情報を表示します。

- 最新・最安・平均価格
- 価格履歴グラフ
- 航空会社別価格
- 出発までの日数別価格
- 所要時間、乗継回数、使用機材、手荷物情報
