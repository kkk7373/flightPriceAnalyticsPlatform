# Backend API

BackendはJava / Spring Bootで実装します。現在は`GET /api/init`を実装済みです。AWS SDK for Javaを使用してAthenaへQueryを発行し、結果をJSONへ変換します。ECS Fargateへの配置は今後の構成です。

```text
Spring Boot
    ↓ StartQueryExecution
Athena QueryExecutionId
    ↓ GetQueryExecution
SUCCEEDED
    ↓ GetQueryResults
JSON Response
```

現在の`GET /api/init`はリクエストパラメータを受け取らず、Backend内の固定SQLを実行します。同じ日本時間の基準日に対する結果はアプリ内でキャッシュし、有効なキャッシュがあればAthenaクエリを実行しません。Athena Queryの失敗やタイムアウトは例外として検知しますが、専用のHTTPエラーレスポンス形式はまだ定義していません。Frontendから任意SQLを送信する機能は提供しません。

## Endpoints

### Initial

```http
GET /api/init
```

KIX → ICNのダッシュボード初期表示に必要な、当日の概況と過去30日間の集計を返します。ここでの日付は収集日（`search_date`）ではなく出発日（`departure_date`）です。固定の説明文はFrontendで管理し、このAPIには含めません。リクエストパラメータはありません。正常時はHTTP `200`で返します。

```json
{
  "route": { "origin": "KIX", "destination": "ICN" },
  "period": {
    "from": "2026-08-23",
    "to": "2026-09-21",
    "timezone": "Asia/Tokyo"
  },
  "currency": "JPY",
  "lastUpdatedAt": "2026-09-21T03:00:00Z",
  "today": {
    "departureDate": "2026-09-21",
    "offerCount": 42,
    "averagePrice": 31050.00,
    "lowestPrice": 22000.00,
    "highestPrice": 48000.00
  },
  "dailyPrices": [
    { "date": "2026-08-23", "averagePrice": 30520.00, "offerCount": 38 },
    { "date": "2026-08-24", "averagePrice": null, "offerCount": 0 }
  ],
  "airlineRanking": [
    {
      "carrierCode": "RS",
      "carrierName": "エアソウル",
      "averagePrice": 28000.00,
      "offerCount": 15
    }
  ],
  "departureTimeBands": [
    { "timeBand": "earlyMorning", "averagePrice": 29000.00, "offerCount": 12 },
    { "timeBand": "morning", "averagePrice": 29500.00, "offerCount": 27 },
    { "timeBand": "afternoon", "averagePrice": 32000.00, "offerCount": 31 },
    { "timeBand": "evening", "averagePrice": null, "offerCount": 0 }
  ]
}
```

上記の`dailyPrices`は形式を示すため2日分だけ記載しています。実際のレスポンスでは、`period.from`から`period.to`までの30日分を日付昇順で返します。

| Field | Meaning |
| --- | --- |
| `period` | 日本時間の当日を含む過去30暦日。`from`と`to`は両端を含む |
| `currency` | 現在はBackendが固定で返す`JPY`。集計SQLでは通貨を絞り込まないため、対象テーブルの価格がJPYのみであることを前提とする |
| `lastUpdatedAt` | Backendがこのレスポンスを生成した日時（UTC）。キャッシュから返す場合も生成時刻のままであり、データの収集日時ではない |
| `today` | 日本時間の当日を`departure_date`とするデータの概況 |
| `dailyPrices` | `departure_date`ごとの平均価格。折れ線グラフの横軸は`date`、縦軸は`averagePrice` |
| `airlineRanking` | 過去30日間の航空会社別平均価格。安い順、同額なら`carrierCode`順 |
| `departureTimeBands` | 過去30日間の出発時間帯別平均価格。安かった時間帯の比較に使用 |

#### 集計ルール

- 集計SQLは`departure_date`、Economy、`price_status = 'verified'`、`price > 0`で絞り込みます。`origin`・`destination`・`currency`はSQLでは絞り込まず、対象テーブルにはKIX → ICNかつJPYの価格のみが入ることを前提とします。為替換算は行いません。
- `offerCount`は条件に一致した価格オファーの行数であり、実際の運航便数ではありません。画面でも「取得オファー数」などと表示し、「便数」とは表記しません。
- 対象テーブルに重複オファーがないことを運用上の前提とします。現在のSQLは`external_offer_id`による重複除外や、最新の`searched_at`だけを選ぶ処理は行いません。
- `today`には当日出発分を使用します。当日分が未収集または対象価格がない場合、`offerCount`は`0`、価格項目は`null`とします。前日のデータを当日分として表示しません。
- `dailyPrices`はデータがない日も含めて30日分を返し、その日は`averagePrice: null`、`offerCount: 0`とします。欠損日を価格0としてグラフに描画しません。
- `airlineRanking`は`carrier_code`・`carrier_name`があるオファーを集計します。航空会社ごとの件数も返し、少数のオファーによる平均値だと分かるようにします。
- `departureTimeBands`は`departure_at`を出発地の現地時刻へ変換して分類します。`earlyMorning`は00:00〜05:59、`morning`は06:00〜11:59、`afternoon`は12:00〜17:59、`evening`は18:00〜23:59です。出発日時・Timezoneが不明なオファーは時間帯集計から除外します。4区分を時刻順に返し、該当データがない区分の平均価格は`null`とします。
- 時間帯の比較は観測された平均価格であり、将来その時間帯の価格が下がるという予測ではありません。画面には「過去30日で平均価格が安かった出発時間帯」と表示します。
- 対象期間にデータがまったくない場合もHTTP `200`を返します。`today`と`dailyPrices`は上記の空データ形式、`airlineRanking`は空配列、時間帯は4区分の空データ形式とします。`lastUpdatedAt`にはレスポンス生成日時を返します。

#### キャッシュ

- キャッシュキーはAthenaクエリの基準日である日本時間の当日です。現在の路線・通貨・SQL条件は固定で、30日間の開始日も基準日から決まります。
- 4クエリをまとめた`InitResponse`全体をSpring Bootプロセス内のCaffeineに保存します。有効期限は生成から24時間、最大32件です。同じ基準日への同時アクセスは1回の生成にまとめます。
- 日付が変わるとキャッシュキーも変わるため、翌日の最初のアクセスではAthenaを再実行します。前日のエントリは生成から24時間後に期限切れになります。クエリ失敗時はキャッシュを更新しません。対象データが0件の正常なレスポンスはキャッシュ対象です。
- プロセス再起動でキャッシュは消え、複数コンテナでは各コンテナが独立して保持します。Glue完了時の即時破棄は未実装のため、当日の初回アクセス後にデータが追加・更新されても、その日のレスポンスには反映されません。

## 今後の候補（未実装）

以下は将来のAPI案です。現在のBackendの契約・動作には含まれません。

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

## Frontend（未実装）

将来のFrontendでは、初期画面に`GET /api/init`の結果を表示します。以下は追加機能の案です。

- 最新・最安・平均価格
- 価格履歴グラフ
- 航空会社別価格
- 出発までの日数別価格
- 所要時間、乗継回数、使用機材、手荷物情報
