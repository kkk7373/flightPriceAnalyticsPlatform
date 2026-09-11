# Data Pipeline

## Overview

航空券情報を1日1回収集し、分析可能なParquetデータへ変換します。

```text
EventBridge Scheduler
        ↓
Python Lambda
        ↓
Ignav Flight API
        ↓
Amazon S3 raw
        ↓
AWS Glue ETL
        ↓
Amazon S3 processed
        ↓
Glue Data Catalog
```

## Target Data

| Item | Value |
| --- | --- |
| Route | NRT → ICN |
| Departure range | 現在から3〜6か月先 |
| Collection frequency | 1日1回 |
| Cabin class | Economy |
| Price status | `verified` |

## Flight API

航空券情報の取得にはIgnav Flight APIを使用します。主な取得項目は以下です。

- 出発・到着空港
- 出発日、出発・到着日時
- 航空会社、航空会社コード、便名
- 所要時間、使用機材、Cabin Class
- 手荷物情報、Self Transfer有無
- 価格、通貨、価格検証ステータス
- Ignav Offer ID

## Lambda

LambdaはPythonで実装し、次の処理だけを担当します。

1. Ignav Flight APIへのリクエスト
2. `searched_at`などの取得メタ情報追加
3. Raw JSONのS3保存
4. Glue ETL Jobの起動

複雑な分析やデータ整形はLambdaでは行いません。APIから正常なレスポンスを取得できない場合やS3保存に失敗した場合、Glue Jobは起動しません。

ローカルではLambda専用のDockerfileとDev Containerを使って開発・テストし、本番にはコードと依存関係をZIP形式でデプロイします。Lambda用のContainer Imageは使用しません。

## S3 Data Lake

```text
s3://flight-price-analytics-data/
├── raw/
│   └── flight-offers/
│       └── collected_date=YYYY-MM-DD/
│           └── route=NRT-ICN/
│               └── *.json
└── processed/
    └── flight-prices/
        └── search_year=YYYY/
            └── search_month=MM/
                └── *.parquet
```

`raw`にはAPIレスポンスを可能な限り元の形式で保存します。`processed`にはGlue ETLで平坦化した分析用データをParquet形式で保存します。

## Glue ETL

Glue Jobは次の処理を行います。

- JSONの解析
- `itineraries`と`segments`の展開・集約
- 必要項目の抽出と型変換
- 欠損値処理と重複排除
- 乗継回数の算出
- Parquetへの変換とS3への保存

変換ルールの要点は以下です。

```text
stops = segments.length - 1
origin = 最初のsegmentの出発空港
destination = 最後のsegmentの到着空港
```

Processedデータ作成後、Glue CrawlerでSchemaとPartition情報をData Catalogへ登録します。

PySparkスクリプトはAWS Glueの実行環境に合わせた専用のDockerfileとDev Containerを使って開発・テストします。本番ではコンテナをデプロイせず、スクリプトと依存関係をGlue Jobへ渡します。
