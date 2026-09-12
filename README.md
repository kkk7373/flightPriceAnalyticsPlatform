# Flight Price Analytics Platform

航空券価格を定期収集し、価格推移、航空会社別価格、購入タイミングごとの傾向を分析・可視化するWebアプリケーションです。

## 主な機能

- 最新・最安・平均価格の表示
- 価格履歴の可視化
- 航空会社別の価格比較
- 出発までの日数と価格の分析
- 所要時間、乗継回数、使用機材、手荷物情報の表示

現時点の対象は、関西国際空港（KIX）から仁川国際空港（ICN）へのEconomy便に限定します。現在から3〜6か月先までを1日1回収集し、検証済み価格（`price_status = 'verified'`）を分析対象とします。

## Architecture

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
React + TypeScript（S3 + CloudFront）
```

データ収集処理とWebアプリケーションは分離し、Webリクエストから外部APIを直接呼び出しません。Rawデータは再処理できるよう元の形式で保持し、分析処理はAthenaで行います。

## Technology Stack

| Area | Technologies |
| --- | --- |
| Frontend | React, TypeScript, Vite, Docker（ローカル開発のみ） |
| Backend | Java, Spring Boot, Maven, Docker |
| Data pipeline | Python, PySpark, Docker（ローカル開発のみ）, AWS Lambda, EventBridge Scheduler, AWS Glue |
| Storage / Analytics | Amazon S3, Apache Parquet, Glue Data Catalog, Athena |
| Hosting | S3, CloudFront, ECS Fargate, ALB, ECR |
| CI/CD / Monitoring | GitHub Actions, CloudWatch |

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
│   ├── src/
│   ├── package.json
│   ├── Dockerfile
│   └── .dockerignore
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/
│   ├── Dockerfile
│   └── .dockerignore
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

各サービスはDockerfileとDev Container設定を持ちます。常時起動するFrontendとBackendはDocker Composeでまとめて起動します。

```bash
docker compose up
```

FrontendのテストもCompose経由で実行します。

```bash
docker compose run --rm frontend npm test
```

開発環境では、BrowserからBackend APIを直接呼び出しません。Frontendは`/api`へのリクエストをVite Proxy経由でBackendコンテナへ転送します。

```text
Browser
    ↓ http://localhost:5173/api/*
Frontend / Vite Proxy
    ↓ http://backend:8080/api/*
Backend / Spring Boot
```

Frontendコンテナから参照するBackend URLには、Docker Composeのサービス名を使用します。

```text
BACKEND_SERVER=http://backend:8080
```

LambdaとGlue ETLは常駐させず、開発・テストするときにそれぞれのDev Containerを起動します。

```text
Lambda Dev Container: pytest / sam local invoke
Glue Dev Container:   pytest / spark-submit
```

ソースコードを各コンテナへVolume Mountし、サービスごとに独立したランタイムと依存関係を使用します。

BackendのDockerfileは、本番Imageを作成するためのマルチステージ構成とします。その他のDockerfileは開発環境のみを定義します。

| Service | Development | Build / Production |
| --- | --- | --- |
| Frontend | Docker Compose / Dev Container | CI/CDで静的ファイルを生成し、S3へ配置 |
| Backend | Docker Compose / Dev Container | `build`でJARを生成し、`runtime` ImageをECSへデプロイ |
| Lambda | 個別のDev Container | CI/CDでテストとZIP生成を行い、Lambdaへデプロイ |
| Glue | 個別のDev Container | CI/CDでテスト後、PySparkスクリプトをGlueへアップロード |

本番環境でコンテナImageそのものを実行するのはBackendだけです。

BackendのテストとビルドにはMaven Wrapperを使用します。

```bash
cd backend
./mvnw test
./mvnw clean package
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
- 開発環境のBackend APIはFrontendのVite Proxy経由で呼び出す
- FrontendとBackendはDocker Compose、LambdaとGlue ETLは個別のDev Containerで開発する
- Frontend、Lambda、Glue ETLのコンテナはローカル開発にのみ使用する
