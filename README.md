# Flight Price Analytics Platform

航空券価格を収集し、Athenaで集計して表示するためのプラットフォームです。現在はLambda・Glueのデータ処理と、初期ダッシュボード用のSpring Boot APIを実装しています。Frontendは未実装です。

## 現在実装している機能

- Lambdaがイベントで指定された`departure_date`の航空券を取得し、GlueがAthena用のParquetへ変換する
- `GET /api/init`が日本時間の当日出発分と、当日を含む過去30日間の出発日別平均価格・航空会社別平均価格・出発時間帯別平均価格を返す
- 同じ日本時間の基準日に対する`GET /api/init`の結果をアプリ内で当日中再利用し、翌日は新しい日付の結果を取得する
- Athenaクエリの失敗・タイムアウトを共通ハンドラで安全なエラーレスポンスに変換する
- 価格がない日も30日分に含め、平均価格を`null`、オファー件数を`0`として返す

現在のAPIはKIX → ICN、JPYのみのProcessedデータを前提とします。SQLはEconomy、検証済み価格（`price_status = 'verified'`）、`price > 0`を集計対象とします。出発日はLambdaのイベントで指定され、Lambda実行日や「3〜6か月先」へ自動設定されません。Frontendでの可視化や追加分析APIは今後の開発対象です。

## Architecture（Frontend・本番ホスティングは構想）

```text
EventBridge Scheduler
        ↓
Python Lambda → Ignav Flight API
        ↓
Amazon S3 raw
        ↓
AWS Glue ETL（JSON → Parquet）
        ↓
Amazon S3 processed / Glue Data Catalog
        ↓
Amazon Athena
        ↓
Spring Boot REST API（ECS Fargate）
        ↓
React + TypeScript（今後実装、S3 + CloudFrontを想定）
```

データ収集処理とWebアプリケーションは分離し、Webリクエストから外部APIを直接呼び出しません。Rawデータは再処理できるよう元の形式で保持し、分析処理はAthenaで行います。初期ダッシュボードはSpring Bootプロセス内のCaffeineでキャッシュします。キャッシュは再起動時に消え、複数コンテナ間では共有されません。

## Technology Stack

| Area | Technologies |
| --- | --- |
| Frontend | React, TypeScript, Vite（今後実装） |
| Backend | Java, Spring Boot, Maven, Docker |
| Data pipeline | Python, PySpark, Docker（ローカル開発のみ）, AWS Lambda, EventBridge Scheduler, AWS Glue |
| Storage / Analytics | Amazon S3, Apache Parquet, Glue Data Catalog, Athena |
| Hosting | S3, CloudFront, ECS Fargate, ALB, ECR（構想） |
| CI/CD / Monitoring | GitHub Actions（構想）, CloudWatch |

## Repository Structure

```text
flight-price-analytics/
├── .devcontainer/
│   ├── frontend/
│   │   └── devcontainer.json
│   ├── backend/
│   │   └── devcontainer.json
│   ├── lambda/
│   │   └── devcontainer.json
│   └── glue/
│       └── devcontainer.json
├── frontend/
│   └── Dockerfile
├── backend/
│   ├── backend/
│   │   ├── src/
│   │   ├── pom.xml
│   │   ├── mvnw
│   │   └── .mvn/
│   └── Dockerfile
├── lambdas/
│   └── flight-fetcher/
│       ├── src/
│       │   └── handler.py
│       ├── tests/
│       ├── requirements.txt
│       ├── Dockerfile
│       └── .dockerignore
├── pipeline/
│   └── glue/
│       ├── jobs/
│       │   └── transform_flight_prices.py
│       ├── tests/
│       ├── requirements.txt
│       ├── Dockerfile
│       └── .dockerignore
├── .github/
│   └── workflows/
├── docs/
└── docker-compose.yml
```

## Local Development

Backendのテストは`backend/backend`で実行します。下記のリージョンとS3 URIは、AWSへ接続しない単体テスト用のダミー設定です。

```bash
cd backend/backend
AWS_REGION=ap-northeast-1 ATHENA_OUTPUT_LOCATION=s3://example-results/ sh ./mvnw test
```

ローカルから実際のAthenaを呼ぶ場合は、有効なAWS認証情報・リージョン・クエリ結果出力先が必要です。ComposeではBackendコンテナに`backend/backend/.env`と読み取り専用の`~/.aws`を渡します。FrontendのアプリケーションとVite Proxyはまだ実装していません。

LambdaとGlue ETLは常駐させず、開発・テストするときにそれぞれのDev Containerを起動します。

```text
Lambda Dev Container: pytest / sam local invoke
Glue Dev Container:   pytest / spark-submit
```

ソースコードを各コンテナへVolume Mountし、サービスごとに独立したランタイムと依存関係を使用する構成です。

BackendのDockerfileは、本番Imageを作成するためのマルチステージ構成とします。その他のDockerfileは開発環境のみを定義します。

| Service | Development | Build / Production |
| --- | --- | --- |
| Frontend | Docker Compose / Dev Container（今後） | CI/CDで静的ファイルを生成し、S3へ配置（構想） |
| Backend | Docker Compose / Dev Container | `build`でJARを生成し、`runtime` ImageをECSへデプロイ（構想） |
| Lambda | 個別のDev Container | CI/CDでテストとZIP生成を行い、Lambdaへデプロイ |
| Glue | 個別のDev Container | CI/CDでテスト後、PySparkスクリプトをGlueへアップロード |

本番環境でコンテナImageそのものを実行するのはBackendだけです。

Backendのテストには上記のコマンドを使用します。JARだけをビルドする場合はMaven Wrapperで以下を実行します。

```bash
cd backend/backend
sh ./mvnw -DskipTests package
```

## Documentation

- [Data pipeline](docs/pipeline.md)
- [Data model and analytics](docs/data-model.md)
- [Backend API](docs/api.md)
- [Deployment and operations](docs/operations.md)

## Development Policy

- アプリケーション、Lambda、Glue JobのコードはGitで管理し、AWS Console上のコードを正としない
- デプロイはGitHub Actionsから行う
- Rawデータは再処理可能な状態で保持する
- FrontendからAthenaを直接利用せず、任意SQLも受け付けない
- Frontend実装後は、開発環境のBackend APIをVite Proxy経由で呼び出す
- Frontend実装後はFrontendとBackendをDocker Composeでまとめ、LambdaとGlue ETLは個別のDev Containerで開発する
- Frontend、Lambda、Glue ETLのコンテナはローカル開発にのみ使用する
