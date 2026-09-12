# Deployment and Operations

## Deployment

GitHub Actionsを使用し、モノレポ内で変更された領域のWorkflowだけを実行します。

ローカル開発ではFrontend、Backend、Lambda、GlueごとのコンテナをDocker Composeで起動します。各サービスはDockerfileとDev Container設定を持ちます。

### Frontend

```text
npm ci
    ↓
npm test
    ↓
npm run build
    ↓
S3 Deploy
    ↓
CloudFront Invalidation
```

Frontendの実行用Docker Imageはローカル開発専用です。CI/CDで生成した静的ファイルをS3へ配置し、ImageをECRや本番環境へデプロイすることはありません。

### Backend

```text
./mvnw test
    ↓
Docker Build
    ↓
ECR Push
    ↓
ECS Fargate Deploy
```

Application Load Balancerを経由してECS上のSpring Bootへアクセスします。Backendだけが同じDockerfileの`runtime`ステージから本番用Imageを生成します。

### Lambda and Glue

```text
Lambda: pytest → ZIP Packaging → Lambda Deploy
Glue:   Test → Glue Script Upload
```

LambdaとGlueの実行用コンテナはDev Containerによるローカル開発専用です。テスト、LambdaのZIP生成、GlueスクリプトのアップロードはCI/CDで行います。

## Lambda Production Environment Variables

本番Lambdaでは、イベントデータから実行環境を切り替えず、Lambdaの環境変数で本番用クライアントと保存先を明示します。環境変数はすべて文字列として設定します。

### Required

| Variable | Production value | Description |
| --- | --- | --- |
| `FLIGHT_CLIENT` | `ignav` | 固定JSONではなくIgnav Flight APIを使用する |
| `IGNAV_API_KEY` | Secrets Managerなどから設定 | Ignav Flight APIの認証キー |
| `STORAGE_TYPE` | `s3` | ローカル保存ではなくS3へRaw JSONを保存する |
| `RAW_BUCKET` | 環境ごとのS3バケット名 | Raw JSONの保存先バケット |
| `GLUE_CLIENT` | `aws` | モックではなくAWS Glueを起動する |
| `GLUE_JOB_NAME` | 環境ごとのGlue Job名 | `StartJobRun`で起動するGlue Job |
| `ORIGIN` | `KIX` | 現在の対象である関西国際空港 |
| `DESTINATION` | `ICN` | 現在の対象である仁川国際空港 |
| `MARKET` | `JP` | Ignav APIの`market`へ渡す日本の2文字国コード |

### Optional

| Variable | Default | Description |
| --- | --- | --- |
| `TRIP_TYPE` | `one-way` | Ignav APIの運賃検索種別 |
| `RAW_PREFIX` | `raw/flight-offers` | Raw JSONのS3キープレフィックス |
| `REQUEST_TIMEOUT_SECONDS` | `30` | Ignav APIのHTTPタイムアウト秒数 |

設定例を以下に示します。実際のAPIキーをリポジトリやデプロイ設定へ平文で記載しないでください。

```text
FLIGHT_CLIENT=ignav
STORAGE_TYPE=s3
RAW_BUCKET=flight-price-analytics-data
RAW_PREFIX=raw/flight-offers
GLUE_CLIENT=aws
GLUE_JOB_NAME=transform-flight-prices
ORIGIN=KIX
DESTINATION=ICN
TRIP_TYPE=one-way
MARKET=JP
REQUEST_TIMEOUT_SECONDS=30
IGNAV_API_KEY=<set securely during deployment>
```

`MARKET`には通貨コードではなくIgnav APIの2文字国コードを設定します。日本市場は`JPY`ではなく`JP`です。

`MOCK_STORAGE_DIR`はローカル開発専用であり、本番Lambdaには設定しません。また、AWSアクセスキーを環境変数へ設定せず、S3とGlueへのアクセスにはLambda実行ロールを使用します。

## IAM

### Lambda

- S3 rawへの`PutObject`
- Glue `StartJobRun`
- CloudWatch Logs

Ignav API Keyはコードへ直接記述せず、実行環境から参照します。

### Glue

- S3 rawのRead
- S3 processedのRead / Write
- Glue Data Catalog
- CloudWatch Logs

### ECS Task Role

- Athena `StartQueryExecution`
- Athena `GetQueryExecution`
- Athena `GetQueryResults`
- Athena Query Result用S3へのアクセス
- Glue Data CatalogのRead

## Monitoring

CloudWatchで以下を監視します。

- Lambda Errors / Logs
- Glue Job Failure / Logs
- ECS Application Logs / Task Failure
- ALB 5xx

## Error Handling

| Failure | Behavior |
| --- | --- |
| Ignav API | Lambdaを失敗として終了し、Glue Jobを起動しない |
| S3 raw保存 | Lambdaを失敗として終了し、Glue Jobを起動しない |
| Glue Job | Processedデータ生成失敗としてCloudWatch Logsへ記録する |
| Athena Query | Backendで検知し、Frontendへ適切なError Responseを返す |
