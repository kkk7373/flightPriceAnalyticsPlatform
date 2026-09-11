# Data Model and Analytics

## Raw Data

Ignav Flight APIの1検索結果には複数の`itineraries`が含まれます。

```json
{
  "origin": "NRT",
  "destination": "ICN",
  "departure_date": "2026-10-11",
  "searched_at": "2026-09-11T12:00:00Z",
  "itineraries": [
    {
      "price": {
        "amount": 252,
        "currency": "USD",
        "status": "verified"
      },
      "outbound": {
        "carrier": "Air Seoul",
        "duration_minutes": 160,
        "segments": [
          {
            "marketing_carrier_code": "RS",
            "flight_number": "706",
            "departure_airport": "NRT",
            "departure_time_utc": "2026-10-11T02:40:00Z",
            "arrival_airport": "ICN",
            "arrival_time_utc": "2026-10-11T05:20:00Z",
            "aircraft": "Airbus A321"
          }
        ]
      },
      "cabin_class": "economy",
      "requires_self_transfer": false,
      "ignav_id": "caab461964f60000003ffa188783e3a0"
    }
  ]
}
```

## Processed Table

Glue Data CatalogではDatabaseを`flight_analytics`、Tableを`flight_prices`とします。

| Column | Type | Description |
| --- | --- | --- |
| search_date | DATE | 航空券情報を取得した日 |
| searched_at | TIMESTAMP | APIからデータを取得した日時 |
| departure_date | DATE | 出発日 |
| origin | STRING | 出発空港IATAコード |
| destination | STRING | 到着空港IATAコード |
| carrier_name | STRING | 主な航空会社名 |
| carrier_code | STRING | 航空会社IATAコード |
| flight_number | STRING | 便名 |
| departure_at | TIMESTAMP | 出発日時（UTC） |
| arrival_at | TIMESTAMP | 到着日時（UTC） |
| departure_timezone | STRING | 出発地Timezone |
| arrival_timezone | STRING | 到着地Timezone |
| duration_minutes | INTEGER | 総所要時間 |
| stops | INTEGER | 乗継回数 |
| aircraft | STRING | 使用機材 |
| cabin_class | STRING | Cabin Class |
| price | DECIMAL | 航空券価格 |
| currency | STRING | 通貨 |
| price_status | STRING | `verified` / `unverified` |
| carry_on_bags | INTEGER | 機内持込手荷物数 |
| checked_bags | INTEGER | 預入手荷物数 |
| requires_self_transfer | BOOLEAN | Self Transfer有無 |
| external_offer_id | STRING | Ignav Offer ID |

## Price Status

APIには検証済み・未検証の価格が含まれるため、Processedデータでは`price_status`を必ず保持します。最安価格や平均価格に未検証データが影響しないよう、価格分析は原則として次の条件を適用します。

```sql
WHERE price_status = 'verified'
```

## Partition

Athenaのスキャン量を抑えるため、Processedデータを検索年月で分割します。

```text
processed/flight-prices/
└── search_year=2026/
    └── search_month=09/
        └── *.parquet
```

## Analytics

Athenaでは以下を集計します。

- 指定路線・出発日の最新価格
- 同じ出発日に対する検索日ごとの価格履歴
- 指定条件における最安価格と平均価格
- 航空会社ごとの平均価格
- 出発までの日数と平均価格の関係
