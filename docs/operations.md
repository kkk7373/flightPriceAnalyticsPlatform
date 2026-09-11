# Deployment and Operations

## Deployment

GitHub Actionsを使用し、モノレポ内で変更された領域のWorkflowだけを実行します。

ローカル開発ではFrontend、Backend、Lambda、GlueごとのコンテナをDocker Composeで起動します。各サービスは専用のDockerfileとDev Container設定を持ちます。

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

FrontendのDocker Imageはローカル開発専用です。ECRへのPushや本番環境へのデプロイは行いません。

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

Application Load Balancerを経由してECS上のSpring Bootへアクセスします。

### Lambda and Glue

```text
Lambda: pytest → ZIP Packaging → Lambda Deploy
Glue:   Test → Glue Script Upload
```

LambdaとGlueのコンテナはDev Containerによるローカル開発専用です。本番では、LambdaはZIPパッケージ、GlueはPySparkスクリプトとしてデプロイします。

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
